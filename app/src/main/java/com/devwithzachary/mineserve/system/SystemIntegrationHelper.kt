package com.devwithzachary.mineserve.system

import android.content.Context
import com.devwithzachary.mineserve.engine.JavaRuntimeManager
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.engine.ServerProcessManager
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.repository.ServerRepository
import com.devwithzachary.mineserve.service.MineServeForegroundService
import kotlinx.coroutines.runBlocking

data class PrimaryServerSnapshot(
    val server: MinecraftServer?,
    val status: ServerStatus,
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val ramMb: Int
)

object SystemIntegrationHelper {

    fun getPrimaryServerSnapshot(context: Context): PrimaryServerSnapshot {
        val appCtx = context.applicationContext
        val pRoot = PRootEngine(appCtx)
        val javaMgr = JavaRuntimeManager(appCtx, pRoot)
        val processManager = ServerProcessManager.getInstance(appCtx, pRoot, javaMgr)
        val repo = ServerRepository(appCtx, pRoot)
        val servers = runBlocking { repo.loadServers() }

        val statuses = processManager.serverStatuses.value
        val metricsMap = processManager.serverMetrics.value

        val primary = servers.firstOrNull { s ->
            val st = statuses[s.id] ?: ServerStatus.STOPPED
            st == ServerStatus.RUNNING || st == ServerStatus.STARTING
        } ?: servers.firstOrNull()

        if (primary == null) {
            return PrimaryServerSnapshot(null, ServerStatus.STOPPED, 0, 20, 0)
        }
        val status = statuses[primary.id] ?: ServerStatus.STOPPED
        val metrics = metricsMap[primary.id]
        return PrimaryServerSnapshot(
            server = primary,
            status = status,
            onlinePlayers = metrics?.onlinePlayerCount ?: 0,
            maxPlayers = 20,
            ramMb = metrics?.ramUsedMb?.toInt() ?: 0
        )
    }

    fun togglePrimaryServer(context: Context) {
        val appCtx = context.applicationContext
        val pRoot = PRootEngine(appCtx)
        val javaMgr = JavaRuntimeManager(appCtx, pRoot)
        val processManager = ServerProcessManager.getInstance(appCtx, pRoot, javaMgr)
        val repo = ServerRepository(appCtx, pRoot)
        val snapshot = getPrimaryServerSnapshot(appCtx)
        val server = snapshot.server ?: return

        if (snapshot.status == ServerStatus.RUNNING || snapshot.status == ServerStatus.STARTING) {
            processManager.stopServer(server.id)
        } else {
            MineServeForegroundService.start(appCtx)
            processManager.startServer(server, repo.getServerDirectory(server.id))
        }
        MineServeWidgetProvider.updateAllWidgets(appCtx)
    }
}
