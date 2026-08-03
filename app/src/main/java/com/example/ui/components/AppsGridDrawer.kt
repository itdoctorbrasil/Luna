package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.InstalledApp

@Composable
fun AppsGridDrawer(
    isVisible: Boolean,
    installedApps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit,
    onAppMenuUninstall: (InstalledApp) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember(installedApps) {
        List(installedApps.size) { FocusRequester() }
    }

    LaunchedEffect(isVisible, installedApps.isNotEmpty()) {
        if (isVisible && installedApps.isNotEmpty()) {
            kotlinx.coroutines.delay(80)
            focusRequesters.firstOrNull()?.requestFocus()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                        onClose()
                        true
                    } else false
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color(0xFF0F172A).copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 40.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Todos os Aplicativos",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Grade de Apps Inferior",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pressione MENU no controle para desinstalar um app",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close_apps_grid")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (installedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Carregando aplicativos do sistema...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(installedApps, key = { _, app -> app.packageName }) { index, app ->
                            val focusRequester = focusRequesters.getOrNull(index) ?: remember { FocusRequester() }
                            AppGridItem(
                                app = app,
                                index = index,
                                totalCount = installedApps.size,
                                focusRequester = focusRequester,
                                onNavigateLeft = {
                                    if (index > 0) {
                                        focusRequesters.getOrNull(index - 1)?.requestFocus()
                                    }
                                },
                                onNavigateRight = {
                                    if (index < installedApps.size - 1) {
                                        focusRequesters.getOrNull(index + 1)?.requestFocus()
                                    }
                                },
                                onNavigateUp = {
                                    if (index >= 6) {
                                        focusRequesters.getOrNull(index - 6)?.requestFocus()
                                    } else {
                                        onClose()
                                    }
                                },
                                onNavigateDown = {
                                    if (index + 6 < installedApps.size) {
                                        focusRequesters.getOrNull(index + 6)?.requestFocus()
                                    }
                                },
                                onClose = onClose,
                                onClick = { onAppClick(app) },
                                onMenuClick = { onAppMenuUninstall(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppGridItem(
    app: InstalledApp,
    index: Int = 0,
    totalCount: Int = 1,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onNavigateLeft: () -> Unit = {},
    onNavigateRight: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onNavigateDown: () -> Unit = {},
    onClose: () -> Unit = {},
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "grid_item_scale"
    )

    val shape = RoundedCornerShape(18.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .width(110.dp)
            .clip(shape)
            .background(
                color = if (isFocused) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color(0xFF00E5FF) else Color.Transparent,
                shape = shape
            )
            .padding(12.dp)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (index > 0) {
                                onNavigateLeft()
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (index < totalCount - 1) {
                                onNavigateRight()
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            onNavigateUp()
                            true
                        }
                        Key.DirectionDown -> {
                            onNavigateDown()
                            true
                        }
                        Key.Back, Key.Escape -> {
                            onClose()
                            true
                        }
                        Key.Menu -> {
                            onMenuClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("app_item_${app.packageName}")
    ) {
        // App Icon
        if (app.icon != null) {
            val bitmap = remember(app.icon) {
                try {
                    app.icon.toBitmap(width = 96, height = 96).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.title,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                DefaultAppIconPlaceholder()
            }
        } else {
            DefaultAppIconPlaceholder()
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DefaultAppIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0288D1)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = "App Icon",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
