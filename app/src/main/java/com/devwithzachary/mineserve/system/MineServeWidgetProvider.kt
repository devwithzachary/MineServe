package com.devwithzachary.mineserve.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.devwithzachary.mineserve.MainActivity
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.ServerStatus

class MineServeWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_SERVER = "com.devwithzachary.mineserve.widget.TOGGLE_PRIMARY"

        fun updateAllWidgets(context: Context) {
            val appCtx = context.applicationContext
            val manager = AppWidgetManager.getInstance(appCtx)
            val ids = manager.getAppWidgetIds(ComponentName(appCtx, MineServeWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val snapshot = SystemIntegrationHelper.getPrimaryServerSnapshot(appCtx)
            for (id in ids) {
                updateWidget(appCtx, manager, id, snapshot)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            snapshot: PrimaryServerSnapshot
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_mineserve)
            val server = snapshot.server

            if (server == null) {
                views.setTextViewText(R.id.widget_server_name, "MineServe")
                views.setTextViewText(R.id.widget_status_text, "No servers created")
                views.setTextViewText(R.id.widget_stats_text, "Tap to create your first server")
                views.setTextViewText(R.id.widget_action_btn, "Open")
            } else {
                val isRunning = snapshot.status == ServerStatus.RUNNING || snapshot.status == ServerStatus.STARTING
                views.setTextViewText(R.id.widget_server_name, server.name)
                views.setTextViewText(
                    R.id.widget_status_text,
                    when (snapshot.status) {
                        ServerStatus.RUNNING -> "ONLINE"
                        ServerStatus.STARTING -> "STARTING"
                        ServerStatus.STOPPING -> "STOPPING"
                        else -> "STOPPED"
                    }
                )
                views.setTextViewText(
                    R.id.widget_stats_text,
                    if (isRunning) {
                        "${snapshot.onlinePlayers}/${snapshot.maxPlayers} players - ${snapshot.ramMb} MB RAM"
                    } else {
                        "${server.type.displayName} ${server.version} - Port ${server.port}"
                    }
                )
                views.setTextViewText(R.id.widget_action_btn, if (isRunning) "Stop" else "Start")
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            val toggleIntent = Intent(context, MineServeWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_SERVER
            }
            val togglePending = PendingIntent.getBroadcast(
                context,
                1,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action_btn, togglePending)

            manager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_SERVER) {
            SystemIntegrationHelper.togglePrimaryServer(context)
            updateAllWidgets(context)
        }
    }
}

