package com.superdenot.powerguard.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.superdenot.powerguard.PowerGuardTestApplication
import com.superdenot.powerguard.domain.model.StatusBarDisplayMode
import com.superdenot.powerguard.domain.model.StatusBarParameter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = PowerGuardTestApplication::class, manifest = Config.NONE)
class StatusBarIndicatorSettingsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        runBlocking { context.powerGuardDataStore.edit { it.clear() } }
    }

    @After
    fun tearDown() {
        runBlocking { context.powerGuardDataStore.edit { it.clear() } }
    }

    @Test
    fun freshInstallDefaultsOffAndToBatterySidePower() {
        val setting = StatusBarIndicatorSettings.resolveSetting(null, null)
        assertFalse(setting.enabled)
        assertEquals(StatusBarParameter.POWER, setting.parameter)
        assertEquals(StatusBarDisplayMode.SINGLE_METRIC, setting.displayMode)
        assertEquals(3, setting.rotationIntervalSeconds)
    }

    @Test
    fun explicitSavedMetricAndToggleRemainPreserved() {
        assertTrue(StatusBarIndicatorSettings.resolveSetting(true, "VOLTAGE").enabled)
        assertFalse(StatusBarIndicatorSettings.resolveSetting(false, "VOLTAGE").enabled)
        assertEquals(StatusBarParameter.VOLTAGE, StatusBarIndicatorSettings.resolveSetting(true, "VOLTAGE").parameter)
    }

    @Test
    fun legacyChargingStatusMapsToResourceSourceAndUnknownMetricFallsBackToPower() {
        assertEquals(StatusBarParameter.RESOURCE_SOURCE, StatusBarIndicatorSettings.resolveSetting(true, "CHARGING_STATUS").parameter)
        assertEquals(StatusBarParameter.POWER, StatusBarIndicatorSettings.resolveSetting(true, "UNKNOWN").parameter)
    }

    @Test
    fun allSixRequestedMetricsAreSelectable() {
        assertEquals(
            listOf(
                StatusBarParameter.POWER,
                StatusBarParameter.CURRENT,
                StatusBarParameter.VOLTAGE,
                StatusBarParameter.TEMPERATURE,
                StatusBarParameter.BATTERY_PERCENTAGE,
                StatusBarParameter.RESOURCE_SOURCE
            ),
            StatusBarParameter.selectableValues
        )
    }

    @Test
    fun customRotationSelectsMetricsDeterministicallyByElapsedRealtimeInterval() {
        val setting = StatusBarIndicatorSettings.resolveSetting(
            enabled = true,
            parameter = "POWER",
            displayMode = "CUSTOM_ROTATION",
            rotationParameters = setOf("POWER", "CURRENT", "VOLTAGE"),
            rotationIntervalSeconds = 3
        )
        assertEquals(StatusBarParameter.POWER, setting.parameterAtElapsedRealtime(0L))
        assertEquals(StatusBarParameter.POWER, setting.parameterAtElapsedRealtime(2_999L))
        assertEquals(StatusBarParameter.CURRENT, setting.parameterAtElapsedRealtime(3_000L))
        assertEquals(StatusBarParameter.CURRENT, setting.parameterAtElapsedRealtime(5_999L))
        assertEquals(StatusBarParameter.VOLTAGE, setting.parameterAtElapsedRealtime(6_000L))
        assertEquals(StatusBarParameter.POWER, setting.parameterAtElapsedRealtime(9_000L))
    }

    @Test
    fun singleMetricModeDoesNotRotate() {
        val setting = StatusBarIndicatorSettings.resolveSetting(
            enabled = true,
            parameter = "TEMPERATURE",
            displayMode = "SINGLE_METRIC",
            rotationParameters = setOf("POWER", "CURRENT"),
            rotationIntervalSeconds = 1
        )
        assertEquals(StatusBarParameter.TEMPERATURE, setting.parameterAtElapsedRealtime(90_000L))
    }

    @Test
    fun rotationModeParametersAndIntervalPersistInDataStore() = runBlocking {
        StatusBarIndicatorSettings.setDisplayMode(context, StatusBarDisplayMode.CUSTOM_ROTATION)
        StatusBarIndicatorSettings.setRotationParameters(
            context,
            listOf(StatusBarParameter.VOLTAGE, StatusBarParameter.POWER, StatusBarParameter.RESOURCE_SOURCE)
        )
        StatusBarIndicatorSettings.setRotationIntervalSeconds(context, 5)

        val persisted = StatusBarIndicatorSettings.observe(context).first()
        assertEquals(StatusBarDisplayMode.CUSTOM_ROTATION, persisted.displayMode)
        assertEquals(
            listOf(StatusBarParameter.POWER, StatusBarParameter.VOLTAGE, StatusBarParameter.RESOURCE_SOURCE),
            persisted.rotationParameters
        )
        assertEquals(5, persisted.rotationIntervalSeconds)
    }

    @Test
    fun rotationSelectionRequiresTwoParametersAndIntervalIsClampedToOneThroughSixty() {
        val setting = StatusBarIndicatorSettings.resolveSetting(
            enabled = true,
            parameter = "POWER",
            displayMode = "CUSTOM_ROTATION",
            rotationParameters = setOf("POWER"),
            rotationIntervalSeconds = 99
        )
        assertEquals(listOf(StatusBarParameter.POWER, StatusBarParameter.CURRENT), setting.rotationParameters)
        assertEquals(60, setting.rotationIntervalSeconds)
    }
}
