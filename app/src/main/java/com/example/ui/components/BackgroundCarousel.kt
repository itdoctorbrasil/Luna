package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.BannerItem

@Composable
fun BackgroundCarousel(
    banners: List<BannerItem>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val currentBanner = banners.getOrNull(currentIndex) ?: banners.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = currentBanner?.imageUrl,
            animationSpec = tween(durationMillis = 1000),
            label = "banner_crossfade"
        ) { url ->
            if (!url.isNullOrEmpty()) {
                AsyncImage(
                    model = url,
                    contentDescription = "Background Wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E1B4B),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                )
            }
        }

        // Overlay gradient for maximum readability of TV top bar and dock
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )
    }
}
