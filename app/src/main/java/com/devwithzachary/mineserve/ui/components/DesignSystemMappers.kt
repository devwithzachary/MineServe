package com.devwithzachary.mineserve.ui.components

import com.devwithzachary.mineserve.designsystem.components.ServerBadgeState
import com.devwithzachary.mineserve.model.ServerStatus

fun ServerStatus.toBadgeState(): ServerBadgeState = when (this) {
    ServerStatus.RUNNING -> ServerBadgeState.Online
    ServerStatus.STARTING -> ServerBadgeState.Starting
    ServerStatus.STOPPING -> ServerBadgeState.Stopping
    ServerStatus.ERROR -> ServerBadgeState.Error
    ServerStatus.STOPPED,
    ServerStatus.STANDBY -> ServerBadgeState.Offline
}
