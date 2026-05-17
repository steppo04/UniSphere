package com.example.unisphere.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun UniSphereAvatar(
    username: String,
    profilePictureUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showBorder: Boolean = false,
    borderColor: Color = Color(0xFFFFB300),
    onClick: (() -> Unit)? = null,
    badge: @Composable (BoxScope.() -> Unit)? = null
) {
    val avatarModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(if (showBorder) Modifier.border(1.5.dp, borderColor, CircleShape) else Modifier)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Box(contentAlignment = Alignment.TopEnd) {
        if (!profilePictureUri.isNullOrEmpty()) {
            AsyncImage(
                model = profilePictureUri,
                contentDescription = null,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = username.take(1).uppercase()
            Box(
                modifier = avatarModifier.background(
                    Brush.linearGradient(colors = listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        badge?.invoke(this)
    }
}