package com.superdenot.powerguard.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.superdenot.powerguard.MainActivity
import com.superdenot.powerguard.R

class PopupCheckTileService : TileService() {
    override fun onTileAdded() {
        super.onTileAdded()
        updateInactiveTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateInactiveTile()
    }

    override fun onClick() {
        super.onClick()
        updateInactiveTile()
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_POPUP_DETECTOR
            putExtra(MainActivity.EXTRA_OPEN_POPUP_DETECTOR, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launchIntent)
        }
    }

    private fun updateInactiveTile() {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_popup_shield)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.label = TILE_LABEL
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    private companion object {
        const val TILE_LABEL = "Cek Pop-up"
        const val REQUEST_CODE = 17017
        const val ACTION_OPEN_POPUP_DETECTOR = "com.superdenot.powerguard.action.OPEN_POPUP_DETECTOR"
    }
}
