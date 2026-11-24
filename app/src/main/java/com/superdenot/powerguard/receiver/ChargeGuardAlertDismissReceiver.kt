package com.superdenot.powerguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.superdenot.powerguard.data.repository.ChargeGuardSettings
import com.superdenot.powerguard.domain.engine.ChargeGuardAlertType

/** Records a user swipe-dismissal so the active Charge Guard incident is not repeated. */
class ChargeGuardAlertDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_ALERT_TYPE)
            ?.let { raw -> runCatching { ChargeGuardAlertType.valueOf(raw) }.getOrNull() }
            ?: return
        ChargeGuardSettings.dismissRepeats(context.applicationContext, type)
    }

    companion object {
        const val EXTRA_ALERT_TYPE = "com.superdenot.powerguard.EXTRA_CHARGE_GUARD_ALERT_TYPE"
    }
}
