package com.devwithzachary.mineserve.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.devwithzachary.mineserve.model.getPlayerAvatarUrl
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800

@Composable
fun PlayerAvatar(
    username: String,
    uuid: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val context = LocalContext.current
    val avatarUrl = getPlayerAvatarUrl(username, uuid)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Slate800),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = username,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape),
            loading = {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(Slate800),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = username.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        text = initial,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = (size.value * 0.45f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(Slate800),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = username.trim().firstOrNull()?.uppercaseChar()?.toString()
                    if (!initial.isNullOrBlank()) {
                        Text(
                            text = initial,
                            color = EmeraldPrimary,
                            fontSize = (size.value * 0.45f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(size * 0.6f)
                        )
                    }
                }
            }
        )
    }
}
