package com.devwithzachary.mineserve.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors

enum class ServerBadgeState(val defaultLabel: String) {
    Online("Online"),
    Starting("Starting"),
    Stopping("Stopping"),
    Crashed("Crashed"),
    Error("Error"),
    Offline("Offline")
}

/**
 * Compact tonal status pill (`ShapeDefaults.Small`, zero border) mapping server state
 * to Ore semantic tokens.
 */
@Composable
fun ServerStatusBadge(
    state: ServerBadgeState,
    modifier: Modifier = Modifier,
    label: String = state.defaultLabel
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current

    val (containerColor, contentColor, dotColor) = when (state) {
        ServerBadgeState.Online -> Triple(
            colorScheme.primaryContainer,
            colorScheme.onPrimaryContainer,
            extended.onlineEmerald
        )
        ServerBadgeState.Starting,
        ServerBadgeState.Stopping -> Triple(
            extended.warningContainer,
            extended.onWarningContainer,
            extended.warning
        )
        ServerBadgeState.Crashed,
        ServerBadgeState.Error -> Triple(
            colorScheme.errorContainer,
            colorScheme.onErrorContainer,
            colorScheme.error
        )
        ServerBadgeState.Offline -> Triple(
            colorScheme.surfaceContainerHighest,
            colorScheme.onSurfaceVariant,
            colorScheme.outline
        )
    }

    Surface(
        modifier = modifier,
        shape = ShapeDefaults.Small,
        color = containerColor,
        contentColor = contentColor,
        border = null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}
