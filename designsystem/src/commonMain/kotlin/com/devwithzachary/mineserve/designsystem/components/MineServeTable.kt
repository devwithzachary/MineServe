package com.devwithzachary.mineserve.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily

/**
 * Single borderless surfaceContainer wrapper (16dp radius) for tables, file explorers,
 * backups, player rosters, and audit logs.
 */
@Composable
fun MineServeTableContainer(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeDefaults.Large,
        color = colorScheme.surfaceContainer,
        contentColor = colorScheme.onSurface,
        border = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (header != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    header()
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
            content()
        }
    }
}

/**
 * Table row separated strictly by an optional 1dp outlineVariant (40% alpha) horizontal divider.
 */
@Composable
fun MineServeTableRow(
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = rowModifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Tabular JetBrains Mono text helper for filenames, paths, byte sizes, timestamps,
 * UUIDs, and IP addresses inside MineServeTableRow.
 */
@Composable
fun MineServeTableMonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = 1
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = monoFont,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = "tnum"
        ),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
