package com.devwithzachary.mineserve.system

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.devwithzachary.mineserve.model.ServerStatus

@RequiresApi(Build.VERSION_CODES.N)
class MineServeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        SystemIntegrationHelper.togglePrimaryServer(applicationContext)
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val snapshot = SystemIntegrationHelper.getPrimaryServerSnapshot(applicationContext)
        val server = snapshot.server
        if (server == null) {
            tile.label = "MineServe"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "No servers"
            }
            tile.state = Tile.STATE_UNAVAILABLE
        } else {
            tile.label = server.name
            val isRunning = snapshot.status == ServerStatus.RUNNING || snapshot.status == ServerStatus.STARTING
            tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = when (snapshot.status) {
                    ServerStatus.RUNNING -> "Online (${snapshot.onlinePlayers}/${snapshot.maxPlayers})"
                    ServerStatus.STARTING -> "Starting..."
                    ServerStatus.STOPPING -> "Stopping..."
                    else -> "Stopped (Tap to start)"
                }
            }
        }
        tile.updateTile()
    }
}

