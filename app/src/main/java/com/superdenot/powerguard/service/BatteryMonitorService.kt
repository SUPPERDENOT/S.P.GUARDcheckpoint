package com.superdenot.powerguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.superdenot.powerguard.MainActivity
import com.superdenot.powerguard.data.local.AppDatabase
import com.superdenot.powerguard.data.local.SessionRecoveryStore
import com.superdenot.powerguard.data.local.dao.BatteryDao
import com.superdenot.powerguard.data.local.dao.SampleFlushStatus
import com.superdenot.powerguard.data.local.dao.SessionFinalizationStatus
import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.domain.engine.AnomalyDetector
import com.superdenot.powerguard.domain.engine.BatteryMonitorEngine
import com.superdenot.powerguard.domain.model.LiveBatteryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal sealed interface TargetSessionLookupResult {
    data class Found(val session: ChargingSessionEntity) : TargetSessionLookupResult
    data object MissingInAccessibleDb : TargetSessionLookupResult
    data class DatabaseInaccessible(val cause: Throwable?) : TargetSessionLookupResult
}

class BatteryMonitorService : Service() {

    private enum class ActorLifecycleState {
        STOPPED,
        STARTING,
        RUNNING_CHARGING,
        RUNNING_MANUAL,
        RECOVERY_HOLD
    }

    private enum class SessionCleanupOutcome {
        FINALIZED,
        ALREADY_CLOSED,
        NO_ACTIVE_SESSION,
        TARGET_MISSING,
        DATABASE_UNAVAILABLE,
        NEEDS_FALLBACK
    }

    internal enum class FallbackClosureStatus {
        FULL_DATA_SAVED_AND_CLEANED,
        CLEANUP_ONLY_DATA_INCOMPLETE,
        TARGET_MISSING_FAILED,
        DATABASE_UNAVAILABLE_FAILED,
        FAILED
    }

    internal enum class PersistedRecoveryReconcileStatus {
        NO_CHECKPOINT,
        RECONCILED_AND_CLEARED,
        RECONCILED_CHECKPOINT_CLEAR_PENDING,
        TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP,
        DATABASE_INACCESSIBLE,
        CHECKPOINT_CORRUPT,
        CHECKPOINT_UNAVAILABLE;

        val isReconciledOrNoCheckpoint: Boolean
            get() = this == NO_CHECKPOINT ||
                this == RECONCILED_AND_CLEARED ||
                this == RECONCILED_CHECKPOINT_CLEAR_PENDING
    }

    internal enum class SessionFinalizationResult {
        COMPLETE_DATA_FINALIZED,
        ALREADY_CLOSED_IDEMPOTENT,
        NO_ACTIVE_SESSION_CLEAN,
        TARGET_MISSING_NEEDS_FOLLOW_UP,
        CLEANUP_ONLY_DATA_INCOMPLETE,
        DATABASE_UNAVAILABLE,
        FAILED
    }

    internal data class BufferedSampleEntry(
        val seqId: Long,
        val timestampMs: Long,
        val endTimestampMs: Long = timestampMs,
        val state: LiveBatteryState,
        val weight: Int = 1,
        val maxTempCelsius: Float = state.tempCelsius,
        // Simpan jumlah berbobot asli agar pemadatan berulang tidak mengakumulasi galat pembulatan.
        val weightedCurrentMaSum: Long = state.currentMa.toLong() * weight.coerceAtLeast(1),
        val weightedVoltageMvSum: Long = state.voltageMv.toLong() * weight.coerceAtLeast(1),
        val weightedTempCelsiusSum: Double = state.tempCelsius.toDouble() * weight.coerceAtLeast(1),
        val weightedPowerWattSum: Double = state.powerWatt.toDouble() * weight.coerceAtLeast(1),
        val weightedDeepSleepRatioSum: Double = state.deepSleepRatio.toDouble() * weight.coerceAtLeast(1)
    )

    private data class WeightedBatchMetrics(
        val totalWeight: Int,
        val spanDurationMs: Long,
        val weightedCurrentMaSum: Long,
        val weightedVoltageMvSum: Long,
        val weightedTempCelsiusSum: Double,
        val weightedPowerWattSum: Double,
        val weightedDeepSleepRatioSum: Double,
        val weightedCurrentMa: Int,
        val weightedVoltageMv: Int,
        val weightedTempCelsius: Float,
        val maxTempCelsius: Float,
        val weightedPowerWatt: Float,
        val weightedDeepSleepRatio: Float,
        val latestState: LiveBatteryState
    )

    internal data class UnpersistedSessionRecoveryMetadata(
        val sessionId: Long,
        val bufferedSamplesCount: Int,
        val bufferedSamplesSnapshot: List<LiveBatteryState>,
        val bufferedTotalSampleWeight: Int,
        val bufferedSampleSpanDurationMs: Long,
        val pendingBatchTimestampMs: Long,
        val accumulatedMah: Double,
        val sessionSeconds: Long,
        val maxSessionTemp: Float,
        val currentSumForAvg: Long,
        val currentReadingsCount: Long,
        val alertedAnomalyCodes: Set<String>,
        val dbSessionsClosed: Boolean,
        val failureReason: String,
        val requiresFollowUp: Boolean = false,
        val recoveryStatus: SessionRecoveryStore.RecoveryStatus = SessionRecoveryStore.RecoveryStatus.PENDING_RECONCILIATION
    )

    internal data class InMemorySessionSnapshot(
        val activeSessionId: Long?,
        val bufferedSamplesCount: Int,
        val bufferedTotalSampleWeight: Int,
        val bufferedSampleSpanDurationMs: Long = 0L,
        val pendingBatchTimestampMs: Long,
        val accumulatedMah: Double,
        val sessionSeconds: Long,
        val maxSessionTemp: Float,
        val currentSumForAvg: Long,
        val currentReadingsCount: Long,
        val alertedAnomalyCodes: Set<String>,
        val consecutiveFlushFailures: Int,
        val nextFlushRetryAtSec: Long,
        val checkpointSavePendingRetry: Boolean,
        val samplePersistenceHalted: Boolean,
        val hasIncompleteRecoverySnapshot: Boolean,
        val lastRecoveryReconcileStatus: PersistedRecoveryReconcileStatus = PersistedRecoveryReconcileStatus.NO_CHECKPOINT,
        val recoveryRequiresFollowUp: Boolean = false,
        val incompleteRecoverySessionId: Long? = null,
        val incompleteRecoveryAccumulatedMah: Double = 0.0,
        val incompleteRecoveryBufferedCount: Int = 0,
        val isInRecoveryHold: Boolean = false,
        val isMonitoringLoopActive: Boolean = false
    )

    private sealed interface LifecycleCommand {
        data class PrepareStart(val generation: Long) : LifecycleCommand
        data class StartFailed(val generation: Long) : LifecycleCommand
        data class Start(val startId: Int, val manual: Boolean, val generation: Long) : LifecycleCommand
        data class Restart(val startId: Int, val generation: Long) : LifecycleCommand
        data class Stop(val startId: Int, val generation: Long) : LifecycleCommand
        data class AutoStop(val generation: Long) : LifecycleCommand
        data class ChargerConnected(val generation: Long) : LifecycleCommand
        data object Destroy : LifecycleCommand
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleChannel = Channel<LifecycleCommand>(Channel.UNLIMITED)
    private val destroyRequested = AtomicBoolean(false)

    // State lifecycle dimiliki dan dimutasi secara eksklusif oleh satu coroutine lifecycleActor
    @Volatile
    private var actorState: ActorLifecycleState = ActorLifecycleState.STOPPED
    private var monitorJob: Job? = null
    private var activeRunGeneration: Long = 0L
    private var manualMonitoringRequested: Boolean = false

    // Pemisahan status proses finalisasi, status cleanup DB, dan keberhasilan penyimpanan data lengkap
    @Volatile
    private var destroyFinalizationInProgress: Boolean = false
    private var destroyDbCleanupSucceeded: Boolean = false
    private var destroyFullDataFinalized: Boolean = false
    private val finalizedSessionIds = mutableSetOf<Long>()

    // Counter startId bersifat per-instance Service sesuai kontrak Android
    private val instanceLatestObservedCommandId = AtomicInteger(-1)
    private val instanceLatestStartCommandId = AtomicInteger(-1)
    private val sampleEntrySequence = AtomicLong(0L)

    private lateinit var engine: BatteryMonitorEngine
    private lateinit var anomalyDetector: AnomalyDetector
    private lateinit var database: AppDatabase
    private lateinit var powerManager: PowerManager
    private lateinit var notificationManager: NotificationManager

    @VisibleForTesting
    internal var daoOverride: BatteryDao? = null

    @VisibleForTesting
    internal var engineOverride: BatteryMonitorEngine? = null

    private fun resolveDao(): BatteryDao = daoOverride ?: database.batteryDao()
    private fun resolveEngine(): BatteryMonitorEngine = engineOverride ?: engine

    // State sesi dan operasi database dilindungi oleh sharedSessionMutex
    @Volatile
    private var activeSessionId: Long? = null
    private var accumulatedMah: Double = 0.0
    private var sessionSeconds: Long = 0L
    private var maxSessionTemp: Float = 0f
    private var currentSumForAvg: Long = 0L
    private var currentReadingsCount: Long = 0L
    private val alertedAnomalyCodes = mutableSetOf<String>()
    private val sampleBuffer = mutableListOf<BufferedSampleEntry>()
    private var samplePersistenceHalted: Boolean = false
    private var pendingFlushBatchTimestampMs: Long = 0L
    private var consecutiveFlushFailures: Int = 0
    private var nextFlushRetryAtSec: Long = 0L
    private var checkpointSavePendingRetry: Boolean = false
    private var lastIncompleteSessionRecovery: UnpersistedSessionRecoveryMetadata? = null
    private var lastReconcileStatus: PersistedRecoveryReconcileStatus = PersistedRecoveryReconcileStatus.NO_CHECKPOINT

    override fun onCreate() {
        super.onCreate()
        synchronized(lifecycleLock) {
            activeInstance = this
        }
        engine = BatteryMonitorEngine(this)
        anomalyDetector = AnomalyDetector(this)
        database = AppDatabase.getInstance(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
        startLifecycleActor()
        if (hasPendingStarts()) {
            lifecycleChannel.trySend(LifecycleCommand.PrepareStart(latestStartGeneration.get()))
        }
    }

    /**
     * Satu-satunya consumer dari lifecycleChannel dan pemilik tunggal transisi state lifecycle.
     * Menangani seluruh event secara berurutan dengan isolasi exception operasional per perintah
     * sambil tetap meneruskan CancellationException dan menjamin cleanup deterministik di finally.
     */
    private fun startLifecycleActor() {
        serviceScope.launch {
            try {
                for (command in lifecycleChannel) {
                    if (command is LifecycleCommand.Destroy) {
                        handleDestroyInActor()
                        break
                    }
                    try {
                        when (command) {
                            is LifecycleCommand.PrepareStart -> handlePrepareStart(command.generation)
                            is LifecycleCommand.StartFailed -> handleStartFailed(command.generation)
                            is LifecycleCommand.Start -> handleStart(command.startId, command.manual, command.generation)
                            is LifecycleCommand.Restart -> handleRestart(command.startId, command.generation)
                            is LifecycleCommand.Stop -> handleStop(command.startId, command.generation)
                            is LifecycleCommand.AutoStop -> handleAutoStop(command.generation)
                            is LifecycleCommand.ChargerConnected -> handleChargerConnected(command.generation)
                            is LifecycleCommand.Destroy -> Unit
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (error: Throwable) {
                        Log.e("PowerGuard", "Kesalahan pada lifecycleActor saat memproses $command: ${error.message}", error)
                        if (!destroyRequested.get() &&
                            actorState == ActorLifecycleState.STARTING &&
                            !hasPendingStarts()
                        ) {
                            transitionActorState(ActorLifecycleState.STOPPED)
                            stopForegroundAndSelfIfIdle(
                                instanceLatestObservedCommandId.get(),
                                latestStartGeneration.get()
                            )
                        }
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    try {
                        handleDestroyInActor()
                    } catch (cleanupError: Throwable) {
                        Log.e("PowerGuard", "Kesalahan cleanup saat terminasi lifecycleActor: ${cleanupError.message}", cleanupError)
                    } finally {
                        synchronized(lifecycleLock) {
                            if (activeInstance === this@BatteryMonitorService) {
                                activeInstance = null
                            }
                        }
                        runCatching { lifecycleChannel.close() }
                        serviceScope.cancel()
                    }
                }
            }
        }
    }

    private fun transitionActorState(newState: ActorLifecycleState) {
        synchronized(lifecycleLock) {
            actorState = newState
            if (activeInstance == null || activeInstance === this) {
                when (newState) {
                    ActorLifecycleState.STOPPED -> {
                        _isStartingFlow.value = false
                        _isRunningFlow.value = false
                    }
                    ActorLifecycleState.STARTING -> {
                        _isStartingFlow.value = true
                        _isRunningFlow.value = false
                    }
                    ActorLifecycleState.RUNNING_CHARGING,
                    ActorLifecycleState.RUNNING_MANUAL,
                    ActorLifecycleState.RECOVERY_HOLD -> {
                        _isStartingFlow.value = false
                        _isRunningFlow.value = true
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        synchronized(lifecycleLock) {
            instanceLatestObservedCommandId.updateAndGet { prev -> maxOf(prev, startId) }
        }

        if (intent == null) {
            val gen = registerPendingStart()
            synchronized(lifecycleLock) {
                instanceLatestStartCommandId.updateAndGet { prev -> maxOf(prev, startId) }
            }
            lifecycleChannel.trySend(LifecycleCommand.PrepareStart(gen))
            val fgResult = runCatching {
                startForegroundCompat(buildMonitorNotification("Memulihkan pemantauan sensor daya..."))
            }
            if (fgResult.isFailure) {
                removePendingStart(gen)
                lifecycleChannel.trySend(LifecycleCommand.StartFailed(gen))
                stopSelfIfStaleAndIdle(startId, gen)
                return START_NOT_STICKY
            }
            if (lifecycleChannel.trySend(LifecycleCommand.Restart(startId, gen)).isFailure) {
                removePendingStart(gen)
            }
            return START_STICKY
        }

        return when (intent.action) {
            ACTION_STOP_CHARGING_SESSION -> {
                val stopGen = commandSequence.incrementAndGet()
                lifecycleChannel.trySend(LifecycleCommand.Stop(startId, stopGen))
                START_NOT_STICKY
            }
            ACTION_START_MANUAL_MONITORING -> {
                dispatchStartCommand(startId = startId, manual = true, intent = intent)
                START_NOT_STICKY
            }
            else -> {
                dispatchStartCommand(startId = startId, manual = false, intent = intent)
                START_STICKY
            }
        }
    }

    private fun dispatchStartCommand(startId: Int, manual: Boolean, intent: Intent) {
        val fromRequestStart = intent.getBooleanExtra(EXTRA_FROM_REQUEST_START, false)
        val extraGen = intent.getLongExtra(EXTRA_START_GENERATION, -1L)

        val gen = if (fromRequestStart && extraGen > 0L) {
            synchronized(lifecycleLock) {
                if (extraGen >= latestStartGeneration.get()) {
                    instanceLatestStartCommandId.updateAndGet { prev -> maxOf(prev, startId) }
                }
            }
            extraGen
        } else {
            val newGen = registerPendingStart()
            synchronized(lifecycleLock) {
                instanceLatestStartCommandId.updateAndGet { prev -> maxOf(prev, startId) }
            }
            newGen
        }

        val isAlreadyStale = synchronized(lifecycleLock) {
            gen < latestStartGeneration.get()
        }

        if (!isAlreadyStale) {
            lifecycleChannel.trySend(LifecycleCommand.PrepareStart(gen))
        }

        val fgResult = runCatching {
            startForegroundCompat(buildMonitorNotification("Memulai pemantauan sensor daya..."))
        }
        if (fgResult.isFailure) {
            removePendingStart(gen)
            lifecycleChannel.trySend(LifecycleCommand.StartFailed(gen))
            stopSelfIfStaleAndIdle(startId, gen)
            return
        }

        if (isAlreadyStale) {
            removePendingStart(gen)
            lifecycleChannel.trySend(LifecycleCommand.StartFailed(gen))
            return
        }

        if (lifecycleChannel.trySend(LifecycleCommand.Start(startId, manual = manual, generation = gen)).isFailure) {
            removePendingStart(gen)
        }
    }

    private fun handlePrepareStart(generation: Long) {
        if (destroyRequested.get()) return
        if (generation < latestStartGeneration.get()) return
        if (actorState == ActorLifecycleState.STOPPED) {
            transitionActorState(ActorLifecycleState.STARTING)
        }
    }

    private fun handleStartFailed(generation: Long) {
        if (destroyRequested.get()) return
        if (generation < latestStartGeneration.get()) return
        if (actorState == ActorLifecycleState.STARTING && !hasPendingStarts()) {
            transitionActorState(ActorLifecycleState.STOPPED)
            stopForegroundAndSelfIfIdle(instanceLatestObservedCommandId.get(), generation)
        }
    }

    private fun hasRecoverableRamOnlyData(): Boolean {
        return activeSessionId != null ||
            sampleBuffer.isNotEmpty() ||
            accumulatedMah > 0.0 ||
            sessionSeconds > 0L ||
            currentReadingsCount > 0L ||
            alertedAnomalyCodes.isNotEmpty()
    }

    /**
     * Durability Gate: mengembalikan true apabila masih ada data sesi/sampel di RAM yang
     * belum terkonfirmasi tersimpan di Room maupun di SessionRecoveryStore persisten.
     */
    private fun hasUnpersistedRamOnlyData(finalizationResult: SessionFinalizationResult): Boolean {
        if (!hasRecoverableRamOnlyData()) return false
        if (finalizationResult == SessionFinalizationResult.FAILED || checkpointSavePendingRetry) {
            return true
        }
        val targetId = activeSessionId ?: return true
        val persistedCheckpoint = SessionRecoveryStore.loadForSession(applicationContext, targetId)
        val loaded = persistedCheckpoint as? SessionRecoveryStore.CheckpointLoadResult.Loaded ?: return true
        val checkpoint = loaded.recovery

        // Checkpoint harus benar-benar mencakup snapshot RAM saat ini; keberadaan file
        // dengan sessionId yang sama saja tidak cukup untuk melewati durability gate.
        if (checkpoint.sessionId != targetId ||
            checkpoint.accumulatedMah + 1e-6 < accumulatedMah ||
            checkpoint.sessionSeconds < sessionSeconds ||
            checkpoint.maxSessionTemp < maxSessionTemp ||
            checkpoint.currentReadingsCount < currentReadingsCount ||
            (checkpoint.currentReadingsCount == currentReadingsCount &&
                checkpoint.currentSumForAvg != currentSumForAvg) ||
            !checkpoint.alertedAnomalyCodes.containsAll(alertedAnomalyCodes)
        ) {
            return true
        }

        if (sampleBuffer.isNotEmpty()) {
            val entries = sampleBuffer.toList()
            val metrics = computeWeightedBatchMetrics(entries)
            val earliestTimestamp = entries.minOf { it.timestampMs }
            val currentSpanMs = metrics.spanDurationMs
            val currentWeight = metrics.totalWeight
            val currentEndTimestamp = entries.maxOf { maxOf(it.timestampMs, it.endTimestampMs) }
            val checkpointEndTimestamp = if (
                checkpoint.sampleSpanDurationMs > Long.MAX_VALUE - checkpoint.pendingBatchTimestampMs
            ) Long.MAX_VALUE else checkpoint.pendingBatchTimestampMs + checkpoint.sampleSpanDurationMs
            val requiredMaxTemp = maxOf(maxSessionTemp, metrics.maxTempCelsius)
            // Jika bobot identik, kedua payload merepresentasikan batch yang sama sehingga
            // seluruh metrik listrik harus cocok. Jika checkpoint berbobot lebih besar,
            // checkpoint dapat mencakup batch terdahulu yang sudah digabung; validasi subset
            // metrik tidak dapat dilakukan, maka gunakan cakupan waktu + bobot + level terbaru.
            val sameBatchWeight = checkpoint.sampleWeight == currentWeight
            if (!checkpoint.hasPendingSample ||
                checkpoint.sampleWeight < currentWeight ||
                checkpoint.pendingBatchTimestampMs > earliestTimestamp ||
                checkpoint.sampleSpanDurationMs < currentSpanMs ||
                checkpointEndTimestamp < currentEndTimestamp ||
                checkpoint.sampleBatteryLevel != metrics.latestState.level ||
                (sameBatchWeight && (
                    checkpoint.sampleCurrentMa != metrics.weightedCurrentMa ||
                        checkpoint.sampleVoltageMv != metrics.weightedVoltageMv ||
                        checkpoint.sampleTempCelsius != metrics.weightedTempCelsius ||
                        checkpoint.samplePowerWatt != metrics.weightedPowerWatt
                    )) ||
                checkpoint.maxSessionTemp < requiredMaxTemp
            ) {
                return true
            }
        }
        return false
    }

    private fun enterRecoveryHoldLocked(generation: Long, reason: String) {
        samplePersistenceHalted = true
        activeRunGeneration = generation
        transitionActorState(ActorLifecycleState.RECOVERY_HOLD)
        ensureMonitoringLoopRunning()
        Log.e("PowerGuard", "Menahan shutdown service (RECOVERY_HOLD): $reason")
    }

    private suspend fun handleStart(startId: Int, manual: Boolean, generation: Long) {
        if (destroyRequested.get()) return
        if (!consumePendingStartIfLatest(generation)) {
            return
        }

        if (actorState == ActorLifecycleState.STOPPED) {
            transitionActorState(ActorLifecycleState.STARTING)
        }

        sharedSessionMutex.withLock {
            if (destroyRequested.get() || generation < latestStartGeneration.get()) {
                return@withLock
            }

            val currentSensor = resolveEngine().readCurrentState()
            activeRunGeneration = generation
            if (manual) {
                manualMonitoringRequested = true
            } else if (!currentSensor.isCharging) {
                manualMonitoringRequested = false
            }

            if (currentSensor.isCharging) {
                if (activeSessionId == null) {
                    recoverOrStartChargingSession(currentSensor)
                }
                transitionActorState(ActorLifecycleState.RUNNING_CHARGING)
                ensureMonitoringLoopRunning()
            } else if (manual) {
                val hasPendingCheckpoint = SessionRecoveryStore.loadAll(applicationContext).isNotEmpty()
                if (activeSessionId != null || sampleBuffer.isNotEmpty() || hasPendingCheckpoint) {
                    val finalizationResult = ensureSessionFinalizedSafely()
                    if (!finalizationResult.isCompleteAndSafeForManualTransition()) {
                        manualMonitoringRequested = false
                        if (hasUnpersistedRamOnlyData(finalizationResult)) {
                            enterRecoveryHoldLocked(generation, "Checkpoint capture gagal saat transisi ke mode manual (result=$finalizationResult)")
                        } else {
                            stopMonitoringLoopLocked()
                            activeRunGeneration = 0L
                            transitionActorState(ActorLifecycleState.STOPPED)
                            _liveState.value = LiveBatteryState()
                            stopForegroundAndSelfIfIdle(startId, generation)
                        }
                        return@withLock
                    }
                }
                transitionActorState(ActorLifecycleState.RUNNING_MANUAL)
                ensureMonitoringLoopRunning()
            } else {
                manualMonitoringRequested = false
                val finalizationResult = ensureSessionFinalizedSafely()
                if (hasUnpersistedRamOnlyData(finalizationResult)) {
                    enterRecoveryHoldLocked(generation, "Finalisasi & checkpoint gagal saat start dalam kondisi tidak mengisi daya (result=$finalizationResult)")
                    return@withLock
                }
                stopMonitoringLoopLocked()
                activeRunGeneration = 0L
                transitionActorState(ActorLifecycleState.STOPPED)
                _liveState.value = LiveBatteryState()
                stopForegroundAndSelfIfIdle(startId, generation)
            }
        }
    }

    private suspend fun handleRestart(startId: Int, generation: Long) {
        if (destroyRequested.get()) return
        if (!consumePendingStartIfLatest(generation)) {
            return
        }

        if (actorState == ActorLifecycleState.STOPPED) {
            transitionActorState(ActorLifecycleState.STARTING)
        }

        sharedSessionMutex.withLock {
            if (destroyRequested.get() || generation < latestStartGeneration.get()) {
                return@withLock
            }

            val currentSensor = resolveEngine().readCurrentState()
            activeRunGeneration = generation
            manualMonitoringRequested = false

            if (currentSensor.isCharging) {
                if (activeSessionId == null) {
                    recoverOrStartChargingSession(currentSensor)
                }
                transitionActorState(ActorLifecycleState.RUNNING_CHARGING)
                ensureMonitoringLoopRunning()
            } else {
                val finalizationResult = ensureSessionFinalizedSafely()
                if (hasUnpersistedRamOnlyData(finalizationResult)) {
                    enterRecoveryHoldLocked(generation, "Finalisasi & checkpoint gagal saat restart service dalam kondisi unplugged (result=$finalizationResult)")
                    return@withLock
                }
                stopMonitoringLoopLocked()
                activeRunGeneration = 0L
                transitionActorState(ActorLifecycleState.STOPPED)
                _liveState.value = LiveBatteryState()
                stopForegroundAndSelfIfIdle(startId, generation)
            }
        }
    }

    private suspend fun handleStop(stopStartId: Int, stopGeneration: Long) {
        if (destroyRequested.get()) return
        if (stopGeneration < latestStartGeneration.get() ||
            stopStartId < instanceLatestStartCommandId.get() ||
            hasPendingStarts()
        ) {
            return
        }

        sharedSessionMutex.withLock {
            if (destroyRequested.get() ||
                stopGeneration < latestStartGeneration.get() ||
                stopStartId < instanceLatestStartCommandId.get() ||
                hasPendingStarts()
            ) {
                return@withLock
            }

            val freshSensor = resolveEngine().readCurrentState()
            if (freshSensor.isCharging) {
                if (activeSessionId == null) {
                    recoverOrStartChargingSession(freshSensor)
                }
                transitionActorState(ActorLifecycleState.RUNNING_CHARGING)
                ensureMonitoringLoopRunning()
                return@withLock
            }

            if (manualMonitoringRequested || actorState == ActorLifecycleState.RUNNING_MANUAL) {
                val hasPendingCheckpoint = SessionRecoveryStore.loadAll(applicationContext).isNotEmpty()
                if (activeSessionId != null || sampleBuffer.isNotEmpty() || hasPendingCheckpoint) {
                    val finalizationResult = ensureSessionFinalizedSafely()
                    if (!finalizationResult.isCompleteAndSafeForManualTransition()) {
                        manualMonitoringRequested = false
                        if (hasUnpersistedRamOnlyData(finalizationResult)) {
                            enterRecoveryHoldLocked(maxOf(activeRunGeneration, latestStartGeneration.get()), "Checkpoint capture gagal saat stop sesi charger di mode manual (result=$finalizationResult)")
                        } else {
                            stopMonitoringLoopLocked()
                            activeRunGeneration = 0L
                            transitionActorState(ActorLifecycleState.STOPPED)
                            _liveState.value = LiveBatteryState()
                            stopForegroundAndSelfIfIdle(stopStartId, stopGeneration)
                        }
                        return@withLock
                    }
                }
                transitionActorState(ActorLifecycleState.RUNNING_MANUAL)
                ensureMonitoringLoopRunning()
                return@withLock
            }

            val finalizationResult = ensureSessionFinalizedSafely()
            if (hasUnpersistedRamOnlyData(finalizationResult)) {
                enterRecoveryHoldLocked(maxOf(activeRunGeneration, latestStartGeneration.get()), "Finalisasi & checkpoint gagal saat ACTION_STOP_CHARGING_SESSION (result=$finalizationResult)")
                return@withLock
            }

            stopMonitoringLoopLocked()
            activeRunGeneration = 0L
            transitionActorState(ActorLifecycleState.STOPPED)
            _liveState.value = LiveBatteryState()
            stopForegroundAndSelfIfIdle(stopStartId, stopGeneration)
        }
    }

    private suspend fun handleAutoStop(generation: Long) {
        if (destroyRequested.get()) return
        if (generation < latestStartGeneration.get() ||
            generation < activeRunGeneration ||
            hasPendingStarts()
        ) {
            return
        }

        sharedSessionMutex.withLock {
            if (destroyRequested.get() ||
                generation < latestStartGeneration.get() ||
                generation < activeRunGeneration ||
                hasPendingStarts()
            ) {
                return@withLock
            }

            val freshSensor = resolveEngine().readCurrentState()
            if (freshSensor.isCharging) {
                if (activeSessionId == null) {
                    recoverOrStartChargingSession(freshSensor)
                }
                transitionActorState(ActorLifecycleState.RUNNING_CHARGING)
                ensureMonitoringLoopRunning()
                return@withLock
            }

            if (manualMonitoringRequested || actorState == ActorLifecycleState.RUNNING_MANUAL) {
                val hasPendingCheckpoint = SessionRecoveryStore.loadAll(applicationContext).isNotEmpty()
                if (activeSessionId != null || sampleBuffer.isNotEmpty() || hasPendingCheckpoint) {
                    val finalizationResult = ensureSessionFinalizedSafely()
                    if (!finalizationResult.isCompleteAndSafeForManualTransition()) {
                        manualMonitoringRequested = false
                        if (hasUnpersistedRamOnlyData(finalizationResult)) {
                            enterRecoveryHoldLocked(generation, "Checkpoint capture gagal saat auto-stop di mode manual (result=$finalizationResult)")
                        } else {
                            stopMonitoringLoopLocked()
                            activeRunGeneration = 0L
                            transitionActorState(ActorLifecycleState.STOPPED)
                            _liveState.value = LiveBatteryState()
                            stopForegroundAndSelfIfIdle(instanceLatestObservedCommandId.get(), generation)
                        }
                        return@withLock
                    }
                }
                transitionActorState(ActorLifecycleState.RUNNING_MANUAL)
                ensureMonitoringLoopRunning()
                return@withLock
            }

            val finalizationResult = ensureSessionFinalizedSafely()
            if (hasUnpersistedRamOnlyData(finalizationResult)) {
                enterRecoveryHoldLocked(generation, "Finalisasi & checkpoint gagal saat auto-stop; menahan layanan sampai persisten (result=$finalizationResult)")
                return@withLock
            }

            stopMonitoringLoopLocked()
            activeRunGeneration = 0L
            transitionActorState(ActorLifecycleState.STOPPED)
            _liveState.value = LiveBatteryState()
            stopForegroundAndSelfIfIdle(instanceLatestObservedCommandId.get(), generation)
        }
    }

    private suspend fun handleChargerConnected(generation: Long) {
        if (destroyRequested.get()) return
        if (generation < latestStartGeneration.get() ||
            generation < activeRunGeneration ||
            actorState == ActorLifecycleState.STOPPED
        ) {
            return
        }

        sharedSessionMutex.withLock {
            if (destroyRequested.get() ||
                generation < latestStartGeneration.get() ||
                generation < activeRunGeneration ||
                actorState == ActorLifecycleState.STOPPED
            ) {
                return@withLock
            }

            val currentSensor = resolveEngine().readCurrentState()
            if (currentSensor.isCharging) {
                if (activeSessionId == null) {
                    recoverOrStartChargingSession(currentSensor)
                } else if (samplePersistenceHalted || checkpointSavePendingRetry) {
                    val isScreenOn = runCatching { powerManager.isInteractive }.getOrDefault(false)
                    retryPendingCheckpointAndFinalizationLocked(isScreenOn)
                }
                transitionActorState(ActorLifecycleState.RUNNING_CHARGING)
                ensureMonitoringLoopRunning()
            }
        }
    }

    private suspend fun handleDestroyInActor() {
        withContext(NonCancellable) {
            var keepInMemoryForRamOnlyFailure = false
            sharedSessionMutex.withLock {
                if (!destroyDbCleanupSucceeded && !destroyFinalizationInProgress) {
                    destroyFinalizationInProgress = true
                    try {
                        val result = ensureSessionFinalizedSafely()
                        when (result) {
                            SessionFinalizationResult.COMPLETE_DATA_FINALIZED,
                            SessionFinalizationResult.ALREADY_CLOSED_IDEMPOTENT,
                            SessionFinalizationResult.NO_ACTIVE_SESSION_CLEAN -> {
                                destroyDbCleanupSucceeded = true
                                destroyFullDataFinalized = true
                            }
                            SessionFinalizationResult.CLEANUP_ONLY_DATA_INCOMPLETE -> {
                                destroyDbCleanupSucceeded = true
                                destroyFullDataFinalized = false
                                samplePersistenceHalted = true
                                Log.w(
                                    "PowerGuard",
                                    "Destroy selesai dengan checkpoint tersimpan; data sesi lengkap akan direkonsiliasi saat recovery berikutnya"
                                )
                            }
                            SessionFinalizationResult.TARGET_MISSING_NEEDS_FOLLOW_UP,
                            SessionFinalizationResult.DATABASE_UNAVAILABLE -> {
                                destroyDbCleanupSucceeded = false
                                destroyFullDataFinalized = false
                                samplePersistenceHalted = true
                                keepInMemoryForRamOnlyFailure = hasUnpersistedRamOnlyData(result)
                                Log.w(
                                    "PowerGuard",
                                    "Destroy mempertahankan checkpoint persisten untuk tindak lanjut (result=$result, activeSessionId=$activeSessionId, keepInRam=$keepInMemoryForRamOnlyFailure)"
                                )
                            }
                            SessionFinalizationResult.FAILED -> {
                                destroyDbCleanupSucceeded = false
                                destroyFullDataFinalized = false
                                samplePersistenceHalted = true
                                keepInMemoryForRamOnlyFailure = hasRecoverableRamOnlyData()
                                Log.e(
                                    "PowerGuard",
                                    "CRITICAL: Finalisasi dan checkpoint gagal saat destroy; data sesi (activeSessionId=$activeSessionId, buffered=${sampleBuffer.size}, accumulatedMah=$accumulatedMah) dipertahankan di memori"
                                )
                            }
                        }
                    } finally {
                        destroyFinalizationInProgress = false
                    }
                }

                stopMonitoringLoopLocked()
                activeRunGeneration = 0L
                manualMonitoringRequested = false
                transitionActorState(ActorLifecycleState.STOPPED)
                if (!keepInMemoryForRamOnlyFailure) {
                    synchronized(lifecycleLock) {
                        if (activeInstance == null || activeInstance === this@BatteryMonitorService) {
                            _liveState.value = LiveBatteryState()
                        }
                    }
                }
            }
        }
    }

    /**
     * Fallback penutupan sesi saat finalisasi utama gagal.
     * Menangani hasil penyimpanan checkpoint secara ketat per sessionId:
     * - Checkpoint selalu disimpan dan diverifikasi SEBELUM penutupan fallback dilakukan.
     * - Jika penyimpanan checkpoint gagal, fallback TIDAK boleh menutup sesi secara destruktif,
     *   TIDAK boleh menghapus sampleBuffer, dan TIDAK boleh mereset state sesi in-memory.
     * - Bahkan ketika fallback menutup row target di DB, state in-memory tetap dipertahankan
     *   sampai seluruh sampel/metrik direkonsiliasi ke Room.
     * Dipanggil eksklusif di dalam sharedSessionMutex.withLock.
     */
    private suspend fun executeFallbackSessionClosure(): FallbackClosureStatus = withContext(NonCancellable) {
        val dao = resolveDao()
        val now = System.currentTimeMillis()
        val fallbackLevel = readSafeBatteryLevelForFinalization()
        val targetId = activeSessionId

        if (targetId == null) {
            return@withContext when (reconcilePersistedRecoveryIfAny(dao)) {
                PersistedRecoveryReconcileStatus.NO_CHECKPOINT,
                PersistedRecoveryReconcileStatus.RECONCILED_AND_CLEARED,
                PersistedRecoveryReconcileStatus.RECONCILED_CHECKPOINT_CLEAR_PENDING ->
                    FallbackClosureStatus.FULL_DATA_SAVED_AND_CLEANED
                PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP ->
                    FallbackClosureStatus.TARGET_MISSING_FAILED
                PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE ->
                    FallbackClosureStatus.DATABASE_UNAVAILABLE_FAILED
                PersistedRecoveryReconcileStatus.CHECKPOINT_CORRUPT,
                PersistedRecoveryReconcileStatus.CHECKPOINT_UNAVAILABLE ->
                    FallbackClosureStatus.FAILED
            }
        }

        // Langkah 1: Upayakan penyimpanan sampel tertunda & finalisasi lengkap untuk targetId
        val preCleanupSaved = tryAttemptPendingDataFlushAndFinalization(
            targetId = targetId,
            endTime = now,
            endLevel = fallbackLevel
        )
        if (preCleanupSaved) {
            finalizedSessionIds.add(targetId)
            val cleared = clearCheckpointIfNoLongerNeeded(targetId)
            if (!cleared) {
                Log.e(
                    "PowerGuard",
                    "Fallback finalisasi lengkap sukses untuk sessionId=$targetId, namun penghapusan checkpoint gagal; mempertahankan snapshot sampai clear terverifikasi"
                )
            }
            resetInMemorySessionTracking(clearRecoverySnapshot = cleared)
            return@withContext FallbackClosureStatus.FULL_DATA_SAVED_AND_CLEANED
        }

        // Langkah 2: Simpan checkpoint recovery SEBELUM mencoba penutupan fallback pada targetId.
        val hasUnsavedInMemoryState = hasRecoverableRamOnlyData()
        if (hasUnsavedInMemoryState) {
            val checkpointSaved = captureIncompleteRecoveryMetadata(
                sessionId = targetId,
                dbSessionsClosed = false,
                reason = "Finalisasi utama gagal; menyimpan checkpoint sebelum fallback untuk sessionId=$targetId",
                recoveryStatus = SessionRecoveryStore.RecoveryStatus.PENDING_RECONCILIATION
            )
            if (!checkpointSaved) {
                Log.e(
                    "PowerGuard",
                    "CRITICAL: SessionRecoveryStore.save() gagal pada fallback sessionId=$targetId; mempertahankan buffer (${sampleBuffer.size} sampel) dan state sesi di memori tanpa menutup sesi secara destruktif"
                )
                return@withContext FallbackClosureStatus.FAILED
            }
        }

        // Langkah 3: Periksa keterjangkauan DB & keberadaan target sebelum mencoba penutupan tunggal
        when (val lookup = lookupTargetSessionSafely(dao, targetId)) {
            is TargetSessionLookupResult.DatabaseInaccessible -> {
                val dbReason = "Fallback closure ditunda: database Room tidak tersedia untuk targetId=$targetId (${lookup.cause?.message ?: "unknown"})"
                val existingPersisted = SessionRecoveryStore.loadForSessionOrNull(applicationContext, targetId)
                if (existingPersisted != null) {
                    preserveRecoveryMetadataFromPersisted(
                        persisted = existingPersisted,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                        reason = dbReason
                    )
                    SessionRecoveryStore.markRequiresFollowUp(
                        applicationContext,
                        expectedSessionId = targetId,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                        reason = dbReason
                    )
                } else {
                    captureIncompleteRecoveryMetadata(
                        sessionId = targetId,
                        dbSessionsClosed = false,
                        reason = dbReason,
                        recoveryStatus = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE
                    )
                }
                lastReconcileStatus = PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE
                return@withContext FallbackClosureStatus.DATABASE_UNAVAILABLE_FAILED
            }

            is TargetSessionLookupResult.MissingInAccessibleDb -> {
                val followUpReason = "Fallback closure gagal: target sessionId=$targetId tidak ditemukan di Room; checkpoint, sampleBuffer, dan accumulator dipertahankan"
                val existingPersisted = SessionRecoveryStore.loadForSessionOrNull(applicationContext, targetId)
                if (hasUnsavedInMemoryState || existingPersisted == null) {
                    captureIncompleteRecoveryMetadata(
                        sessionId = targetId,
                        dbSessionsClosed = false,
                        reason = followUpReason,
                        recoveryStatus = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING
                    )
                } else {
                    preserveRecoveryMetadataFromPersisted(
                        persisted = existingPersisted,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                        reason = followUpReason
                    )
                    SessionRecoveryStore.markRequiresFollowUp(
                        applicationContext,
                        expectedSessionId = targetId,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                        reason = followUpReason
                    )
                }
                lastReconcileStatus = PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP
                Log.e(
                    "PowerGuard",
                    "CRITICAL: Fallback single-session menemukan TARGET_MISSING untuk targetId=$targetId; mempertahankan checkpoint, sampleBuffer (${sampleBuffer.size}), dan accumulator tanpa reset destruktif"
                )
                return@withContext FallbackClosureStatus.TARGET_MISSING_FAILED
            }

            is TargetSessionLookupResult.Found -> Unit
        }

        // Langkah 4: Checkpoint sudah aman di disk; coba tutup HANYA targetId dengan metrik konsisten
        return@withContext try {
            val singleStatus = dao.finalizeSingleSessionWithReconciledMetrics(
                sessionId = targetId,
                endTime = now,
                endLevel = fallbackLevel,
                designCapacityMah = resolveEngine().designCapacityMah
            )
            val verifiedRow = runCatching { dao.getSessionById(targetId) }.getOrNull()
            val targetClosed = (singleStatus == SessionFinalizationStatus.FINALIZED ||
                singleStatus == SessionFinalizationStatus.ALREADY_CLOSED) &&
                verifiedRow?.endTime != null

            if (targetClosed) {
                if (hasUnsavedInMemoryState) {
                    val marked = SessionRecoveryStore.markDbSessionsClosed(
                        applicationContext,
                        sessionId = targetId
                    )
                    if (!marked) {
                        Log.e(
                            "PowerGuard",
                            "Sesi targetId=$targetId ditutup di DB, tetapi gagal memperbarui flag dbSessionsClosed pada checkpoint"
                        )
                    }
                    // Durability Gate: JANGAN reset state in-memory sebelum data sampel/metrik direkonsiliasi ke Room!
                    Log.w(
                        "PowerGuard",
                        "Fallback DB menutup sesi targetId=$targetId; checkpoint persisten dan buffer in-memory dipertahankan sampai rekonsiliasi penuh berhasil"
                    )
                    FallbackClosureStatus.CLEANUP_ONLY_DATA_INCOMPLETE
                } else {
                    val cleared = clearCheckpointIfNoLongerNeeded(targetId)
                    resetInMemorySessionTracking(clearRecoverySnapshot = cleared)
                    FallbackClosureStatus.FULL_DATA_SAVED_AND_CLEANED
                }
            } else if (singleStatus == SessionFinalizationStatus.TARGET_MISSING) {
                val followUpReason = "Fallback closure gagal: target sessionId=$targetId hilang saat penutupan; checkpoint dan data sesi dipertahankan"
                val existingPersisted = SessionRecoveryStore.loadForSessionOrNull(applicationContext, targetId)
                if (hasUnsavedInMemoryState || existingPersisted == null) {
                    captureIncompleteRecoveryMetadata(
                        sessionId = targetId,
                        dbSessionsClosed = false,
                        reason = followUpReason,
                        recoveryStatus = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING
                    )
                } else {
                    preserveRecoveryMetadataFromPersisted(
                        persisted = existingPersisted,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                        reason = followUpReason
                    )
                    SessionRecoveryStore.markRequiresFollowUp(
                        applicationContext,
                        expectedSessionId = targetId,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                        reason = followUpReason
                    )
                }
                lastReconcileStatus = PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP
                FallbackClosureStatus.TARGET_MISSING_FAILED
            } else {
                Log.e(
                    "PowerGuard",
                    "Fallback single-session gagal menutup targetId=$targetId (status=$singleStatus); checkpoint persisten tetap dipertahankan"
                )
                FallbackClosureStatus.FAILED
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (fallbackError: Throwable) {
            Log.e(
                "PowerGuard",
                "Fallback single-session exception untuk targetId=$targetId: ${fallbackError.message}; checkpoint persisten dipertahankan",
                fallbackError
            )
            // Jika target sudah tertutup, kegagalan rekonsiliasi adalah kegagalan finalisasi data,
            // bukan bukti bahwa database tidak tersedia. Pertahankan status FAILED yang spesifik.
            val targetAlreadyClosed = runCatching {
                dao.getSessionById(targetId)?.endTime != null
            }.getOrDefault(false)
            if (targetAlreadyClosed) {
                FallbackClosureStatus.FAILED
            } else {
                FallbackClosureStatus.DATABASE_UNAVAILABLE_FAILED
            }
        }
    }

    private suspend fun tryAttemptPendingDataFlushAndFinalization(
        targetId: Long,
        endTime: Long,
        endLevel: Int
    ): Boolean = withContext(NonCancellable) {
        return@withContext try {
            val dao = resolveDao()
            val existingSession = dao.getSessionById(targetId)
            if (existingSession == null || existingSession.endTime != null) {
                return@withContext false
            }

            val overallAvgMa = if (currentReadingsCount > 0) (currentSumForAvg / currentReadingsCount).toInt() else 0
            val deepSleepRatio = runCatching { resolveEngine().calculateDeepSleepRatio() }
                .getOrDefault(existingSession.deepSleepRatio)
            val anomalyReason = alertedAnomalyCodes.joinToString(", ").ifEmpty { null }
            val hasAnomaly = alertedAnomalyCodes.isNotEmpty()
            val pendingEntriesSnapshot = sampleBuffer.toList()
            val pendingSample = buildPendingFinalSampleEntity(targetId, endTime)
            val healthLog = buildHealthLogEntityIfEligible(targetId, existingSession, endLevel, endTime)

            // Upaya A: Transaksi finalisasi lengkap beserta pendingSample dan healthLog
            val fullAttempt = runCatching {
                dao.finalizeSessionAtomically(
                    sessionId = targetId,
                    endTime = endTime,
                    endLevel = endLevel,
                    totalMahDelivered = accumulatedMah,
                    avgCurrentMa = overallAvgMa,
                    maxTempCelsius = maxSessionTemp,
                    hasAnomaly = hasAnomaly,
                    anomalyReason = anomalyReason,
                    deepSleepRatio = deepSleepRatio,
                    finalSample = pendingSample,
                    healthLog = healthLog
                )
            }.getOrNull()

            if (fullAttempt == SessionFinalizationStatus.FINALIZED) {
                val verifiedRow = runCatching { dao.getSessionById(targetId) }.getOrNull()
                val sampleVerified = pendingSample == null ||
                    runCatching { dao.isSamplePersisted(targetId, pendingSample.timestamp) }.getOrDefault(false)
                if (verifiedRow?.endTime != null && sampleVerified) {
                    removePersistedBufferEntries(pendingEntriesSnapshot)
                    return@withContext true
                }
            }

            // Upaya B: Simpan batch sampel tertunda terlebih dahulu secara atomik & idempotent,
            // lalu finalisasi sesi tanpa lampiran opsional jika fullAttempt gagal karena lampiran
            if (pendingSample != null) {
                val sampleSaved = runCatching {
                    dao.persistSampleAndUpdateSessionAtomically(
                        sample = pendingSample,
                        totalMahDelivered = accumulatedMah,
                        avgCurrentMa = overallAvgMa,
                        maxTempCelsius = maxSessionTemp,
                        hasAnomaly = hasAnomaly,
                        anomalyReason = anomalyReason,
                        deepSleepRatio = deepSleepRatio
                    )
                }.getOrNull()

                val sampleVerified = (sampleSaved == SampleFlushStatus.FLUSHED ||
                    sampleSaved == SampleFlushStatus.ALREADY_FLUSHED_IDEMPOTENT) &&
                    runCatching { dao.isSamplePersisted(targetId, pendingSample.timestamp) }.getOrDefault(false)

                if (!sampleVerified) {
                    return@withContext false
                }

            }

            val sessionOnlyAttempt = runCatching {
                dao.finalizeSessionAtomically(
                    sessionId = targetId,
                    endTime = endTime,
                    endLevel = endLevel,
                    totalMahDelivered = accumulatedMah,
                    avgCurrentMa = overallAvgMa,
                    maxTempCelsius = maxSessionTemp,
                    hasAnomaly = hasAnomaly,
                    anomalyReason = anomalyReason,
                    deepSleepRatio = deepSleepRatio,
                    finalSample = null,
                    healthLog = null
                )
            }.getOrNull()

            if (sessionOnlyAttempt == SessionFinalizationStatus.FINALIZED) {
                val verifiedRow = runCatching { dao.getSessionById(targetId) }.getOrNull()
                val finalized = verifiedRow?.endTime != null
                if (finalized) {
                    // Pertahankan buffer sampai penutupan sesi terverifikasi; retry tetap idempotent
                    // jika sampel sudah lebih dulu tersimpan pada Upaya B.
                    removePersistedBufferEntries(pendingEntriesSnapshot)
                }
                finalized
            } else {
                false
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            false
        }
    }

    private fun computeWeightedBatchMetrics(entries: List<BufferedSampleEntry>): WeightedBatchMetrics {
        val totalWeight = entries.sumOf { it.weight.coerceAtLeast(1) }.coerceAtLeast(1)
        val startTs = entries.first().timestampMs
        val endTs = entries.maxOf { maxOf(it.timestampMs, it.endTimestampMs) }
        val spanDurationMs = (endTs - startTs).coerceAtLeast(0L)
        val weightedCurrentMaSum = entries.sumOf { it.weightedCurrentMaSum }
        val weightedVoltageMvSum = entries.sumOf { it.weightedVoltageMvSum }
        val weightedTempCelsiusSum = entries.sumOf { it.weightedTempCelsiusSum }
        val weightedPowerWattSum = entries.sumOf { it.weightedPowerWattSum }
        val weightedDeepSleepRatioSum = entries.sumOf { it.weightedDeepSleepRatioSum }
        val weightedCurrentMa = (weightedCurrentMaSum / totalWeight).toInt()
        val weightedVoltageMv = (weightedVoltageMvSum / totalWeight).toInt()
        val weightedTempCelsius = (weightedTempCelsiusSum / totalWeight).toFloat()
        val maxTempCelsius = entries.maxOf { maxOf(it.maxTempCelsius, it.state.tempCelsius) }
        val weightedPowerWatt = (weightedPowerWattSum / totalWeight).toFloat()
        val weightedDeepSleepRatio = (weightedDeepSleepRatioSum / totalWeight).toFloat().coerceIn(0f, 1f)
        return WeightedBatchMetrics(
            totalWeight = totalWeight,
            spanDurationMs = spanDurationMs,
            weightedCurrentMaSum = weightedCurrentMaSum,
            weightedVoltageMvSum = weightedVoltageMvSum,
            weightedTempCelsiusSum = weightedTempCelsiusSum,
            weightedPowerWattSum = weightedPowerWattSum,
            weightedDeepSleepRatioSum = weightedDeepSleepRatioSum,
            weightedCurrentMa = weightedCurrentMa,
            weightedVoltageMv = weightedVoltageMv,
            weightedTempCelsius = weightedTempCelsius,
            maxTempCelsius = maxTempCelsius,
            weightedPowerWatt = weightedPowerWatt,
            weightedDeepSleepRatio = weightedDeepSleepRatio,
            latestState = entries.last().state
        )
    }

    private fun removePersistedBufferEntries(persistedEntries: List<BufferedSampleEntry>) {
        if (persistedEntries.isEmpty()) return
        val persistedSeqIds = persistedEntries.map { it.seqId }.toSet()
        sampleBuffer.removeAll { it.seqId in persistedSeqIds }
        pendingFlushBatchTimestampMs = sampleBuffer.firstOrNull()?.timestampMs ?: 0L
    }

    private fun clearCheckpointIfNoLongerNeeded(sessionId: Long): Boolean {
        return when (val current = SessionRecoveryStore.loadForSession(applicationContext, sessionId)) {
            is SessionRecoveryStore.CheckpointLoadResult.Empty -> true
            is SessionRecoveryStore.CheckpointLoadResult.Loaded -> {
                if (current.recovery.sessionId == sessionId) {
                    SessionRecoveryStore.clear(applicationContext, expectedSessionId = sessionId)
                } else {
                    false
                }
            }
            is SessionRecoveryStore.CheckpointLoadResult.Corrupt,
            is SessionRecoveryStore.CheckpointLoadResult.Unavailable -> false
        }
    }

    /**
     * Menyimpan checkpoint recovery secara sinkron ke SessionRecoveryStore untuk [sessionId]
     * dengan validasi kepemilikan eksplisit. Jika gagal commit/verifikasi atau ditolak,
     * snapshot dipertahankan di memori dan flag retry diaktifkan.
     */
    private fun captureIncompleteRecoveryMetadata(
        sessionId: Long,
        dbSessionsClosed: Boolean,
        reason: String,
        recoveryStatus: SessionRecoveryStore.RecoveryStatus = SessionRecoveryStore.RecoveryStatus.PENDING_RECONCILIATION
    ): Boolean {
        val entriesSnapshot = sampleBuffer.toList()
        val stateSnapshot = entriesSnapshot.map { it.state }
        val totalWeight = entriesSnapshot.sumOf { it.weight.coerceAtLeast(1) }
        val spanDurationMs = if (entriesSnapshot.isNotEmpty()) {
            val startTs = entriesSnapshot.first().timestampMs
            val endTs = entriesSnapshot.maxOf { maxOf(it.timestampMs, it.endTimestampMs) }
            (endTs - startTs).coerceAtLeast(0L)
        } else {
            0L
        }
        val batchTs = if (entriesSnapshot.isNotEmpty() && pendingFlushBatchTimestampMs <= 0L) {
            entriesSnapshot.first().timestampMs.also { pendingFlushBatchTimestampMs = it }
        } else {
            pendingFlushBatchTimestampMs
        }

        lastIncompleteSessionRecovery = UnpersistedSessionRecoveryMetadata(
            sessionId = sessionId,
            bufferedSamplesCount = entriesSnapshot.size,
            bufferedSamplesSnapshot = stateSnapshot,
            bufferedTotalSampleWeight = totalWeight,
            bufferedSampleSpanDurationMs = spanDurationMs,
            pendingBatchTimestampMs = batchTs,
            accumulatedMah = accumulatedMah,
            sessionSeconds = sessionSeconds,
            maxSessionTemp = maxSessionTemp,
            currentSumForAvg = currentSumForAvg,
            currentReadingsCount = currentReadingsCount,
            alertedAnomalyCodes = alertedAnomalyCodes.toSet(),
            dbSessionsClosed = dbSessionsClosed,
            failureReason = reason,
            requiresFollowUp = recoveryStatus != SessionRecoveryStore.RecoveryStatus.PENDING_RECONCILIATION,
            recoveryStatus = recoveryStatus
        )

        val compactedSample = if (entriesSnapshot.isNotEmpty()) {
            val metrics = computeWeightedBatchMetrics(entriesSnapshot)
            SessionRecoveryStore.PersistedSessionRecovery(
                sessionId = sessionId,
                accumulatedMah = accumulatedMah,
                sessionSeconds = sessionSeconds,
                maxSessionTemp = maxOf(maxSessionTemp, metrics.maxTempCelsius),
                currentSumForAvg = currentSumForAvg,
                currentReadingsCount = currentReadingsCount,
                alertedAnomalyCodes = alertedAnomalyCodes.toSet(),
                pendingBatchTimestampMs = batchTs,
                hasPendingSample = true,
                sampleBatteryLevel = metrics.latestState.level,
                sampleCurrentMa = metrics.weightedCurrentMa,
                sampleVoltageMv = metrics.weightedVoltageMv,
                sampleTempCelsius = metrics.weightedTempCelsius,
                samplePowerWatt = metrics.weightedPowerWatt,
                sampleIsScreenOn = runCatching { powerManager.isInteractive }.getOrDefault(false),
                sampleWeight = metrics.totalWeight,
                sampleSpanDurationMs = metrics.spanDurationMs,
                dbSessionsClosed = dbSessionsClosed,
                failureReason = reason,
                recoveryStatus = recoveryStatus
            )
        } else {
            SessionRecoveryStore.PersistedSessionRecovery(
                sessionId = sessionId,
                accumulatedMah = accumulatedMah,
                sessionSeconds = sessionSeconds,
                maxSessionTemp = maxSessionTemp,
                currentSumForAvg = currentSumForAvg,
                currentReadingsCount = currentReadingsCount,
                alertedAnomalyCodes = alertedAnomalyCodes.toSet(),
                pendingBatchTimestampMs = 0L,
                hasPendingSample = false,
                sampleBatteryLevel = 0,
                sampleCurrentMa = 0,
                sampleVoltageMv = 0,
                sampleTempCelsius = 0f,
                samplePowerWatt = 0f,
                sampleIsScreenOn = false,
                sampleWeight = 1,
                sampleSpanDurationMs = 0L,
                dbSessionsClosed = dbSessionsClosed,
                failureReason = reason,
                recoveryStatus = recoveryStatus
            )
        }

        val saveResult = SessionRecoveryStore.saveForSession(
            context = applicationContext,
            expectedOwnerSessionId = sessionId,
            recovery = compactedSample
        )
        val saved = saveResult.isSuccess
        checkpointSavePendingRetry = !saved
        if (!saved) {
            Log.e(
                "PowerGuard",
                "CRITICAL: SessionRecoveryStore.saveForSession() gagal (status=${saveResult.status}, existingOwner=${saveResult.existingOwnerSessionId}, sessionId=$sessionId, reason=$reason): ${saveResult.message}"
            )
        }
        return saved
    }

    private fun preserveRecoveryMetadataFromPersisted(
        persisted: SessionRecoveryStore.PersistedSessionRecovery,
        status: SessionRecoveryStore.RecoveryStatus,
        reason: String
    ) {
        val existingInMemory = lastIncompleteSessionRecovery?.takeIf { it.sessionId == persisted.sessionId }
        val inMemorySamples = when {
            activeSessionId == persisted.sessionId && sampleBuffer.isNotEmpty() -> sampleBuffer.map { it.state }
            existingInMemory != null && existingInMemory.bufferedSamplesSnapshot.isNotEmpty() -> existingInMemory.bufferedSamplesSnapshot
            else -> listOfNotNull(persisted.toCompactedLiveBatteryState())
        }
        val inMemoryWeight = when {
            activeSessionId == persisted.sessionId && sampleBuffer.isNotEmpty() -> sampleBuffer.sumOf { it.weight.coerceAtLeast(1) }
            existingInMemory != null && existingInMemory.bufferedTotalSampleWeight > 0 -> existingInMemory.bufferedTotalSampleWeight
            persisted.hasPendingSample -> persisted.sampleWeight.coerceAtLeast(1)
            else -> 0
        }
        val inMemorySpanMs = when {
            activeSessionId == persisted.sessionId && sampleBuffer.isNotEmpty() -> {
                val startTs = sampleBuffer.first().timestampMs
                val endTs = sampleBuffer.maxOf { maxOf(it.timestampMs, it.endTimestampMs) }
                (endTs - startTs).coerceAtLeast(0L)
            }
            existingInMemory != null && existingInMemory.bufferedSampleSpanDurationMs > 0L -> existingInMemory.bufferedSampleSpanDurationMs
            persisted.hasPendingSample -> persisted.sampleSpanDurationMs.coerceAtLeast(0L)
            else -> 0L
        }
        val resolvedBatchTs = when {
            activeSessionId == persisted.sessionId && pendingFlushBatchTimestampMs > 0L -> pendingFlushBatchTimestampMs
            existingInMemory != null && existingInMemory.pendingBatchTimestampMs > 0L -> existingInMemory.pendingBatchTimestampMs
            else -> persisted.pendingBatchTimestampMs
        }
        val resolvedMah = maxOf(
            persisted.accumulatedMah,
            if (activeSessionId == persisted.sessionId) accumulatedMah else 0.0,
            existingInMemory?.accumulatedMah ?: 0.0
        )
        val resolvedSeconds = maxOf(
            persisted.sessionSeconds,
            if (activeSessionId == persisted.sessionId) sessionSeconds else 0L,
            existingInMemory?.sessionSeconds ?: 0L
        )
        val resolvedMaxTemp = maxOf(
            persisted.maxSessionTemp,
            if (activeSessionId == persisted.sessionId) maxSessionTemp else 0f,
            existingInMemory?.maxSessionTemp ?: 0f
        )
        val resolvedCurrentCount = maxOf(
            persisted.currentReadingsCount,
            if (activeSessionId == persisted.sessionId) currentReadingsCount else 0L,
            existingInMemory?.currentReadingsCount ?: 0L
        )
        val resolvedCurrentSum = when {
            activeSessionId == persisted.sessionId && currentReadingsCount >= resolvedCurrentCount && currentReadingsCount > 0L -> currentSumForAvg
            existingInMemory != null && existingInMemory.currentReadingsCount >= resolvedCurrentCount && existingInMemory.currentReadingsCount > 0L -> existingInMemory.currentSumForAvg
            else -> persisted.currentSumForAvg
        }
        val resolvedAnomalies = buildSet {
            addAll(persisted.alertedAnomalyCodes)
            if (activeSessionId == persisted.sessionId) addAll(alertedAnomalyCodes)
            existingInMemory?.alertedAnomalyCodes?.let { addAll(it) }
        }

        lastIncompleteSessionRecovery = UnpersistedSessionRecoveryMetadata(
            sessionId = persisted.sessionId,
            bufferedSamplesCount = inMemorySamples.size,
            bufferedSamplesSnapshot = inMemorySamples,
            bufferedTotalSampleWeight = inMemoryWeight,
            bufferedSampleSpanDurationMs = inMemorySpanMs,
            pendingBatchTimestampMs = resolvedBatchTs,
            accumulatedMah = resolvedMah,
            sessionSeconds = resolvedSeconds,
            maxSessionTemp = resolvedMaxTemp,
            currentSumForAvg = resolvedCurrentSum,
            currentReadingsCount = resolvedCurrentCount,
            alertedAnomalyCodes = resolvedAnomalies,
            dbSessionsClosed = persisted.dbSessionsClosed,
            failureReason = reason,
            requiresFollowUp = status != SessionRecoveryStore.RecoveryStatus.PENDING_RECONCILIATION,
            recoveryStatus = status
        )
    }

    private fun readSafeBatteryLevelForFinalization(): Int {
        // Sampel terakhir yang sudah diterima service lebih representatif daripada pembacaan
        // sensor baru yang dapat gagal/bernilai default ketika lifecycle sedang dihentikan.
        sampleBuffer.lastOrNull()?.state?.level?.takeIf { it in 0..100 }?.let { return it }
        _liveState.value.level.takeIf { it in 0..100 && it > 0 }?.let { return it }
        return try {
            resolveEngine().readCurrentState().level.coerceIn(0, 100)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            0
        }
    }

    private fun resetInMemorySessionTracking(clearRecoverySnapshot: Boolean = true) {
        activeSessionId = null
        sampleBuffer.clear()
        pendingFlushBatchTimestampMs = 0L
        consecutiveFlushFailures = 0
        nextFlushRetryAtSec = 0L
        checkpointSavePendingRetry = false
        accumulatedMah = 0.0
        sessionSeconds = 0L
        maxSessionTemp = 0f
        currentSumForAvg = 0L
        currentReadingsCount = 0L
        alertedAnomalyCodes.clear()
        if (clearRecoverySnapshot) {
            lastIncompleteSessionRecovery = null
        }
    }

    private fun stopForegroundAndSelfIfIdle(targetCommandId: Int, targetGeneration: Long) {
        synchronized(lifecycleLock) {
            if (destroyRequested.get()) return
            if (actorState != ActorLifecycleState.STOPPED) return
            if (hasPendingStarts()) return
            if (latestStartGeneration.get() > targetGeneration) return
            if (instanceLatestStartCommandId.get() > targetCommandId) return
            val stopIdToUse = maxOf(targetCommandId, instanceLatestObservedCommandId.get())

            stopForeground(STOP_FOREGROUND_REMOVE)
            if (stopIdToUse > 0) {
                stopSelf(stopIdToUse)
            } else {
                stopSelf()
            }
        }
    }

    private fun stopSelfIfStaleAndIdle(startId: Int, failedGeneration: Long) {
        synchronized(lifecycleLock) {
            if (destroyRequested.get()) return
            if (hasPendingStarts()) return
            if (latestStartGeneration.get() > failedGeneration) return
            if (instanceLatestStartCommandId.get() > startId) return
            if (actorState == ActorLifecycleState.RUNNING_CHARGING ||
                actorState == ActorLifecycleState.RUNNING_MANUAL ||
                actorState == ActorLifecycleState.RECOVERY_HOLD
            ) {
                return
            }
            if (startId > 0) {
                stopSelf(startId)
            }
        }
    }

    private fun ensureMonitoringLoopRunning() {
        if (monitorJob?.isActive == true) return
        val runGen = activeRunGeneration
        monitorJob = serviceScope.launch {
            runMonitoringLoop(runGen)
        }
    }

    private fun stopMonitoringLoopLocked() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun runMonitoringLoop(loopGeneration: Long) {
        var manualTicks = 0L
        var holdTicks = 0L
        var lastChargerTransitionGen = -1L

        while (serviceScope.isActive && !destroyRequested.get()) {
            try {
                val isScreenOn = powerManager.isInteractive
                var notificationSummaryToPost: String? = null
                var autoStopGenerationToSend: Long? = null
                var chargerConnectedGenerationToSend: Long? = null
                var stopLoopForStaleOrStopped = false

                sharedSessionMutex.withLock {
                    if (destroyRequested.get() ||
                        loopGeneration != activeRunGeneration ||
                        actorState == ActorLifecycleState.STOPPED
                    ) {
                        stopLoopForStaleOrStopped = true
                        return@withLock
                    }

                    if (actorState == ActorLifecycleState.RECOVERY_HOLD) {
                        holdTicks++
                        val currentSensor = resolveEngine().readCurrentState(
                            sessionMah = accumulatedMah,
                            sessionDurationSec = sessionSeconds
                        )
                        if (currentSensor.isCharging) {
                            val currentGen = maxOf(loopGeneration, latestStartGeneration.get())
                            chargerConnectedGenerationToSend = currentGen
                            return@withLock
                        }

                        val finalizationResult = ensureSessionFinalizedSafely()
                        if (!hasUnpersistedRamOnlyData(finalizationResult)) {
                            autoStopGenerationToSend = maxOf(loopGeneration, latestStartGeneration.get())
                            return@withLock
                        }
                        if (holdTicks % 3L == 0L) {
                            notificationSummaryToPost = "Menahan layanan: mencoba ulang penyimpanan data sesi..."
                        }
                        return@withLock
                    }

                    var effectiveSensor = resolveEngine().readCurrentState(
                        sessionMah = accumulatedMah,
                        sessionDurationSec = sessionSeconds
                    )
                    val currentGen = maxOf(loopGeneration, latestStartGeneration.get())

                    if (effectiveSensor.isCharging) {
                        if (actorState == ActorLifecycleState.RUNNING_MANUAL || activeSessionId == null) {
                            if (lastChargerTransitionGen != currentGen) {
                                lastChargerTransitionGen = currentGen
                                chargerConnectedGenerationToSend = currentGen
                            }
                        }
                    } else {
                        lastChargerTransitionGen = -1L
                        if (actorState == ActorLifecycleState.RUNNING_CHARGING || activeSessionId != null) {
                            val confirmState = resolveEngine().readCurrentState(
                                sessionMah = accumulatedMah,
                                sessionDurationSec = sessionSeconds
                            )
                            if (!confirmState.isCharging) {
                                autoStopGenerationToSend = currentGen
                            } else {
                                effectiveSensor = confirmState
                            }
                        }
                    }

                    if (autoStopGenerationToSend == null) {
                        var activeAlertMsg: String? = null
                        if (activeSessionId != null) {
                            sessionSeconds++
                            accumulatedMah += (effectiveSensor.currentMa / 3600.0)
                            if (effectiveSensor.tempCelsius > maxSessionTemp) {
                                maxSessionTemp = effectiveSensor.tempCelsius
                            }
                            currentSumForAvg += effectiveSensor.currentMa
                            currentReadingsCount++

                            val anomaly = anomalyDetector.evaluate(effectiveSensor, isScreenOn)
                            if (anomaly.isAnomaly && anomaly.code != null && anomaly.message != null) {
                                activeAlertMsg = anomaly.message
                                if (alertedAnomalyCodes.add(anomaly.code)) {
                                    sendAnomalyAlertNotification(anomaly.message)
                                }
                            }
                        } else {
                            manualTicks++
                        }

                        val updatedState = effectiveSensor.copy(
                            sessionMahDelivered = accumulatedMah,
                            sessionDurationSec = sessionSeconds,
                            anomalyAlert = activeAlertMsg
                        )
                        _liveState.value = updatedState

                        if (activeSessionId != null && checkpointSavePendingRetry) {
                            captureIncompleteRecoveryMetadata(
                                sessionId = activeSessionId!!,
                                dbSessionsClosed = false,
                                reason = "Retry penyimpanan checkpoint recovery yang tertunda"
                            )
                        }

                        if (activeSessionId != null) {
                            if (samplePersistenceHalted && sessionSeconds >= nextFlushRetryAtSec) {
                                retryPendingCheckpointAndFinalizationLocked(isScreenOn)
                            }
                            // Tetap rekam sampel ke bounded buffer di RAM meskipun persistensi Room sedang tertahan
                            appendSampleToBoundedBuffer(updatedState)
                            if (!samplePersistenceHalted &&
                                sampleBuffer.size >= SAMPLE_FLUSH_BATCH_SIZE &&
                                sessionSeconds >= nextFlushRetryAtSec
                            ) {
                                flushSampleBufferToDatabase(isScreenOn)
                            }
                        }

                        val tickCounter = if (activeSessionId != null) sessionSeconds else manualTicks
                        if (tickCounter % 3L == 0L) {
                            val sign = if (updatedState.currentMa > 0) "+" else ""
                            val mahText = if (updatedState.isCharging) {
                                String.format(Locale.US, " | Terisi: %+.1f mAh", accumulatedMah)
                            } else ""
                            notificationSummaryToPost = "$sign${updatedState.currentMa} mA (${String.format(Locale.US, "%.1f", updatedState.powerWatt)}W) • ${updatedState.tempCelsius}°C$mahText"
                        }
                    }
                }

                if (stopLoopForStaleOrStopped) {
                    break
                }

                chargerConnectedGenerationToSend?.let { gen ->
                    lifecycleChannel.trySend(LifecycleCommand.ChargerConnected(gen))
                }

                val autoStopGeneration = autoStopGenerationToSend
                if (autoStopGeneration != null) {
                    lifecycleChannel.trySend(LifecycleCommand.AutoStop(autoStopGeneration))
                    break
                }

                notificationSummaryToPost?.let { summary ->
                    notificationManager.notify(MONITOR_NOTIFICATION_ID, buildMonitorNotification(summary))
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (error: Throwable) {
                Log.e("PowerGuard", "Kesalahan iterasi monitoring loop: ${error.message}", error)
            }

            delay(1000L)
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, notification)
        }
    }

    /**
     * Merekonsiliasi seluruh checkpoint recovery persisten multi-sesi ke Room secara terisolasi
     * per sessionId (baik saat sesi masih terbuka endTime == null maupun sudah tertutup endTime != null).
     * Checkpoint persisten HANYA dihapus setelah transaksi Room berhasil dan keadaan DB terverifikasi
     * untuk sessionId terkait, tanpa pernah memengaruhi atau memblokir checkpoint sesi lain.
     * Dipanggil eksklusif di dalam sharedSessionMutex.withLock.
     */
    private suspend fun reconcilePersistedRecoveryIfAny(dao: BatteryDao): PersistedRecoveryReconcileStatus =
        withContext(NonCancellable) {
            val allResults = SessionRecoveryStore.loadAll(applicationContext)
            if (allResults.isEmpty()) {
                lastReconcileStatus = PersistedRecoveryReconcileStatus.NO_CHECKPOINT
                return@withContext PersistedRecoveryReconcileStatus.NO_CHECKPOINT
            }

            var sawUnavailable = false
            var sawCorrupt = false
            var sawDbInaccessible = false
            var sawTargetMissing = false
            var sawClearPending = false

            for (loadResult in allResults) {
                when (loadResult) {
                    is SessionRecoveryStore.CheckpointLoadResult.Empty -> Unit

                    is SessionRecoveryStore.CheckpointLoadResult.Unavailable -> {
                        sawUnavailable = true
                        Log.e(
                            "PowerGuard",
                            "Penyimpanan checkpoint tidak dapat dibaca saat rekonsiliasi: ${loadResult.reason}",
                            loadResult.cause
                        )
                    }

                    is SessionRecoveryStore.CheckpointLoadResult.Corrupt -> {
                        sawCorrupt = true
                        Log.e(
                            "PowerGuard",
                            "Checkpoint persisten terdeteksi CORRUPT (rawSessionId=${loadResult.rawSessionId}): ${loadResult.reason}"
                        )
                    }

                    is SessionRecoveryStore.CheckpointLoadResult.Loaded -> {
                        val persisted = loadResult.recovery
                        when (val lookup = lookupTargetSessionSafely(dao, persisted.sessionId)) {
                            is TargetSessionLookupResult.DatabaseInaccessible -> {
                                sawDbInaccessible = true
                                val reason = "Database Room tidak dapat diakses saat memeriksa target sessionId=${persisted.sessionId}: ${lookup.cause?.message ?: "unknown"}"
                                preserveRecoveryMetadataFromPersisted(
                                    persisted = persisted,
                                    status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                                    reason = reason
                                )
                                SessionRecoveryStore.markRequiresFollowUp(
                                    applicationContext,
                                    expectedSessionId = persisted.sessionId,
                                    status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                                    reason = reason
                                )
                                Log.e(
                                    "PowerGuard",
                                    "Rekonsiliasi recovery ditunda karena database Room tidak dapat diakses (sessionId=${persisted.sessionId}); checkpoint dipertahankan",
                                    lookup.cause
                                )
                            }

                            is TargetSessionLookupResult.MissingInAccessibleDb -> {
                                sawTargetMissing = true
                                val reason = "Target sessionId=${persisted.sessionId} tidak ditemukan di Room dan data checkpoint belum cukup untuk membuat ulang sesi secara aman; membutuhkan penanganan lanjutan"
                                preserveRecoveryMetadataFromPersisted(
                                    persisted = persisted,
                                    status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                                    reason = reason
                                )
                                SessionRecoveryStore.markRequiresFollowUp(
                                    applicationContext,
                                    expectedSessionId = persisted.sessionId,
                                    status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                                    reason = reason
                                )
                                Log.w(
                                    "PowerGuard",
                                    "Target sesi recovery sessionId=${persisted.sessionId} tidak ditemukan di Room; mempertahankan checkpoint dan seluruh metadata recovery dengan status NEEDS_FOLLOW_UP_TARGET_MISSING"
                                )
                            }

                            is TargetSessionLookupResult.Found -> {
                                val existingSession = lookup.session
                                val avgCurrent = if (persisted.currentReadingsCount > 0L) {
                                    (persisted.currentSumForAvg / persisted.currentReadingsCount).toInt()
                                } else {
                                    existingSession.avgCurrentMa
                                }
                                val anomalyReason = persisted.alertedAnomalyCodes.joinToString(", ").ifEmpty { null }
                                val sampleEntity = persisted.toSampleEntityOrNull()

                                val reconciled = runCatching {
                                    dao.reconcileRecoveredSessionAtomically(
                                        sessionId = persisted.sessionId,
                                        recoveredTotalMah = persisted.accumulatedMah,
                                        recoveredAvgCurrentMa = avgCurrent,
                                        recoveredMaxTempCelsius = persisted.maxSessionTemp,
                                        recoveredHasAnomaly = persisted.alertedAnomalyCodes.isNotEmpty(),
                                        recoveredAnomalyReason = anomalyReason,
                                        recoveredSample = sampleEntity
                                    )
                                }.onFailure { error ->
                                    Log.e(
                                        "PowerGuard",
                                        "Rekonsiliasi checkpoint recovery ke Room gagal (sessionId=${persisted.sessionId}); checkpoint persisten dipertahankan: ${error.message}",
                                        error
                                    )
                                }.getOrDefault(false)

                                val verifiedSampleInDb = sampleEntity == null ||
                                    runCatching { dao.isSamplePersisted(persisted.sessionId, sampleEntity.timestamp) }.getOrDefault(false)
                                val verifiedRowInDb = runCatching { dao.getSessionById(persisted.sessionId) }.getOrNull()
                                val verifiedMetricsInDb = verifiedRowInDb != null &&
                                    verifiedRowInDb.totalMahDelivered + 0.0001 >= persisted.accumulatedMah &&
                                    verifiedRowInDb.maxTempCelsius + 0.001f >= persisted.maxSessionTemp

                                if (!reconciled || !verifiedSampleInDb || !verifiedMetricsInDb) {
                                    when (lookupTargetSessionSafely(dao, persisted.sessionId)) {
                                        is TargetSessionLookupResult.MissingInAccessibleDb -> {
                                            sawTargetMissing = true
                                            val missingReason = "Target sessionId=${persisted.sessionId} hilang saat rekonsiliasi; checkpoint dipertahankan"
                                            preserveRecoveryMetadataFromPersisted(
                                                persisted = persisted,
                                                status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                                                reason = missingReason
                                            )
                                            SessionRecoveryStore.markRequiresFollowUp(
                                                applicationContext,
                                                expectedSessionId = persisted.sessionId,
                                                status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                                                reason = missingReason
                                            )
                                        }
                                        else -> {
                                            sawDbInaccessible = true
                                            val dbFailReason = "Rekonsiliasi Room gagal atau belum terverifikasi untuk sessionId=${persisted.sessionId}; checkpoint dipertahankan"
                                            preserveRecoveryMetadataFromPersisted(
                                                persisted = persisted,
                                                status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                                                reason = dbFailReason
                                            )
                                            SessionRecoveryStore.markRequiresFollowUp(
                                                applicationContext,
                                                expectedSessionId = persisted.sessionId,
                                                status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                                                reason = dbFailReason
                                            )
                                        }
                                    }
                                    continue
                                }

                                // Hapus checkpoint milik sessionId ini SAJA setelah transaksi & verifikasi Room berhasil
                                val cleared = SessionRecoveryStore.clear(
                                    applicationContext,
                                    expectedSessionId = persisted.sessionId
                                )
                                if (cleared) {
                                    if (lastIncompleteSessionRecovery?.sessionId == persisted.sessionId) {
                                        lastIncompleteSessionRecovery = null
                                    }
                                } else {
                                    sawClearPending = true
                                    Log.e(
                                        "PowerGuard",
                                        "Rekonsiliasi Room sukses untuk sessionId=${persisted.sessionId}, namun penghapusan checkpoint gagal; akan dibersihkan idempotent pada siklus berikutnya"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val finalStatus = when {
                sawUnavailable -> PersistedRecoveryReconcileStatus.CHECKPOINT_UNAVAILABLE
                sawCorrupt -> PersistedRecoveryReconcileStatus.CHECKPOINT_CORRUPT
                sawDbInaccessible -> PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE
                sawTargetMissing -> PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP
                sawClearPending -> PersistedRecoveryReconcileStatus.RECONCILED_CHECKPOINT_CLEAR_PENDING
                else -> PersistedRecoveryReconcileStatus.RECONCILED_AND_CLEARED
            }
            lastReconcileStatus = finalStatus
            return@withContext finalStatus
        }

    // Dipanggil eksklusif di dalam sharedSessionMutex.withLock
    private suspend fun recoverOrStartChargingSession(state: LiveBatteryState) = withContext(NonCancellable) {
        val dao = resolveDao()

        // Langkah 1: Rekonsiliasi & simpan data checkpoint ke Room TERLEBIH DAHULU per sessionId.
        val reconcileStatus = reconcilePersistedRecoveryIfAny(dao)

        val openSessionsResult = runCatching { dao.getAllOpenSessions() }
        if (openSessionsResult.isFailure) {
            samplePersistenceHalted = true
            Log.e(
                "PowerGuard",
                "Tidak dapat membaca daftar sesi terbuka saat startup charging; menahan pembuatan sesi baru",
                openSessionsResult.exceptionOrNull()
            )
            return@withContext
        }
        val openSessions = openSessionsResult.getOrDefault(emptyList())
        val allCheckpoints = SessionRecoveryStore.loadAll(applicationContext)
        val loadedCheckpointsById = allCheckpoints
            .filterIsInstance<SessionRecoveryStore.CheckpointLoadResult.Loaded>()
            .map { it.recovery }
            .associateBy { it.sessionId }
        val corruptSessionIds = allCheckpoints
            .filterIsInstance<SessionRecoveryStore.CheckpointLoadResult.Corrupt>()
            .mapNotNull { it.rawSessionId }
            .toSet()
        val hasStorageLevelError = allCheckpoints.any {
            it is SessionRecoveryStore.CheckpointLoadResult.Unavailable ||
                (it is SessionRecoveryStore.CheckpointLoadResult.Corrupt && it.rawSessionId == null)
        }

        if (openSessions.isNotEmpty()) {
            val targetOpen = openSessions.firstOrNull { loadedCheckpointsById.containsKey(it.id) }
                ?: openSessions.last()

            if (openSessions.size > 1 && !hasStorageLevelError) {
                val now = System.currentTimeMillis()
                val excludedIds = buildSet {
                    add(targetOpen.id)
                    addAll(loadedCheckpointsById.keys)
                    addAll(corruptSessionIds)
                }
                finalizeRemainingOpenSessionsIndividually(
                    dao = dao,
                    endTime = now,
                    endLevel = state.level,
                    excludeSessionIds = excludedIds
                )
            }

            val reconciledLatest = runCatching { dao.getSessionById(targetOpen.id) }.getOrNull() ?: targetOpen
            val unappliedRecovery = loadedCheckpointsById[reconciledLatest.id]

            val preservingPreviousInMemory = activeSessionId == reconciledLatest.id
            activeSessionId = reconciledLatest.id
            if (!preservingPreviousInMemory) {
                val elapsedFromDbSec = ((System.currentTimeMillis() - reconciledLatest.startTime) / 1000L)
                    .coerceAtLeast(0L)
                accumulatedMah = maxOf(
                    reconciledLatest.totalMahDelivered,
                    unappliedRecovery?.accumulatedMah ?: 0.0
                )
                sessionSeconds = maxOf(
                    elapsedFromDbSec,
                    unappliedRecovery?.sessionSeconds ?: 0L
                )
                maxSessionTemp = maxOf(
                    reconciledLatest.maxTempCelsius,
                    state.tempCelsius,
                    unappliedRecovery?.maxSessionTemp ?: 0f
                )

                if (unappliedRecovery != null && unappliedRecovery.currentReadingsCount > 0L) {
                    currentSumForAvg = unappliedRecovery.currentSumForAvg
                    currentReadingsCount = unappliedRecovery.currentReadingsCount
                } else if (reconciledLatest.avgCurrentMa != 0 && elapsedFromDbSec > 0L) {
                    currentSumForAvg = reconciledLatest.avgCurrentMa.toLong() * elapsedFromDbSec
                    currentReadingsCount = elapsedFromDbSec
                } else {
                    currentSumForAvg = 0L
                    currentReadingsCount = 0L
                }

                alertedAnomalyCodes.clear()
                reconciledLatest.anomalyReason
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.let { alertedAnomalyCodes.addAll(it) }
                unappliedRecovery?.alertedAnomalyCodes?.let { alertedAnomalyCodes.addAll(it) }

                sampleBuffer.clear()
                val pendingSampleState = unappliedRecovery?.toCompactedLiveBatteryState()
                if (pendingSampleState != null && unappliedRecovery.pendingBatchTimestampMs > 0L) {
                    pendingFlushBatchTimestampMs = unappliedRecovery.pendingBatchTimestampMs
                    sampleBuffer.add(
                        BufferedSampleEntry(
                            seqId = sampleEntrySequence.incrementAndGet(),
                            timestampMs = unappliedRecovery.pendingBatchTimestampMs,
                            endTimestampMs = unappliedRecovery.pendingBatchTimestampMs + unappliedRecovery.sampleSpanDurationMs.coerceAtLeast(0L),
                            state = pendingSampleState,
                            weight = unappliedRecovery.sampleWeight.coerceAtLeast(1),
                            maxTempCelsius = unappliedRecovery.maxSessionTemp
                        )
                    )
                } else {
                    pendingFlushBatchTimestampMs = 0L
                }
            } else {
                accumulatedMah = maxOf(
                    accumulatedMah,
                    reconciledLatest.totalMahDelivered,
                    unappliedRecovery?.accumulatedMah ?: 0.0
                )
                maxSessionTemp = maxOf(
                    maxSessionTemp,
                    reconciledLatest.maxTempCelsius,
                    state.tempCelsius,
                    unappliedRecovery?.maxSessionTemp ?: 0f
                )
                if (sampleBuffer.isEmpty() && unappliedRecovery != null) {
                    val pendingSampleState = unappliedRecovery.toCompactedLiveBatteryState()
                    if (pendingSampleState != null && unappliedRecovery.pendingBatchTimestampMs > 0L) {
                        pendingFlushBatchTimestampMs = unappliedRecovery.pendingBatchTimestampMs
                        sampleBuffer.add(
                            BufferedSampleEntry(
                                seqId = sampleEntrySequence.incrementAndGet(),
                                timestampMs = unappliedRecovery.pendingBatchTimestampMs,
                                endTimestampMs = unappliedRecovery.pendingBatchTimestampMs + unappliedRecovery.sampleSpanDurationMs.coerceAtLeast(0L),
                                state = pendingSampleState,
                                weight = unappliedRecovery.sampleWeight.coerceAtLeast(1),
                                maxTempCelsius = unappliedRecovery.maxSessionTemp
                            )
                        )
                    }
                }
            }

            // Pulihkan samplePersistenceHalted & reset retry counters HANYA jika target sesi & DB sehat
            val targetCheckpointResult = SessionRecoveryStore.loadForSession(applicationContext, reconciledLatest.id)
            val persistenceHealthy = reconcileStatus != PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE &&
                targetCheckpointResult !is SessionRecoveryStore.CheckpointLoadResult.Corrupt &&
                targetCheckpointResult !is SessionRecoveryStore.CheckpointLoadResult.Unavailable &&
                reconciledLatest.endTime == null

            if (persistenceHealthy) {
                samplePersistenceHalted = false
                consecutiveFlushFailures = 0
                nextFlushRetryAtSec = 0L
                checkpointSavePendingRetry = false
            } else {
                samplePersistenceHalted = true
            }
            resolveEngine().resetDeepSleepBaseline()
            anomalyDetector.reset()
            return@withContext
        }

        // Jangan mulai sesi pengisian baru apabila penyimpanan checkpoint dalam kondisi CORRUPT / UNAVAILABLE,
        // atau masih ada checkpoint berstatus PENDING_RECONCILIATION / NEEDS_FOLLOW_UP_DB_UNAVAILABLE yang belum terverifikasi.
        // Checkpoint orphan yang sudah terklasifikasi NEEDS_FOLLOW_UP_TARGET_MISSING tetap terisolasi di key sessionId-nya
        // sehingga tidak menghalangi sesi baru maupun tertimpa oleh sesi baru.
        if (hasStorageLevelError || corruptSessionIds.isNotEmpty()) {
            samplePersistenceHalted = true
            Log.e(
                "PowerGuard",
                "Menolak memulai sesi charging baru karena penyimpanan checkpoint dalam kondisi CORRUPT/UNAVAILABLE"
            )
            return@withContext
        }

        val blockingUnresolved = loadedCheckpointsById.values.firstOrNull { cp ->
            cp.recoveryStatus == SessionRecoveryStore.RecoveryStatus.PENDING_RECONCILIATION ||
                cp.recoveryStatus == SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE
        }
        if (blockingUnresolved != null) {
            preserveRecoveryMetadataFromPersisted(
                persisted = blockingUnresolved,
                status = blockingUnresolved.recoveryStatus,
                reason = "Menolak memulai sesi baru sementara checkpoint sessionId=${blockingUnresolved.sessionId} belum dapat direkonsiliasi (status=${blockingUnresolved.recoveryStatus})"
            )
            samplePersistenceHalted = true
            Log.w(
                "PowerGuard",
                "Menolak memulai sesi charging baru karena checkpoint sessionId=${blockingUnresolved.sessionId} masih berstatus ${blockingUnresolved.recoveryStatus}"
            )
            return@withContext
        }

        startNewChargingSession(state)
    }

    // Dipanggil eksklusif di dalam sharedSessionMutex.withLock
    private suspend fun startNewChargingSession(state: LiveBatteryState) = withContext(NonCancellable) {
        val allCheckpoints = SessionRecoveryStore.loadAll(applicationContext)
        val hasStorageCorruption = allCheckpoints.any {
            it is SessionRecoveryStore.CheckpointLoadResult.Corrupt ||
                it is SessionRecoveryStore.CheckpointLoadResult.Unavailable
        }
        if (hasStorageCorruption) {
            samplePersistenceHalted = true
            Log.w(
                "PowerGuard",
                "startNewChargingSession dibatalkan karena penyimpanan checkpoint dalam kondisi CORRUPT/UNAVAILABLE"
            )
            return@withContext
        }

        val modeLabel = if (state.isFastCharging) "FAST (${state.chargeSource})" else state.chargeSource
        val session = ChargingSessionEntity(
            startTime = System.currentTimeMillis(),
            startLevel = state.level,
            chargerType = modeLabel
        )
        val insertedId = try {
            resolveDao().insertSession(session)
        } catch (ce: CancellationException) {
            throw ce
        } catch (dbError: Throwable) {
            samplePersistenceHalted = true
            Log.e("PowerGuard", "Gagal menyisipkan sesi charging baru ke Room: ${dbError.message}", dbError)
            return@withContext
        }

        samplePersistenceHalted = false
        consecutiveFlushFailures = 0
        nextFlushRetryAtSec = 0L
        pendingFlushBatchTimestampMs = 0L
        checkpointSavePendingRetry = false
        resolveEngine().resetDeepSleepBaseline()
        anomalyDetector.reset()
        alertedAnomalyCodes.clear()
        sampleBuffer.clear()

        accumulatedMah = 0.0
        sessionSeconds = 0L
        maxSessionTemp = state.tempCelsius
        currentSumForAvg = 0L
        currentReadingsCount = 0L
        activeSessionId = insertedId
    }

    private fun appendSampleToBoundedBuffer(state: LiveBatteryState) {
        val nowMs = System.currentTimeMillis()
        val effectiveTs = sampleBuffer.lastOrNull()?.endTimestampMs?.let { maxOf(nowMs, it) } ?: nowMs
        if (sampleBuffer.isEmpty()) {
            pendingFlushBatchTimestampMs = effectiveTs
        }
        sampleBuffer.add(
            BufferedSampleEntry(
                seqId = sampleEntrySequence.incrementAndGet(),
                timestampMs = effectiveTs,
                endTimestampMs = effectiveTs,
                state = state,
                weight = 1,
                maxTempCelsius = state.tempCelsius
            )
        )
        if (sampleBuffer.size > MAX_SAMPLE_BUFFER_SIZE) {
            val compactCount = (MAX_SAMPLE_BUFFER_SIZE / 2).coerceAtLeast(2)
            val oldestSlice = sampleBuffer.subList(0, compactCount).toList()
            val head = oldestSlice.first()
            val tail = oldestSlice.last()
            val metrics = computeWeightedBatchMetrics(oldestSlice)
            val compactedState = tail.state.copy(
                currentMa = metrics.weightedCurrentMa,
                voltageMv = metrics.weightedVoltageMv,
                powerWatt = metrics.weightedPowerWatt,
                tempCelsius = metrics.weightedTempCelsius,
                deepSleepRatio = metrics.weightedDeepSleepRatio
            )
            sampleBuffer.subList(0, compactCount).clear()
            sampleBuffer.add(
                0,
                BufferedSampleEntry(
                    seqId = tail.seqId,
                    timestampMs = head.timestampMs,
                    endTimestampMs = maxOf(head.timestampMs, tail.endTimestampMs),
                    state = compactedState,
                    weight = metrics.totalWeight,
                    maxTempCelsius = metrics.maxTempCelsius,
                    weightedCurrentMaSum = metrics.weightedCurrentMaSum,
                    weightedVoltageMvSum = metrics.weightedVoltageMvSum,
                    weightedTempCelsiusSum = metrics.weightedTempCelsiusSum,
                    weightedPowerWattSum = metrics.weightedPowerWattSum,
                    weightedDeepSleepRatioSum = metrics.weightedDeepSleepRatioSum
                )
            )
        }
    }

    // Dipanggil eksklusif di dalam sharedSessionMutex.withLock
    private suspend fun flushSampleBufferToDatabase(isScreenOn: Boolean) = withContext(NonCancellable) {
        if (samplePersistenceHalted) return@withContext
        val sessionId = activeSessionId ?: return@withContext
        if (sampleBuffer.isEmpty()) return@withContext

        val batchEntries = sampleBuffer.toList()
        val metrics = computeWeightedBatchMetrics(batchEntries)
        val latest = metrics.latestState
        val overallAvgMa = if (currentReadingsCount > 0) (currentSumForAvg / currentReadingsCount).toInt() else metrics.weightedCurrentMa
        val batchTimestamp = if (pendingFlushBatchTimestampMs > 0L) {
            pendingFlushBatchTimestampMs
        } else {
            batchEntries.first().timestampMs.also { pendingFlushBatchTimestampMs = it }
        }
        val effectiveMaxTemp = maxOf(maxSessionTemp, metrics.maxTempCelsius)

        val sampleEntity = BatterySampleEntity(
            sessionId = sessionId,
            timestamp = batchTimestamp,
            batteryLevel = latest.level,
            currentMa = metrics.weightedCurrentMa,
            voltageMv = metrics.weightedVoltageMv,
            tempCelsius = metrics.weightedTempCelsius,
            powerWatt = metrics.weightedPowerWatt,
            isScreenOn = isScreenOn
        )

        val flushStatus = try {
            resolveDao().persistSampleAndUpdateSessionAtomically(
                sample = sampleEntity,
                totalMahDelivered = accumulatedMah,
                avgCurrentMa = overallAvgMa,
                maxTempCelsius = effectiveMaxTemp,
                hasAnomaly = alertedAnomalyCodes.isNotEmpty(),
                anomalyReason = alertedAnomalyCodes.joinToString(", ").ifEmpty { null },
                deepSleepRatio = metrics.weightedDeepSleepRatio
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (dbError: Throwable) {
            consecutiveFlushFailures++
            val backoffSec = (SAMPLE_FLUSH_BASE_RETRY_SEC shl (consecutiveFlushFailures - 1).coerceAtMost(3))
                .coerceAtMost(SAMPLE_FLUSH_MAX_RETRY_SEC)
            nextFlushRetryAtSec = sessionSeconds + backoffSec

            val checkpointSaved = captureIncompleteRecoveryMetadata(
                sessionId = sessionId,
                dbSessionsClosed = false,
                reason = "Penyimpanan batch sampel ke Room gagal: ${dbError.message}"
            )
            Log.e(
                "PowerGuard",
                "Gagal menyimpan batch sampel secara atomik (sessionId=$sessionId, buffered=${sampleBuffer.size}, checkpointSaved=$checkpointSaved, retryIn=${backoffSec}s): ${dbError.message}",
                dbError
            )
            return@withContext
        }

        when (flushStatus) {
            SampleFlushStatus.FLUSHED,
            SampleFlushStatus.ALREADY_FLUSHED_IDEMPOTENT -> {
                val sampleVerifiedInRoom = runCatching {
                    resolveDao().isSamplePersisted(sessionId, batchTimestamp)
                }.getOrDefault(false)

                if (!sampleVerifiedInRoom) {
                    consecutiveFlushFailures++
                    val backoffSec = (SAMPLE_FLUSH_BASE_RETRY_SEC shl (consecutiveFlushFailures - 1).coerceAtMost(3))
                        .coerceAtMost(SAMPLE_FLUSH_MAX_RETRY_SEC)
                    nextFlushRetryAtSec = sessionSeconds + backoffSec
                    captureIncompleteRecoveryMetadata(
                        sessionId = sessionId,
                        dbSessionsClosed = false,
                        reason = "Verifikasi sampel pasca flush gagal di Room (sessionId=$sessionId, ts=$batchTimestamp)"
                    )
                    return@withContext
                }

                // Hapus HANYA entri buffer yang terbukti telah dipersistensikan
                removePersistedBufferEntries(batchEntries)
                consecutiveFlushFailures = 0
                nextFlushRetryAtSec = 0L
                checkpointSavePendingRetry = false
                if (sampleBuffer.isEmpty()) {
                    val cleared = clearCheckpointIfNoLongerNeeded(sessionId)
                    if (cleared && lastIncompleteSessionRecovery?.sessionId == sessionId) {
                        lastIncompleteSessionRecovery = null
                    }
                }
            }
            SampleFlushStatus.VERIFICATION_FAILED -> {
                consecutiveFlushFailures++
                val backoffSec = (SAMPLE_FLUSH_BASE_RETRY_SEC shl (consecutiveFlushFailures - 1).coerceAtMost(3))
                    .coerceAtMost(SAMPLE_FLUSH_MAX_RETRY_SEC)
                nextFlushRetryAtSec = sessionSeconds + backoffSec
                captureIncompleteRecoveryMetadata(
                    sessionId = sessionId,
                    dbSessionsClosed = false,
                    reason = "Transaksi flush sampel gagal verifikasi konsistensi Room (sessionId=$sessionId)"
                )
            }
            SampleFlushStatus.SESSION_CLOSED_OR_MISSING -> {
                samplePersistenceHalted = true
                val checkpointSaved = captureIncompleteRecoveryMetadata(
                    sessionId = sessionId,
                    dbSessionsClosed = true,
                    reason = "Sesi id=$sessionId sudah tertutup atau hilang saat flush sampel"
                )
                if (!checkpointSaved) {
                    Log.e(
                        "PowerGuard",
                        "CRITICAL: Sesi tertutup/hilang saat flush dan SessionRecoveryStore.save() gagal (sessionId=$sessionId); mempertahankan buffer di memori"
                    )
                } else {
                    Log.w(
                        "PowerGuard",
                        "Flush sampel dibatalkan karena sesi id=$sessionId sudah tertutup/hilang; checkpoint recovery disimpan"
                    )
                }
            }
        }
    }

    /**
     * Memfinalisasi sesi aktif dengan aman tanpa pernah menutup sesi lain secara massal.
     * Menegakkan Durability Gate:
     * - Tidak ada penghapusan checkpoint maupun reset state sesi sebelum data direkonsiliasi & diverifikasi.
     * - Membedakan target hilang (TARGET_MISSING_NEEDS_FOLLOW_UP) dari database tidak tersedia (DATABASE_UNAVAILABLE).
     * Dipanggil eksklusif di dalam sharedSessionMutex.withLock.
     */
    private suspend fun ensureSessionFinalizedSafely(): SessionFinalizationResult = withContext(NonCancellable) {
        val dao = resolveDao()
        runCatching { reconcilePersistedRecoveryIfAny(dao) }

        val primaryOutcome = try {
            finalizeActiveSession()
        } catch (ce: CancellationException) {
            throw ce
        } catch (primaryError: Throwable) {
            Log.e(
                "PowerGuard",
                "Finalisasi sesi gagal pada transisi lifecycle (activeSessionId=$activeSessionId): ${primaryError.message}",
                primaryError
            )
            SessionCleanupOutcome.NEEDS_FALLBACK
        }

        when (primaryOutcome) {
            SessionCleanupOutcome.FINALIZED -> {
                samplePersistenceHalted = false
                return@withContext SessionFinalizationResult.COMPLETE_DATA_FINALIZED
            }
            SessionCleanupOutcome.ALREADY_CLOSED -> {
                samplePersistenceHalted = false
                return@withContext SessionFinalizationResult.ALREADY_CLOSED_IDEMPOTENT
            }
            SessionCleanupOutcome.NO_ACTIVE_SESSION -> {
                samplePersistenceHalted = false
                return@withContext SessionFinalizationResult.NO_ACTIVE_SESSION_CLEAN
            }
            SessionCleanupOutcome.TARGET_MISSING -> {
                samplePersistenceHalted = true
                return@withContext SessionFinalizationResult.TARGET_MISSING_NEEDS_FOLLOW_UP
            }
            SessionCleanupOutcome.DATABASE_UNAVAILABLE -> {
                samplePersistenceHalted = true
                return@withContext SessionFinalizationResult.DATABASE_UNAVAILABLE
            }
            SessionCleanupOutcome.NEEDS_FALLBACK -> Unit
        }

        // Jalur utama gagal untuk target sesi: hentikan penulisan sampel baru dan jalankan fallback terkendali
        samplePersistenceHalted = true
        return@withContext when (executeFallbackSessionClosure()) {
            FallbackClosureStatus.FULL_DATA_SAVED_AND_CLEANED -> {
                samplePersistenceHalted = false
                SessionFinalizationResult.COMPLETE_DATA_FINALIZED
            }
            FallbackClosureStatus.CLEANUP_ONLY_DATA_INCOMPLETE -> {
                SessionFinalizationResult.CLEANUP_ONLY_DATA_INCOMPLETE
            }
            FallbackClosureStatus.TARGET_MISSING_FAILED -> {
                SessionFinalizationResult.TARGET_MISSING_NEEDS_FOLLOW_UP
            }
            FallbackClosureStatus.DATABASE_UNAVAILABLE_FAILED -> {
                SessionFinalizationResult.DATABASE_UNAVAILABLE
            }
            FallbackClosureStatus.FAILED -> {
                Log.e(
                    "PowerGuard",
                    "CRITICAL: Finalisasi dan fallback gagal; metadata recovery (activeSessionId=$activeSessionId, buffered=${sampleBuffer.size}, accumulatedMah=$accumulatedMah) dipertahankan aman"
                )
                SessionFinalizationResult.FAILED
            }
        }
    }

    private fun SessionFinalizationResult.isCompleteAndSafeForManualTransition(): Boolean {
        return this == SessionFinalizationResult.COMPLETE_DATA_FINALIZED ||
            this == SessionFinalizationResult.ALREADY_CLOSED_IDEMPOTENT ||
            this == SessionFinalizationResult.NO_ACTIVE_SESSION_CLEAN
    }

    private fun buildPendingFinalSampleEntity(sessionId: Long, fallbackTimestamp: Long): BatterySampleEntity? {
        if (sampleBuffer.isEmpty()) return null
        val batchEntries = sampleBuffer.toList()
        val metrics = computeWeightedBatchMetrics(batchEntries)
        val latest = metrics.latestState
        val sampleTimestamp = if (pendingFlushBatchTimestampMs > 0L) {
            pendingFlushBatchTimestampMs
        } else {
            batchEntries.first().timestampMs.takeIf { it > 0L } ?: fallbackTimestamp
        }
        return BatterySampleEntity(
            sessionId = sessionId,
            timestamp = sampleTimestamp,
            batteryLevel = latest.level,
            currentMa = metrics.weightedCurrentMa,
            voltageMv = metrics.weightedVoltageMv,
            tempCelsius = metrics.weightedTempCelsius,
            powerWatt = metrics.weightedPowerWatt,
            isScreenOn = runCatching { powerManager.isInteractive }.getOrDefault(false)
        )
    }

    private fun buildHealthLogEntityIfEligible(
        sessionId: Long,
        existingSession: ChargingSessionEntity,
        endLevel: Int,
        now: Long
    ): BatteryHealthEntity? {
        if (existingSession.endTime != null) return null
        val deltaPercent = endLevel - existingSession.startLevel
        if (deltaPercent < 40 || accumulatedMah <= 300.0) return null
        val estimatedFullMah = ((accumulatedMah / deltaPercent.toDouble()) * 100.0).toInt()
        val designMah = resolveEngine().designCapacityMah
        val healthPct = ((estimatedFullMah.toFloat() / designMah.toFloat()) * 100f).coerceIn(10f, 110f)
        return BatteryHealthEntity(
            timestamp = now,
            sessionId = sessionId,
            estimatedCapacityMah = estimatedFullMah,
            designCapacityMah = designMah,
            healthPercentage = healthPct
        )
    }

    /**
     * Memfinalisasi setiap sesi terbuka secara individual dan terisolasi dengan metrik
     * masing-masing sesi. Kegagalan satu sesi tidak pernah mengorbankan sesi lainnya.
     */
    private suspend fun finalizeRemainingOpenSessionsIndividually(
        dao: BatteryDao,
        endTime: Long,
        endLevel: Int,
        excludeSessionIds: Set<Long> = emptySet()
    ): Boolean {
        val openSessions = dao.getAllOpenSessions().filter { it.id !in excludeSessionIds }
        val designMah = resolveEngine().designCapacityMah
        var allSucceeded = true
        for (openSession in openSessions) {
            val status = runCatching {
                dao.finalizeSingleSessionWithReconciledMetrics(
                    sessionId = openSession.id,
                    endTime = endTime,
                    endLevel = endLevel,
                    designCapacityMah = designMah
                )
            }.onFailure { error ->
                Log.e(
                    "PowerGuard",
                    "Gagal memfinalisasi sesi terbuka id=${openSession.id}; sesi lain tetap diproses: ${error.message}",
                    error
                )
            }.getOrNull()

            if (status != SessionFinalizationStatus.FINALIZED &&
                status != SessionFinalizationStatus.ALREADY_CLOSED
            ) {
                allSucceeded = false
            }
        }
        return allSucceeded
    }

    /**
     * Merekonsiliasi sisa buffer/metrik in-memory untuk sesi yang sudah tertutup di Room
     * (endTime != null) agar tidak ada sampel tertunda atau anomali baru yang terlewat
     * hanya karena baris sesi sudah ditutup lebih awal.
     */
    private suspend fun reconcileInMemoryPendingDataIntoClosedSession(
        dao: BatteryDao,
        sessionId: Long,
        existingSession: ChargingSessionEntity,
        now: Long
    ): Boolean {
        val pendingEntriesSnapshot = sampleBuffer.toList()
        val pendingSample = buildPendingFinalSampleEntity(sessionId, now)
        val existingAnomalyCodes = existingSession.anomalyReason
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()
        val hasMissingAnomalyCodes = alertedAnomalyCodes.any { it !in existingAnomalyCodes }
        val effectiveMaxTemp = maxOf(
            maxSessionTemp,
            pendingEntriesSnapshot.maxOfOrNull { maxOf(it.maxTempCelsius, it.state.tempCelsius) } ?: 0f
        )

        val hasNewerMetrics = accumulatedMah > existingSession.totalMahDelivered + 0.0001 ||
            effectiveMaxTemp > existingSession.maxTempCelsius + 0.001f ||
            (alertedAnomalyCodes.isNotEmpty() && !existingSession.isAnomaly) ||
            hasMissingAnomalyCodes

        if (pendingSample == null && !hasNewerMetrics) {
            return true
        }

        val overallAvgMa = if (currentReadingsCount > 0L) {
            (currentSumForAvg / currentReadingsCount).toInt()
        } else {
            existingSession.avgCurrentMa
        }
        val anomalyReason = alertedAnomalyCodes.joinToString(", ").ifEmpty { null }

        val reconciled = runCatching {
            dao.reconcileRecoveredSessionAtomically(
                sessionId = sessionId,
                recoveredTotalMah = accumulatedMah,
                recoveredAvgCurrentMa = overallAvgMa,
                recoveredMaxTempCelsius = effectiveMaxTemp,
                recoveredHasAnomaly = alertedAnomalyCodes.isNotEmpty(),
                recoveredAnomalyReason = anomalyReason,
                recoveredSample = pendingSample
            )
        }.getOrDefault(false)

        if (!reconciled) {
            return false
        }

        val sampleVerified = pendingSample == null ||
            runCatching { dao.isSamplePersisted(sessionId, pendingSample.timestamp) }.getOrDefault(false)
        val verifiedRow = runCatching { dao.getSessionById(sessionId) }.getOrNull()
        val verifiedAnomalyCodes = verifiedRow?.anomalyReason
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()
        val anomaliesVerified = alertedAnomalyCodes.all { it in verifiedAnomalyCodes }
        val metricsVerified = verifiedRow != null &&
            verifiedRow.endTime != null &&
            verifiedRow.totalMahDelivered + 0.0001 >= accumulatedMah &&
            verifiedRow.maxTempCelsius + 0.001f >= effectiveMaxTemp &&
            anomaliesVerified

        if (!sampleVerified || !metricsVerified) {
            return false
        }

        removePersistedBufferEntries(pendingEntriesSnapshot)
        return true
    }

    /**
     * Memfinalisasi HANYA sesi aktif [activeSessionId] secara atomik dan idempotent.
     * Menegakkan Durability Gate:
     * - Sesi yang sudah tertutup di Room (endTime != null) tetap direkonsiliasi apabila
     *   masih memiliki checkpoint persisten atau sampel tertunda di memori.
     * - Kegagalan rekonsiliasi mencegah penghapusan checkpoint maupun reset state memori.
     * - Target session yang hilang tidak pernah dianggap sebagai persistensi data yang sukses.
     * Dipanggil eksklusif di dalam sharedSessionMutex.withLock.
     */
    private suspend fun finalizeActiveSession(): SessionCleanupOutcome = withContext(NonCancellable) {
        val dao = resolveDao()
        val now = System.currentTimeMillis()
        val endLevel = readSafeBatteryLevelForFinalization()
        val sessionId = activeSessionId

        if (sessionId == null) {
            val reconcileStatus = reconcilePersistedRecoveryIfAny(dao)
            return@withContext when (reconcileStatus) {
                PersistedRecoveryReconcileStatus.NO_CHECKPOINT,
                PersistedRecoveryReconcileStatus.RECONCILED_AND_CLEARED -> {
                    if (!hasRecoverableRamOnlyData() &&
                        SessionRecoveryStore.loadAll(applicationContext).isEmpty()
                    ) {
                        resetInMemorySessionTracking(clearRecoverySnapshot = true)
                    }
                    SessionCleanupOutcome.NO_ACTIVE_SESSION
                }
                PersistedRecoveryReconcileStatus.RECONCILED_CHECKPOINT_CLEAR_PENDING ->
                    SessionCleanupOutcome.NEEDS_FALLBACK
                PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP ->
                    SessionCleanupOutcome.TARGET_MISSING
                PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE ->
                    SessionCleanupOutcome.DATABASE_UNAVAILABLE
                PersistedRecoveryReconcileStatus.CHECKPOINT_CORRUPT,
                PersistedRecoveryReconcileStatus.CHECKPOINT_UNAVAILABLE ->
                    SessionCleanupOutcome.NEEDS_FALLBACK
            }
        }

        val lookup = lookupTargetSessionSafely(dao, sessionId)
        val existingSession = when (lookup) {
            is TargetSessionLookupResult.DatabaseInaccessible -> {
                val dbReason = "Database Room tidak dapat diakses saat finalisasi sessionId=$sessionId: ${lookup.cause?.message ?: "unknown"}"
                if (hasRecoverableRamOnlyData()) {
                    captureIncompleteRecoveryMetadata(
                        sessionId = sessionId,
                        dbSessionsClosed = false,
                        reason = dbReason,
                        recoveryStatus = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE
                    )
                } else {
                    SessionRecoveryStore.markRequiresFollowUp(
                        applicationContext,
                        expectedSessionId = sessionId,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                        reason = dbReason
                    )
                }
                lastReconcileStatus = PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE
                return@withContext SessionCleanupOutcome.DATABASE_UNAVAILABLE
            }

            is TargetSessionLookupResult.MissingInAccessibleDb -> {
                Log.w(
                    "PowerGuard",
                    "Target session id=$sessionId tidak ditemukan saat finalisasi; mempertahankan checkpoint dan data sesi tanpa menutup sesi lain di database"
                )
                val followUpReason = "Target session id=$sessionId tidak ditemukan di Room saat finalisasi utama; checkpoint dan data sesi dipertahankan"
                val existingPersisted = SessionRecoveryStore.loadForSessionOrNull(applicationContext, sessionId)
                if (hasRecoverableRamOnlyData()) {
                    captureIncompleteRecoveryMetadata(
                        sessionId = sessionId,
                        dbSessionsClosed = false,
                        reason = followUpReason,
                        recoveryStatus = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING
                    )
                } else if (existingPersisted != null) {
                    preserveRecoveryMetadataFromPersisted(
                        persisted = existingPersisted,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                        reason = followUpReason
                    )
                    SessionRecoveryStore.markRequiresFollowUp(
                        applicationContext,
                        expectedSessionId = sessionId,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                        reason = followUpReason
                    )
                }
                lastReconcileStatus = PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP
                return@withContext SessionCleanupOutcome.TARGET_MISSING
            }

            is TargetSessionLookupResult.Found -> lookup.session
        }

        if (finalizedSessionIds.contains(sessionId) || existingSession.endTime != null) {
            val reconcileStatus = reconcilePersistedRecoveryIfAny(dao)
            val targetCheckpointAfter = SessionRecoveryStore.loadForSession(applicationContext, sessionId)
            if (reconcileStatus == PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE ||
                targetCheckpointAfter is SessionRecoveryStore.CheckpointLoadResult.Corrupt ||
                targetCheckpointAfter is SessionRecoveryStore.CheckpointLoadResult.Unavailable
            ) {
                return@withContext SessionCleanupOutcome.NEEDS_FALLBACK
            }

            val closedReconciled = reconcileInMemoryPendingDataIntoClosedSession(
                dao = dao,
                sessionId = sessionId,
                existingSession = existingSession,
                now = now
            )
            if (!closedReconciled) {
                captureIncompleteRecoveryMetadata(
                    sessionId = sessionId,
                    dbSessionsClosed = true,
                    reason = "Gagal merekonsiliasi sampel tertunda ke sesi tertutup id=$sessionId"
                )
                return@withContext SessionCleanupOutcome.NEEDS_FALLBACK
            }

            val verifiedClosed = runCatching { dao.getSessionById(sessionId)?.endTime != null }.getOrDefault(false)
            if (!verifiedClosed) {
                return@withContext SessionCleanupOutcome.NEEDS_FALLBACK
            }

            finalizedSessionIds.add(sessionId)
            val cleared = clearCheckpointIfNoLongerNeeded(sessionId)
            if (!cleared) {
                return@withContext SessionCleanupOutcome.NEEDS_FALLBACK
            }
            resetInMemorySessionTracking(clearRecoverySnapshot = true)
            return@withContext SessionCleanupOutcome.ALREADY_CLOSED
        }

        val pendingEntriesSnapshot = sampleBuffer.toList()
        val finalSample = buildPendingFinalSampleEntity(sessionId, now)
        val overallAvgMa = if (currentReadingsCount > 0) (currentSumForAvg / currentReadingsCount).toInt() else 0
        val deepSleepRatio = try {
            resolveEngine().calculateDeepSleepRatio()
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            existingSession.deepSleepRatio
        }
        val healthLog = buildHealthLogEntityIfEligible(sessionId, existingSession, endLevel, now)
        val effectiveMaxTemp = maxOf(
            maxSessionTemp,
            pendingEntriesSnapshot.maxOfOrNull { maxOf(it.maxTempCelsius, it.state.tempCelsius) } ?: 0f
        )

        val status = dao.finalizeSessionAtomically(
            sessionId = sessionId,
            endTime = now,
            endLevel = endLevel,
            totalMahDelivered = accumulatedMah,
            avgCurrentMa = overallAvgMa,
            maxTempCelsius = effectiveMaxTemp,
            hasAnomaly = alertedAnomalyCodes.isNotEmpty(),
            anomalyReason = alertedAnomalyCodes.joinToString(", ").ifEmpty { null },
            deepSleepRatio = deepSleepRatio,
            finalSample = finalSample,
            healthLog = healthLog
        )

        return@withContext when (status) {
            SessionFinalizationStatus.FINALIZED -> {
                val verifiedRow = runCatching { dao.getSessionById(sessionId) }.getOrNull()
                val verifiedSample = finalSample == null ||
                    runCatching { dao.isSamplePersisted(sessionId, finalSample.timestamp) }.getOrDefault(false)
                if (verifiedRow?.endTime == null || !verifiedSample) {
                    return@withContext SessionCleanupOutcome.NEEDS_FALLBACK
                }

                removePersistedBufferEntries(pendingEntriesSnapshot)
                finalizedSessionIds.add(sessionId)
                val cleared = clearCheckpointIfNoLongerNeeded(sessionId)
                if (!cleared) {
                    Log.e(
                        "PowerGuard",
                        "Finalisasi sesi id=$sessionId sukses, namun penghapusan checkpoint gagal; mempertahankan snapshot sampai clear terverifikasi"
                    )
                }
                resetInMemorySessionTracking(clearRecoverySnapshot = cleared)
                SessionCleanupOutcome.FINALIZED
            }
            SessionFinalizationStatus.ALREADY_CLOSED -> {
                val reconcileStatus = reconcilePersistedRecoveryIfAny(dao)
                val targetCheckpointAfter = SessionRecoveryStore.loadForSession(applicationContext, sessionId)
                if (reconcileStatus == PersistedRecoveryReconcileStatus.DATABASE_INACCESSIBLE ||
                    targetCheckpointAfter is SessionRecoveryStore.CheckpointLoadResult.Corrupt ||
                    targetCheckpointAfter is SessionRecoveryStore.CheckpointLoadResult.Unavailable
                ) {
                    return@withContext SessionCleanupOutcome.NEEDS_FALLBACK
                }
                val closedReconciled = reconcileInMemoryPendingDataIntoClosedSession(
                    dao = dao,
                    sessionId = sessionId,
                    existingSession = existingSession,
                    now = now
                )
                if (!closedReconciled) {
                    return@withContext SessionCleanupOutcome.NEEDS_FALLBACK
                }
                finalizedSessionIds.add(sessionId)
                val cleared = clearCheckpointIfNoLongerNeeded(sessionId)
                if (!cleared) {
                    return@withContext SessionCleanupOutcome.NEEDS_FALLBACK
                }
                resetInMemorySessionTracking(clearRecoverySnapshot = true)
                SessionCleanupOutcome.ALREADY_CLOSED
            }
            SessionFinalizationStatus.TARGET_MISSING -> {
                Log.w(
                    "PowerGuard",
                    "Target session id=$sessionId tidak ditemukan saat transaksi finalisasi; mempertahankan checkpoint dan data in-memory tanpa menutup sesi lain"
                )
                val followUpReason = "Target session id=$sessionId tidak ditemukan saat transaksi finalisasi; checkpoint dan data sesi dipertahankan"
                if (hasRecoverableRamOnlyData()) {
                    captureIncompleteRecoveryMetadata(
                        sessionId = sessionId,
                        dbSessionsClosed = false,
                        reason = followUpReason,
                        recoveryStatus = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING
                    )
                } else {
                    SessionRecoveryStore.markRequiresFollowUp(
                        applicationContext,
                        expectedSessionId = sessionId,
                        status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                        reason = followUpReason
                    )
                }
                lastReconcileStatus = PersistedRecoveryReconcileStatus.TARGET_SESSION_MISSING_NEEDS_FOLLOW_UP
                SessionCleanupOutcome.TARGET_MISSING
            }
            SessionFinalizationStatus.OPEN_SESSIONS_REMAIN -> {
                SessionCleanupOutcome.NEEDS_FALLBACK
            }
        }
    }

    /**
     * Implementasi internal retry pemulihan & flush di dalam sharedSessionMutex.withLock.
     * Menjamin:
     * - Sampel hanya dihapus dari memori setelah terbukti tersimpan di Room.
     * - Counter retry hanya direset setelah penyelesaian yang terverifikasi.
     * - samplePersistenceHalted hanya dipulihkan ketika persistensi Room benar-benar sehat.
     * - Monitoring loop dipulihkan dengan aman sehingga layanan tidak pernah tertinggal macet.
     */
    private suspend fun retryPendingCheckpointAndFinalizationLocked(isScreenOn: Boolean): Boolean =
        withContext(NonCancellable) {
            val dao = resolveDao()
            val currentSessionId = activeSessionId

            if (currentSessionId != null && checkpointSavePendingRetry) {
                val saved = captureIncompleteRecoveryMetadata(
                    sessionId = currentSessionId,
                    dbSessionsClosed = false,
                    reason = "Retry penyimpanan checkpoint recovery"
                )
                if (!saved) {
                    return@withContext false
                }
            }

            if (currentSessionId != null) {
                when (val lookup = lookupTargetSessionSafely(dao, currentSessionId)) {
                    is TargetSessionLookupResult.DatabaseInaccessible -> {
                        samplePersistenceHalted = true
                        return@withContext false
                    }
                    is TargetSessionLookupResult.MissingInAccessibleDb -> {
                        samplePersistenceHalted = true
                        val reason = "Retry gagal: target sessionId=$currentSessionId tidak ditemukan di Room"
                        captureIncompleteRecoveryMetadata(
                            sessionId = currentSessionId,
                            dbSessionsClosed = false,
                            reason = reason,
                            recoveryStatus = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING
                        )
                        return@withContext false
                    }
                    is TargetSessionLookupResult.Found -> {
                        val sessionRow = lookup.session
                        if (sessionRow.endTime == null) {
                            if (sampleBuffer.isNotEmpty()) {
                                val wasHalted = samplePersistenceHalted
                                samplePersistenceHalted = false
                                flushSampleBufferToDatabase(isScreenOn)
                                if (sampleBuffer.isNotEmpty()) {
                                    if (wasHalted) {
                                        samplePersistenceHalted = true
                                    }
                                    return@withContext false
                                }
                            }
                            samplePersistenceHalted = false
                            consecutiveFlushFailures = 0
                            nextFlushRetryAtSec = 0L
                            checkpointSavePendingRetry = false
                        } else {
                            val reconciledClosed = reconcileInMemoryPendingDataIntoClosedSession(
                                dao = dao,
                                sessionId = currentSessionId,
                                existingSession = sessionRow,
                                now = System.currentTimeMillis()
                            )
                            if (!reconciledClosed || sampleBuffer.isNotEmpty()) {
                                samplePersistenceHalted = true
                                return@withContext false
                            }
                            consecutiveFlushFailures = 0
                            nextFlushRetryAtSec = 0L
                            checkpointSavePendingRetry = false
                            samplePersistenceHalted = false
                        }
                    }
                }
            }

            val allCheckpoints = SessionRecoveryStore.loadAll(applicationContext)
            if (allCheckpoints.isNotEmpty()) {
                val reconcileStatus = reconcilePersistedRecoveryIfAny(dao)
                if (!reconcileStatus.isReconciledOrNoCheckpoint) {
                    return@withContext false
                }
            }

            // Pulihkan pemantauan secara aman bila service masih aktif / dalam RECOVERY_HOLD saat charger tersambung
            if (!destroyRequested.get() && actorState != ActorLifecycleState.STOPPED) {
                val sensor = runCatching { resolveEngine().readCurrentState() }.getOrNull()
                if (actorState == ActorLifecycleState.RECOVERY_HOLD && sensor?.isCharging == true) {
                    transitionActorState(ActorLifecycleState.RUNNING_CHARGING)
                }
                if (actorState == ActorLifecycleState.RUNNING_CHARGING ||
                    actorState == ActorLifecycleState.RUNNING_MANUAL
                ) {
                    ensureMonitoringLoopRunning()
                }
            }

            true
        }

    /**
     * Mekanisme retry aman untuk menyimpan checkpoint yang sempat gagal disimpan,
     * mem-flush sampleBuffer yang tertunda ke Room, merekonsiliasi checkpoint persisten,
     * dan memulihkan pemantauan secara aman.
     */
    @VisibleForTesting
    internal suspend fun retryPendingCheckpointAndFinalization(): Boolean = withContext(NonCancellable) {
        sharedSessionMutex.withLock {
            val isScreenOn = runCatching { powerManager.isInteractive }.getOrDefault(false)
            retryPendingCheckpointAndFinalizationLocked(isScreenOn)
        }
    }

    @VisibleForTesting
    internal suspend fun retryPendingCheckpointAndFinalizationForTest(): Boolean {
        return retryPendingCheckpointAndFinalization()
    }

    @VisibleForTesting
    internal fun seedActiveSessionStateForTesting(
        sessionId: Long?,
        accumulatedMah: Double,
        sessionSeconds: Long,
        maxSessionTemp: Float,
        currentSumForAvg: Long,
        currentReadingsCount: Long,
        alertedAnomalyCodes: Set<String> = emptySet(),
        bufferedSamples: List<LiveBatteryState> = emptyList(),
        pendingBatchTimestampMs: Long = 0L,
        samplePersistenceHalted: Boolean = false
    ) {
        this.activeSessionId = sessionId
        this.accumulatedMah = accumulatedMah
        this.sessionSeconds = sessionSeconds
        this.maxSessionTemp = maxSessionTemp
        this.currentSumForAvg = currentSumForAvg
        this.currentReadingsCount = currentReadingsCount
        this.alertedAnomalyCodes.clear()
        this.alertedAnomalyCodes.addAll(alertedAnomalyCodes)
        this.sampleBuffer.clear()
        val baseTs = if (pendingBatchTimestampMs > 0L) pendingBatchTimestampMs else System.currentTimeMillis()
        bufferedSamples.forEachIndexed { idx, state ->
            val ts = baseTs + idx
            this.sampleBuffer.add(
                BufferedSampleEntry(
                    seqId = sampleEntrySequence.incrementAndGet(),
                    timestampMs = ts,
                    endTimestampMs = ts,
                    state = state,
                    weight = 1,
                    maxTempCelsius = state.tempCelsius
                )
            )
        }
        this.pendingFlushBatchTimestampMs = pendingBatchTimestampMs
        this.samplePersistenceHalted = samplePersistenceHalted
        this.consecutiveFlushFailures = 0
        this.nextFlushRetryAtSec = 0L
        this.checkpointSavePendingRetry = false
    }

    @VisibleForTesting
    internal fun hasUnpersistedRamOnlyDataForTesting(
        result: SessionFinalizationResult = SessionFinalizationResult.FAILED
    ): Boolean = hasUnpersistedRamOnlyData(result)

    @VisibleForTesting
    internal fun appendSampleToBufferForTesting(state: LiveBatteryState) {
        appendSampleToBoundedBuffer(state)
    }

    @VisibleForTesting
    internal fun getInMemorySessionSnapshotForTesting(): InMemorySessionSnapshot {
        val incomplete = lastIncompleteSessionRecovery
        val spanMs = if (sampleBuffer.isNotEmpty()) {
            val startTs = sampleBuffer.first().timestampMs
            val endTs = sampleBuffer.maxOf { maxOf(it.timestampMs, it.endTimestampMs) }
            (endTs - startTs).coerceAtLeast(0L)
        } else {
            0L
        }
        return InMemorySessionSnapshot(
            activeSessionId = activeSessionId,
            bufferedSamplesCount = sampleBuffer.size,
            bufferedTotalSampleWeight = sampleBuffer.sumOf { it.weight.coerceAtLeast(1) },
            bufferedSampleSpanDurationMs = spanMs,
            pendingBatchTimestampMs = pendingFlushBatchTimestampMs,
            accumulatedMah = accumulatedMah,
            sessionSeconds = sessionSeconds,
            maxSessionTemp = maxSessionTemp,
            currentSumForAvg = currentSumForAvg,
            currentReadingsCount = currentReadingsCount,
            alertedAnomalyCodes = alertedAnomalyCodes.toSet(),
            consecutiveFlushFailures = consecutiveFlushFailures,
            nextFlushRetryAtSec = nextFlushRetryAtSec,
            checkpointSavePendingRetry = checkpointSavePendingRetry,
            samplePersistenceHalted = samplePersistenceHalted,
            hasIncompleteRecoverySnapshot = incomplete != null,
            lastRecoveryReconcileStatus = lastReconcileStatus,
            recoveryRequiresFollowUp = incomplete?.requiresFollowUp == true,
            incompleteRecoverySessionId = incomplete?.sessionId,
            incompleteRecoveryAccumulatedMah = incomplete?.accumulatedMah ?: 0.0,
            incompleteRecoveryBufferedCount = incomplete?.bufferedSamplesCount ?: 0,
            isInRecoveryHold = actorState == ActorLifecycleState.RECOVERY_HOLD,
            isMonitoringLoopActive = monitorJob?.isActive == true
        )
    }

    @VisibleForTesting
    internal suspend fun reconcilePersistedRecoveryForTesting(): PersistedRecoveryReconcileStatus {
        return sharedSessionMutex.withLock {
            reconcilePersistedRecoveryIfAny(resolveDao())
        }
    }

    @VisibleForTesting
    internal suspend fun executeFallbackSessionClosureForTesting(): FallbackClosureStatus {
        return sharedSessionMutex.withLock {
            executeFallbackSessionClosure()
        }
    }

    @VisibleForTesting
    internal suspend fun recoverOrStartChargingSessionForTesting(state: LiveBatteryState) {
        sharedSessionMutex.withLock {
            recoverOrStartChargingSession(state)
        }
    }

    @VisibleForTesting
    internal suspend fun flushSampleBufferForTesting(isScreenOn: Boolean) {
        sharedSessionMutex.withLock {
            flushSampleBufferToDatabase(isScreenOn)
        }
    }

    @VisibleForTesting
    internal suspend fun ensureSessionFinalizedSafelyForTesting(): SessionFinalizationResult {
        return sharedSessionMutex.withLock {
            ensureSessionFinalizedSafely()
        }
    }

    @VisibleForTesting
    internal suspend fun handleStopCommandForTesting(stopStartId: Int = 1, stopGeneration: Long = latestStartGeneration.get()) {
        handleStop(stopStartId = stopStartId, stopGeneration = stopGeneration)
    }

    @VisibleForTesting
    internal fun setActorRunningForTesting(charging: Boolean = true, generation: Long = 1L) {
        activeRunGeneration = generation
        latestStartGeneration.updateAndGet { maxOf(it, generation) }
        transitionActorState(if (charging) ActorLifecycleState.RUNNING_CHARGING else ActorLifecycleState.RUNNING_MANUAL)
    }

    private fun createNotificationChannels() {
        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR,
            "Live Power Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Menampilkan arus pengisian dan suhu baterai secara real-time"
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ANOMALY,
            "Peringatan Anomali Pengisian",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi darurat jika fast charging terhambat atau baterai tidak terisi"
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(monitorChannel)
        notificationManager.createNotificationChannel(alertChannel)
    }

    private fun buildMonitorNotification(contentText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setContentTitle("PowerGuard Aktif")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun sendAnomalyAlertNotification(alertMessage: String) {
        val openAppIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ANOMALY)
            .setContentTitle("⚠️ Peringatan Daya & Pengisian")
            .setContentText(alertMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertMessage))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ANOMALY_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        if (destroyRequested.compareAndSet(false, true)) {
            lifecycleChannel.trySend(LifecycleCommand.Destroy)
            lifecycleChannel.close()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_MONITORING = "com.superdenot.powerguard.START_MONITORING"
        const val ACTION_START_MANUAL_MONITORING = "com.superdenot.powerguard.START_MANUAL_MONITORING"
        const val ACTION_STOP_CHARGING_SESSION = "com.superdenot.powerguard.STOP_CHARGING_SESSION"

        private const val EXTRA_FROM_REQUEST_START = "com.superdenot.powerguard.EXTRA_FROM_REQUEST_START"
        private const val EXTRA_START_GENERATION = "com.superdenot.powerguard.EXTRA_START_GENERATION"

        private const val CHANNEL_MONITOR = "powerguard_live_monitor"
        private const val CHANNEL_ANOMALY = "powerguard_anomaly_alerts"
        private const val MONITOR_NOTIFICATION_ID = 1001
        private const val ANOMALY_NOTIFICATION_ID = 1002

        private const val SAMPLE_FLUSH_BATCH_SIZE = 60
        private const val MAX_SAMPLE_BUFFER_SIZE = 300
        private const val SAMPLE_FLUSH_BASE_RETRY_SEC = 5L
        private const val SAMPLE_FLUSH_MAX_RETRY_SEC = 30L

        // Mutex bersama antara service actor, receiver cleanup, dan startup recovery
        val sharedSessionMutex = Mutex()

        private val _liveState = MutableStateFlow(LiveBatteryState())
        val liveState: StateFlow<LiveBatteryState> = _liveState.asStateFlow()

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        private val _isStartingFlow = MutableStateFlow(false)
        val isStartingFlow: StateFlow<Boolean> = _isStartingFlow.asStateFlow()

        val isRunning: Boolean
            get() = _isRunningFlow.value

        val isStartingOrRunning: Boolean
            get() = synchronized(lifecycleLock) {
                pendingStartGenerations.isNotEmpty() || _isStartingFlow.value || _isRunningFlow.value
            }

        internal fun hasActiveServiceSessionFor(sessionId: Long? = null): Boolean = synchronized(lifecycleLock) {
            val instance = activeInstance ?: return false
            if (sessionId == null) {
                instance.activeSessionId != null ||
                    instance.destroyFinalizationInProgress ||
                    instance.actorState != ActorLifecycleState.STOPPED
            } else {
                instance.activeSessionId == sessionId ||
                    instance.lastIncompleteSessionRecovery?.sessionId == sessionId
            }
        }

        private val lifecycleLock = Any()
        private val pendingStartGenerations = mutableSetOf<Long>()
        private val pendingStartCount = AtomicInteger(0)
        private val commandSequence = AtomicLong(0L)
        private val latestStartGeneration = AtomicLong(0L)

        @Volatile
        private var activeInstance: BatteryMonitorService? = null

        private fun hasPendingStarts(): Boolean = synchronized(lifecycleLock) {
            pendingStartGenerations.isNotEmpty()
        }

        private fun registerPendingStart(): Long = synchronized(lifecycleLock) {
            val gen = commandSequence.incrementAndGet()
            latestStartGeneration.updateAndGet { prev -> maxOf(prev, gen) }
            pendingStartGenerations.add(gen)
            pendingStartCount.set(pendingStartGenerations.size)
            gen
        }

        private fun consumePendingStartIfLatest(generation: Long): Boolean = synchronized(lifecycleLock) {
            val wasPresent = pendingStartGenerations.remove(generation)
            val isLatest = generation >= latestStartGeneration.get()
            if (wasPresent && isLatest) {
                pendingStartGenerations.removeAll { it < generation }
            }
            pendingStartCount.set(pendingStartGenerations.size)
            wasPresent && isLatest
        }

        private fun removePendingStart(generation: Long) {
            synchronized(lifecycleLock) {
                pendingStartGenerations.remove(generation)
                pendingStartCount.set(pendingStartGenerations.size)
            }
        }

        internal suspend fun lookupTargetSessionSafely(
            dao: BatteryDao,
            sessionId: Long
        ): TargetSessionLookupResult {
            val sessionResult = runCatching { dao.getSessionById(sessionId) }
            if (sessionResult.isFailure) {
                return TargetSessionLookupResult.DatabaseInaccessible(sessionResult.exceptionOrNull())
            }
            val session = sessionResult.getOrNull()
            if (session != null) {
                return TargetSessionLookupResult.Found(session)
            }
            val probeResult = runCatching { dao.getAllOpenSessions() }
            if (probeResult.isFailure) {
                return TargetSessionLookupResult.DatabaseInaccessible(probeResult.exceptionOrNull())
            }
            return TargetSessionLookupResult.MissingInAccessibleDb
        }

        /**
         * Pemulihan saat aplikasi dibuka kembali ketika perangkat tidak sedang mengisi daya
         * namun masih terdapat sesi yang belum selesai atau checkpoint recovery persisten multi-sesi.
         * Merekonsiliasi seluruh checkpoint ke Room terlebih dahulu per sessionId, memverifikasi hasil DB,
         * memfinalisasi tiap sesi terbuka secara individual, dan HANYA menghapus checkpoint
         * setelah transaksi Room & verifikasi keadaan database berhasil.
         */
        suspend fun recoverOrReconcileOnAppLaunch(
            context: Context,
            daoOverride: BatteryDao? = null,
            engineOverride: BatteryMonitorEngine? = null
        ) {
            val appContext = context.applicationContext
            if (isStartingOrRunning || hasActiveServiceSessionFor()) return

            sharedSessionMutex.withLock {
                if (isStartingOrRunning || hasActiveServiceSessionFor()) return@withLock

                val resolvedEngine = engineOverride ?: BatteryMonitorEngine(appContext)
                val snapshot = runCatching { resolvedEngine.readCurrentState() }.getOrNull() ?: return@withLock
                if (snapshot.isCharging) return@withLock

                val dao = daoOverride ?: AppDatabase.getInstance(appContext).batteryDao()
                val now = System.currentTimeMillis()
                val designCapacityMah = runCatching { resolvedEngine.designCapacityMah }.getOrDefault(5000)

                withContext(NonCancellable) {
                    val blockedSessionIds = mutableSetOf<Long>()
                    var hasGlobalCheckpointCorruptionOrError = false

                    val allResults = SessionRecoveryStore.loadAll(appContext)
                    for (loadResult in allResults) {
                        when (loadResult) {
                            is SessionRecoveryStore.CheckpointLoadResult.Empty -> Unit

                            is SessionRecoveryStore.CheckpointLoadResult.Unavailable -> {
                                hasGlobalCheckpointCorruptionOrError = true
                                Log.e(
                                    "PowerGuard",
                                    "Startup recovery: penyimpanan checkpoint tidak dapat dibaca (${loadResult.reason}); mempertahankan seluruh state",
                                    loadResult.cause
                                )
                            }

                            is SessionRecoveryStore.CheckpointLoadResult.Corrupt -> {
                                val rawId = loadResult.rawSessionId
                                if (rawId != null && rawId > 0L) {
                                    blockedSessionIds.add(rawId)
                                } else {
                                    hasGlobalCheckpointCorruptionOrError = true
                                }
                                Log.e(
                                    "PowerGuard",
                                    "Startup recovery: checkpoint korup terdeteksi (rawSessionId=${loadResult.rawSessionId}): ${loadResult.reason}"
                                )
                            }

                            is SessionRecoveryStore.CheckpointLoadResult.Loaded -> {
                                val persisted = loadResult.recovery
                                blockedSessionIds.add(persisted.sessionId)
                                when (val lookup = lookupTargetSessionSafely(dao, persisted.sessionId)) {
                                    is TargetSessionLookupResult.DatabaseInaccessible -> {
                                        Log.e(
                                            "PowerGuard",
                                            "Startup recovery: database tidak dapat diakses untuk sessionId=${persisted.sessionId}; checkpoint dipertahankan",
                                            lookup.cause
                                        )
                                        SessionRecoveryStore.markRequiresFollowUp(
                                            appContext,
                                            expectedSessionId = persisted.sessionId,
                                            status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                                            reason = "Startup recovery: database tidak dapat diakses untuk sessionId=${persisted.sessionId}"
                                        )
                                    }

                                    is TargetSessionLookupResult.MissingInAccessibleDb -> {
                                        Log.w(
                                            "PowerGuard",
                                            "Startup recovery: target sessionId=${persisted.sessionId} tidak ditemukan di Room; mempertahankan checkpoint dan menandai butuh tindak lanjut"
                                        )
                                        SessionRecoveryStore.markRequiresFollowUp(
                                            appContext,
                                            expectedSessionId = persisted.sessionId,
                                            status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                                            reason = "Startup recovery: target sessionId=${persisted.sessionId} tidak ditemukan di Room"
                                        )
                                    }

                                    is TargetSessionLookupResult.Found -> {
                                        val existing = lookup.session
                                        val avgCurrent = if (persisted.currentReadingsCount > 0L) {
                                            (persisted.currentSumForAvg / persisted.currentReadingsCount).toInt()
                                        } else {
                                            existing.avgCurrentMa
                                        }
                                        val sampleEntity = persisted.toSampleEntityOrNull()
                                        val reconciled = runCatching {
                                            dao.reconcileRecoveredSessionAtomically(
                                                sessionId = persisted.sessionId,
                                                recoveredTotalMah = persisted.accumulatedMah,
                                                recoveredAvgCurrentMa = avgCurrent,
                                                recoveredMaxTempCelsius = persisted.maxSessionTemp,
                                                recoveredHasAnomaly = persisted.alertedAnomalyCodes.isNotEmpty(),
                                                recoveredAnomalyReason = persisted.alertedAnomalyCodes.joinToString(", ").ifEmpty { null },
                                                recoveredSample = sampleEntity
                                            )
                                        }.onFailure { error ->
                                            Log.e(
                                                "PowerGuard",
                                                "Startup recovery gagal merekonsiliasi Room untuk sessionId=${persisted.sessionId}; checkpoint dipertahankan: ${error.message}",
                                                error
                                            )
                                        }.getOrDefault(false)

                                        val verifiedSample = sampleEntity == null ||
                                            runCatching { dao.isSamplePersisted(persisted.sessionId, sampleEntity.timestamp) }.getOrDefault(false)

                                        if (reconciled && verifiedSample) {
                                            val postReconcileRow = runCatching { dao.getSessionById(persisted.sessionId) }.getOrNull()
                                            val targetClosed = if (postReconcileRow != null && postReconcileRow.endTime == null) {
                                                val status = runCatching {
                                                    dao.finalizeSingleSessionWithReconciledMetrics(
                                                        sessionId = persisted.sessionId,
                                                        endTime = now,
                                                        endLevel = snapshot.level,
                                                        designCapacityMah = designCapacityMah
                                                    )
                                                }.getOrNull()
                                                (status == SessionFinalizationStatus.FINALIZED ||
                                                    status == SessionFinalizationStatus.ALREADY_CLOSED) &&
                                                    runCatching { dao.getSessionById(persisted.sessionId)?.endTime != null }.getOrDefault(false)
                                            } else {
                                                postReconcileRow?.endTime != null
                                            }

                                            if (targetClosed) {
                                                val cleared = SessionRecoveryStore.clear(
                                                    appContext,
                                                    expectedSessionId = persisted.sessionId
                                                )
                                                if (cleared) {
                                                    blockedSessionIds.remove(persisted.sessionId)
                                                } else {
                                                    Log.e(
                                                        "PowerGuard",
                                                        "Startup recovery sukses tetapi gagal menghapus checkpoint sessionId=${persisted.sessionId}"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!hasGlobalCheckpointCorruptionOrError) {
                        val openSessions = runCatching { dao.getAllOpenSessions() }.getOrDefault(emptyList())
                        for (openSession in openSessions) {
                            if (openSession.id in blockedSessionIds || hasActiveServiceSessionFor(openSession.id)) {
                                continue
                            }
                            runCatching {
                                dao.finalizeSingleSessionWithReconciledMetrics(
                                    sessionId = openSession.id,
                                    endTime = now,
                                    endLevel = snapshot.level,
                                    designCapacityMah = designCapacityMah
                                )
                            }.onFailure { error ->
                                Log.e(
                                    "PowerGuard",
                                    "Gagal memfinalisasi sesi terbuka id=${openSession.id} saat startup; sesi lain tetap diproses: ${error.message}",
                                    error
                                )
                            }
                        }
                    }
                }
            }
        }

        fun requestStartService(context: Context, manual: Boolean) {
            val gen = registerPendingStart()
            val targetInstance = synchronized(lifecycleLock) { activeInstance }
            targetInstance?.lifecycleChannel?.trySend(LifecycleCommand.PrepareStart(gen))

            val serviceIntent = Intent(context, BatteryMonitorService::class.java).apply {
                action = if (manual) ACTION_START_MANUAL_MONITORING else ACTION_START_MONITORING
                putExtra(EXTRA_FROM_REQUEST_START, true)
                putExtra(EXTRA_START_GENERATION, gen)
            }
            runCatching {
                ContextCompat.startForegroundService(context, serviceIntent)
            }.onFailure { error ->
                removePendingStart(gen)
                val failInstance = synchronized(lifecycleLock) { activeInstance }
                failInstance?.lifecycleChannel?.trySend(LifecycleCommand.StartFailed(gen))
                throw error
            }
        }
    }
}
