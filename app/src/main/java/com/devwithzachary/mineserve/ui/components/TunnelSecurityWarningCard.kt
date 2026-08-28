package com.devwithzachary.mineserve.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.ui.theme.GoldYellow

@Composable
fun TunnelSecurityWarningCard(
    modifier: Modifier = Modifier,
    prefKey: String = "tunnel_security_warning_dismissed"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mineserve_ui_prefs", Context.MODE_PRIVATE) }
    var isDismissed by remember { mutableStateOf(prefs.getBoolean(prefKey, false)) }

    AnimatedVisibility(
        visible = !isDismissed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = GoldYellow.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, GoldYellow.copy(alpha = 0.35f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = GoldYellow,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.tunnel_security_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldYellow,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        isDismissed = true
                        prefs.edit().putBoolean(prefKey, true).apply()
                    },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Warning",
                        tint = GoldYellow.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
