package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BootVideoPlayer(
    isVisible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        exit = fadeOut(animationSpec = tween(500)),
        modifier = modifier.fillMaxSize()
    ) {
        val context = LocalContext.current
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onFinished
                )
                .focusable()
                .onKeyEvent {
                    onFinished()
                    true
                },
            contentAlignment = Alignment.Center
        ) {
            val rawResId = remember {
                val res1 = context.resources.getIdentifier("bootvideo", "raw", context.packageName)
                if (res1 != 0) res1 else context.resources.getIdentifier("welcome", "raw", context.packageName)
            }

            if (rawResId != 0) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val videoUri = Uri.parse("android.resource://${ctx.packageName}/$rawResId")
                            setVideoURI(videoUri)
                            setOnCompletionListener {
                                onFinished()
                            }
                            setOnErrorListener { _, _, _ ->
                                onFinished()
                                true
                            }
                            requestFocus()
                            start()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LaunchedEffect(Unit) {
                    onFinished()
                }
            }
        }
    }
}
