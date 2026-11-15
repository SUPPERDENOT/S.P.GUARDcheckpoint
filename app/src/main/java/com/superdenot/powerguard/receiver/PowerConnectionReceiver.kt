package com.superdenot.powerguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import com.superdenot.powerguard.data.local.AppDatabase
import com.superdenot.powerguard.data.local.SessionRecoveryStore
import com.superdenot.powerguard.data.local.dao.BatteryDao
import com.superdenot.powerguard.data.local.dao.SessionFinalizationStatus
import com.superdenot.powerguard.data.repository.AutoMonitoringSettings
import com.superdenot.powerguard.domain.engine.BatteryMonitorEngine
import com.superdenot.powerguard.service.BatteryMonitorService
import com.superdenot.powerguard.service.TargetSessionLookupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (isDeviceCurrentlyCharging(context)) {
                    // BOOT_COMPLETED is an Android background-FGS exemption, subject to
                    // foreground-service-type restrictions on the device's OS version.
                    startMonitorServiceSafe(context, requiresBatteryOptimizationExemption = false)
                } else if (!BatteryMonitorService.isStartingOrRunning &&
                    !BatteryMonitorService.hasActiveServiceSessionFor()
                ) {
                    scheduleGuardedBackgroundCleanup(context)
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                // POWER_CONNECTED is not itself a general background-FGS exemption.
                startMonitorServiceSafe(context, requiresBatteryOptimizationExemption = true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                handlePowerDisconnected(context)
            }
        }
    }

    private fun startMonitorServiceSafe(
        context: Context,
        requiresBatteryOptimizationExemption: Boolean
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = AutoMonitoringSettings.isEnabled(appContext)
                val allowed = if (requiresBatteryOptimizationExemption) {
                    val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                    AutoMonitoringSettings.canStartFromPowerConnected(
                        autoMonitoringEnabled = enabled,
                        batteryOptimizationIgnored =
                            powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
                    )
                } else {
                    // Avoid an unnecessary system-service dependency for the BOOT_COMPLETED path.
                    enabled
                }

                if (allowed) {
                    BatteryMonitorService.requestStartService(appContext, manual = false)
                } else if (!enabled) {
                    Log.i("PowerGuard", "Auto Monitoring nonaktif; pemicu charger/boot diabaikan")
                } else {
                    Log.w(
                        "PowerGuard",
                        "Auto-start saat charger terhubung dilewati: pengecualian optimasi baterai belum diberikan"
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                Log.e("PowerGuard", "Gagal memulai Foreground Service dari receiver: ${error.message}", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handlePowerDisconnected(context: Context) {
        if (BatteryMonitorService.isStartingOrRunning || BatteryMonitorService.hasActiveServiceSessionFor()) {
            val stopSessionIntent = Intent(context, BatteryMonitorService::class.java).apply {
                action = BatteryMonitorService.ACTION_STOP_CHARGING_SESSION
            }
            val delivered = runCatching {
                context.startService(stopSessionIntent)
            }.isSuccess

            if (delivered) {
                return
            }
        }

        scheduleGuardedBackgroundCleanup(context)
    }

    private fun scheduleGuardedBackgroundCleanup(context: Context) {
        val pendingResult: BroadcastReceiver.PendingResult? = goAsync()
        val appContext = context.applicationContext

        if (!cleanupInProgress.compareAndSet(false, true)) {
            cleanupRescheduleRequested.set(true)
            pendingResult?.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                do {
                    cleanupRescheduleRequested.set(false)
                    executeGuardedBackgroundCleanupNow(appContext)
                } while (cleanupRescheduleRequested.getAndSet(false))
            } finally {
                cleanupInProgress.set(false)
                if (cleanupRescheduleRequested.getAndSet(false) &&
                    cleanupInProgress.compareAndSet(false, true)
                ) {
                    try {
                        executeGuardedBackgroundCleanupNow(appContext)
                    } finally {
                        cleanupInProgress.set(false)
                    }
                }
                pendingResult?.finish()
            }
        }
    }

    /**
     * Menjalankan cleanup latar belakang multi-sesi yang disinkronkan dengan sharedSessionMutex:
     * 1. Memeriksa seluruh record checkpoint per sessionId secara eksplisit (EMPTY, LOADED, UNAVAILABLE, CORRUPT).
     * 2. Merekonsiliasi tiap checkpoint recovery persisten ke Room terlebih dahulu, memverifikasi hasil DB,
     *    dan HANYA menghapus checkpoint untuk sessionId terkait setelah transaksi Room & penutupan sesi terverifikasi.
     * 3. Memfinalisasi setiap sesi terbuka tersisa yang tidak diblokir secara individual (bukan penutupan massal buta),
     *    sehingga kegagalan atau korupsi pada satu sesi tidak mengorbankan data sesi lain.
     */
    internal suspend fun executeGuardedBackgroundCleanupNow(
        context: Context,
        daoOverride: BatteryDao? = null,
        isChargingOverride: Boolean? = null,
        batteryLevelOverride: Int? = null
    ) {
        val appContext = context.applicationContext
        try {
            val initialCharging = isChargingOverride ?: isDeviceCurrentlyCharging(appContext)
            if (initialCharging) {
                return
            }

            BatteryMonitorService.sharedSessionMutex.withLock {
                val stillCharging = isChargingOverride ?: isDeviceCurrentlyCharging(appContext)
                if (stillCharging) {
                    return@withLock
                }
                if (BatteryMonitorService.isStartingOrRunning ||
                    BatteryMonitorService.hasActiveServiceSessionFor()
                ) {
                    return@withLock
                }

                val currentPct = batteryLevelOverride ?: readCurrentBatteryLevel(appContext)
                val dao = daoOverride ?: AppDatabase.getInstance(appContext).batteryDao()
                val designCapacityMah = runCatching {
                    BatteryMonitorEngine(appContext).designCapacityMah
                }.getOrDefault(5000)
                val now = System.currentTimeMillis()

                withContext(NonCancellable) {
                    val blockedSessionIds = mutableSetOf<Long>()
                    var hasGlobalCheckpointCorruptionOrError = false

                    // Langkah 1: Rekonsiliasi & finalisasi sesi dari seluruh checkpoint persisten per sessionId
                    val allResults = SessionRecoveryStore.loadAll(appContext)
                    for (loadResult in allResults) {
                        when (loadResult) {
                            is SessionRecoveryStore.CheckpointLoadResult.Empty -> Unit

                            is SessionRecoveryStore.CheckpointLoadResult.Unavailable -> {
                                hasGlobalCheckpointCorruptionOrError = true
                                Log.e(
                                    "PowerGuard",
                                    "Cleanup background: penyimpanan checkpoint tidak tersedia (${loadResult.reason}); mempertahankan seluruh state",
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
                                    "Cleanup background: checkpoint korup terdeteksi (rawSessionId=${loadResult.rawSessionId}): ${loadResult.reason}"
                                )
                            }

                            is SessionRecoveryStore.CheckpointLoadResult.Loaded -> {
                                val persisted = loadResult.recovery
                                blockedSessionIds.add(persisted.sessionId)
                                when (val lookup = BatteryMonitorService.lookupTargetSessionSafely(dao, persisted.sessionId)) {
                                    is TargetSessionLookupResult.DatabaseInaccessible -> {
                                        Log.e(
                                            "PowerGuard",
                                            "Cleanup background: database tidak dapat diakses untuk sessionId=${persisted.sessionId}; checkpoint dipertahankan",
                                            lookup.cause
                                        )
                                        SessionRecoveryStore.markRequiresFollowUp(
                                            appContext,
                                            expectedSessionId = persisted.sessionId,
                                            status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_DB_UNAVAILABLE,
                                            reason = "Cleanup background: database tidak dapat diakses untuk sessionId=${persisted.sessionId}"
                                        )
                                    }

                                    is TargetSessionLookupResult.MissingInAccessibleDb -> {
                                        Log.w(
                                            "PowerGuard",
                                            "Cleanup background: target sessionId=${persisted.sessionId} tidak ditemukan di Room; mempertahankan checkpoint dan menandai butuh tindak lanjut tanpa menutup sesi lain"
                                        )
                                        SessionRecoveryStore.markRequiresFollowUp(
                                            appContext,
                                            expectedSessionId = persisted.sessionId,
                                            status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_TARGET_MISSING,
                                            reason = "Cleanup background: target sessionId=${persisted.sessionId} tidak ditemukan di Room"
                                        )
                                    }

                                    is TargetSessionLookupResult.Found -> {
                                        val targetRow = lookup.session
                                        val avgCurrent = if (persisted.currentReadingsCount > 0L) {
                                            (persisted.currentSumForAvg / persisted.currentReadingsCount).toInt()
                                        } else {
                                            targetRow.avgCurrentMa
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
                                                "Rekonsiliasi Room gagal saat cleanup background (sessionId=${persisted.sessionId}); checkpoint dipertahankan: ${error.message}",
                                                error
                                            )
                                        }.getOrDefault(false)

                                        val verifiedSample = sampleEntity == null ||
                                            runCatching { dao.isSamplePersisted(persisted.sessionId, sampleEntity.timestamp) }.getOrDefault(false)

                                        if (reconciled && verifiedSample) {
                                            val finalizeStatus = runCatching {
                                                dao.finalizeSingleSessionWithReconciledMetrics(
                                                    sessionId = persisted.sessionId,
                                                    endTime = now,
                                                    endLevel = currentPct,
                                                    designCapacityMah = designCapacityMah
                                                )
                                            }.getOrNull()

                                            val verifiedRow = runCatching { dao.getSessionById(persisted.sessionId) }.getOrNull()
                                            val targetClosed = (finalizeStatus == SessionFinalizationStatus.FINALIZED ||
                                                finalizeStatus == SessionFinalizationStatus.ALREADY_CLOSED) &&
                                                verifiedRow?.endTime != null

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
                                                        "Rekonsiliasi & finalisasi background sukses tetapi gagal menghapus checkpoint sessionId=${persisted.sessionId}"
                                                    )
                                                }
                                            } else {
                                                SessionRecoveryStore.markRequiresFollowUp(
                                                    appContext,
                                                    expectedSessionId = persisted.sessionId,
                                                    status = SessionRecoveryStore.RecoveryStatus.NEEDS_FOLLOW_UP_FALLBACK_FAILED,
                                                    reason = "Cleanup background: data direkonsiliasi tetapi penutupan sesi id=${persisted.sessionId} belum terkonfirmasi (status=$finalizeStatus)"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Langkah 2: Finalisasi tiap sesi terbuka tersisa yang tidak diblokir secara individual & terisolasi
                    if (!hasGlobalCheckpointCorruptionOrError) {
                        val remainingOpen = runCatching { dao.getAllOpenSessions() }.getOrDefault(emptyList())
                        for (openSession in remainingOpen) {
                            if (openSession.id in blockedSessionIds ||
                                BatteryMonitorService.hasActiveServiceSessionFor(openSession.id)
                            ) {
                                continue
                            }
                            val singleResult = runCatching {
                                dao.finalizeSingleSessionWithReconciledMetrics(
                                    sessionId = openSession.id,
                                    endTime = now,
                                    endLevel = currentPct,
                                    designCapacityMah = designCapacityMah
                                )
                            }.onFailure { error ->
                                Log.e(
                                    "PowerGuard",
                                    "Gagal memfinalisasi sesi terbuka id=${openSession.id} di background; sesi lain tetap diproses: ${error.message}",
                                    error
                                )
                            }.getOrNull()

                            if (singleResult != SessionFinalizationStatus.FINALIZED &&
                                singleResult != SessionFinalizationStatus.ALREADY_CLOSED
                            ) {
                                Log.w(
                                    "PowerGuard",
                                    "Sesi terbuka id=${openSession.id} belum tertutup saat cleanup background (status=$singleResult)"
                                )
                            }
                        }
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (error: Throwable) {
            Log.e("PowerGuard", "Gagal menjalankan cleanup background: ${error.message}", error)
        }
    }

    private fun readCurrentBatteryLevel(context: Context): Int {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (level >= 0 && scale > 0) (level * 100) / scale else 0
    }

    private fun isDeviceCurrentlyCharging(context: Context): Boolean {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    companion object {
        private val cleanupInProgress = AtomicBoolean(false)
        private val cleanupRescheduleRequested = AtomicBoolean(false)
    }
}
