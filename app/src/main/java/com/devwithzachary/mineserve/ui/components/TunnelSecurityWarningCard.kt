package com.devwithzachary.mineserve.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors

@Composable
fun TunnelSecurityWarningCard(
    modifier: Modifier = Modifier,
    prefKey: String = "tunnel_security_warning_dismissed"
) {
    val context = LocalContext.current
    val extended = LocalMineServeColors.current
    val prefs = remember { context.getSharedPreferences("mineserve_ui_prefs", Context.MODE_PRIVATE) }
    var isDismissed by remember { mutableStateOf(prefs.getBoolean(prefKey, false)) }

    AnimatedVisibility(
        visible = !isDismissed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = ShapeDefaults.Small,
            color = extended.warningContainer,
            contentColor = extended.onWarningContainer,
            border = null,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = extended.warning,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.tunnel_security_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.onWarningContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        isDismissed = true
                        prefs.edit().putBoolean(prefKey, true).apply()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss Warning",
                        tint = extended.onWarningContainer,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
