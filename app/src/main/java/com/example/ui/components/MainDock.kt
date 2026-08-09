package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.data.DockAppItem

// Card background gradient palettes inspired by Apple TV / JMGO Luna design
private val dockGradients = listOf(
    listOf(Color(0xFF4285F4), Color(0xFF1976D2)), // Blue
    listOf(Color(0xFF26A69A), Color(0xFF00897B)), // Teal/Green
    listOf(Color(0xFF7E57C2), Color(0xFF512DA8)), // Purple
    listOf(Color(0xFFFF7043), Color(0xFFE64A19)), // Coral/Orange
    listOf(Color(0xFF26C6DA), Color(0xFF0097A7)), // Cyan
    listOf(Color(0xFFEC407A), Color(0xFFC2185B))  // Pink
)

@Composable
fun MainDock(
    dockApps: List<DockAppItem>,
    installedPackageNames: Set<String> = emptySet(),
    onAppsGridClick: () -> Unit,
    onDockAppClick: (DockAppItem) -> Unit,
    onNavigateDownToGrid: () -> Unit,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember(dockApps) {
        List(dockApps.size) { FocusRequester() }
    }

    androidx.compose.runtime.LaunchedEffect(isActive, dockApps.isNotEmpty()) {
        if (isActive && dockApps.isNotEmpty()) {
            kotlinx.coroutines.delay(80)
            focusRequesters.firstOrNull()?.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 36.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = dockApps,
                key = { index, app -> "${index}_${app.id}_${app.packageName.ifEmpty { "no_pkg" }}" }
            ) { index, app ->
                val gradient = dockGradients[index % dockGradients.size]
                val isInstalled = app.packageName.isNotEmpty() && installedPackageNames.contains(app.packageName)
                val focusRequester = focusRequesters.getOrNull(index) ?: remember { FocusRequester() }

                DockAppTile(
                    app = app,
                    isInstalled = isInstalled,
                    index = index,
                    totalCount = dockApps.size,
                    focusRequester = focusRequester,
                    gradientColors = gradient,
                    onNavigateLeft = {
                        if (index > 0) {
                            focusRequesters.getOrNull(index - 1)?.requestFocus()
                        }
                    },
                    onNavigateRight = {
                        if (index < dockApps.size - 1) {
                            focusRequesters.getOrNull(index + 1)?.requestFocus()
                        }
                    },
                    onClick = { onDockAppClick(app) },
                    onNavigateDown = onNavigateDownToGrid
                )
            }
        }
    }
}

@Composable
fun AppsGridTile(
    onClick: () -> Unit,
    onNavigateDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tile_scale"
    )

    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .width(150.dp)
            .height(86.dp)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isFocused) {
                        listOf(Color.White, Color(0xFFE0E0E0))
                    } else {
                        listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.65f))
                    }
                )
            )
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color(0xFF00E5FF) else Color.Transparent,
                shape = shape
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                    onNavigateDown()
                    true
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("btn_apps_grid_tile"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00E5FF))
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF212121))
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF212121))
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00E5FF))
                )
            }
        }
    }
}

@Composable
fun DockAppTile(
    app: DockAppItem,
    isInstalled: Boolean = true,
    index: Int = 0,
    totalCount: Int = 1,
    focusRequester: FocusRequester = remember { FocusRequester() },
    gradientColors: List<Color>,
    onNavigateLeft: () -> Unit = {},
    onNavigateRight: () -> Unit = {},
    onClick: () -> Unit,
    onNavigateDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tile_scale"
    )

    val shape = RoundedCornerShape(18.dp)
    val context = LocalContext.current

    val isSettingsPackage = app.packageName == "com.android.tv.settings" || app.packageName == "com.android.settings"

    Box(
        modifier = modifier
            .scale(scale)
            .width(180.dp)
            .height(85.dp)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isFocused) {
                        gradientColors.map { it.copy(alpha = 1.0f) }
                    } else {
                        gradientColors.map { it.copy(alpha = 0.82f) }
                    }
                )
            )
            .border(
                width = if (isFocused) 3.5.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = shape
            )
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionDown -> {
                            onNavigateDown()
                            true
                        }
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
            .testTag("dock_app_${app.packageName.ifEmpty { app.id }}"),
        contentAlignment = Alignment.Center
    ) {
        val imageUrl = app.bannerUrl.ifEmpty { app.iconUrl }

        // Se o painel Web enviou uma imagem/URL, prioriza a URL do servidor
        if (imageUrl.isNotEmpty()) {
            val modelRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .crossfade(true)
                    .apply {
                        if (isSettingsPackage) {
                            error(R.drawable.settings)
                            fallback(R.drawable.settings)
                        }
                    }
                    .build()
            }

            AsyncImage(
                model = modelRequest,
                contentDescription = app.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        } else if (isSettingsPackage) {
            // Se for o app de configurações e NÃO tiver imagem vinda do painel Web, usa o drawable interno
            Image(
                bitmap = remember {
                    val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.settings)
                    drawable?.toBitmap(380, 180)?.asImageBitmap()
                } ?: run {
                    return@Box
                },
                contentDescription = app.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        } else if (app.iconDrawable != null) {
            val isBannerDrawable = remember(app.iconDrawable) {
                val w = app.iconDrawable.intrinsicWidth
                val h = app.iconDrawable.intrinsicHeight
                (w > 0 && h > 0 && (w.toFloat() / h.toFloat()) >= 1.4f)
            }
            val bitmap = remember(app.iconDrawable) {
                try {
                    if (isBannerDrawable) {
                        app.iconDrawable.toBitmap(width = 380, height = 180).asImageBitmap()
                    } else {
                        app.iconDrawable.toBitmap(width = 128, height = 128).asImageBitmap()
                    }
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap != null && isBannerDrawable) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else if (bitmap != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.title,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                val lowerTitle = app.title.lowercase()
                val vectorIcon = when {
                    lowerTitle.contains("youtube") -> Icons.Default.PlayCircle
                    lowerTitle.contains("netflix") || lowerTitle.contains("prime") || lowerTitle.contains("filme") -> Icons.Default.Movie
                    lowerTitle.contains("chrome") || lowerTitle.contains("browser") || lowerTitle.contains("web") -> Icons.Default.Language
                    lowerTitle.contains("play") || lowerTitle.contains("store") -> Icons.Default.ShoppingBag
                    else -> Icons.Default.Tv
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = vectorIcon,
                        contentDescription = app.title,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            val lowerTitle = app.title.lowercase()
            val vectorIcon = when {
                lowerTitle.contains("youtube") -> Icons.Default.PlayCircle
                lowerTitle.contains("netflix") || lowerTitle.contains("prime") || lowerTitle.contains("filme") -> Icons.Default.Movie
                lowerTitle.contains("chrome") || lowerTitle.contains("browser") || lowerTitle.contains("web") -> Icons.Default.Language
                lowerTitle.contains("play") || lowerTitle.contains("store") -> Icons.Default.ShoppingBag
                else -> Icons.Default.Tv
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = app.title,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // "Baixar" badge overlay for uninstalled dock apps
        if (!isInstalled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp, end = 6.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF0288D1))
                            )
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Baixar",
                        tint = Color.Black,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Baixar",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}