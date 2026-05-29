package com.charles.scamradar.app.quicksettings

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.charles.scamradar.app.R
import com.charles.scamradar.app.data.db.AppDatabase
import com.charles.scamradar.app.ui.quickverdict.QuickVerdictActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings tile that opens [QuickVerdictActivity] with the current clipboard
 * contents prefilled. Tile state reflects the most-recent verdict color.
 */
class ScamRadarTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, QuickVerdictActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.label = getString(R.string.qs_tile_label)
        tile.subtitle = getString(R.string.qs_tile_subtitle)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_shield)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()

        scope.launch {
            val dao = AppDatabase.getInstance(applicationContext).scanHistoryDao()
            val latest = dao.getAll().first().maxByOrNull { it.timestamp }
            val state = when (latest?.verdict) {
                "LIKELY_SCAM" -> Tile.STATE_ACTIVE
                "SUSPICIOUS" -> Tile.STATE_ACTIVE
                "SAFE" -> Tile.STATE_INACTIVE
                else -> Tile.STATE_INACTIVE
            }
            tile.state = state
            tile.updateTile()
        }
    }
}
