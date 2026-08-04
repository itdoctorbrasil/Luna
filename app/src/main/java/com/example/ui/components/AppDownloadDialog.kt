package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AppDownloadProgress
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AppDownloadDialog(
    downloadProgress: AppDownloadProgress,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!downloadProgress.isVisible) return

    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(downloadProgress.isVisible, downloadProgress.isDownloading, downloadProgress.isDownloadComplete) {
        if (downloadProgress.isVisible) {
            delay(100)
            try {
                primaryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val brandColor = when {
        downloadProgress.errorMessage != null -> Color(0xFFEF4444)
        downloadProgress.isDownloadComplete -> Color(0xFF10B981)
        downloadProgress.title.lowercase().contains("unitv") || downloadProgress.packageName.contains("unitv") -> Color(0xFFE50914)
        downloadProgress.title.lowercase().contains("play") || downloadProgress.packageName.contains("vending") -> Color(0xFF00875F)
        downloadProgress.title.lowercase().contains("prime") || downloadProgress.packageName.contains("amazon") -> Color(0xFF00A8E1)
        downloadProgress.title.lowercase().contains("disney") || downloadProgress.packageName.contains("disney") -> Color(0xFF113CCF)
        downloadProgress.title.lowercase().contains("youtube") || downloadProgress.packageName.contains("youtube") -> Color(0xFFFF0000)
        else -> Color(0xFF00E5FF)
    }

    Dialog(
        onDismissRequest = {
            if (!downloadProgress.isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF111827),
                            Color(0xFF0B0F17)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            brandColor.copy(alpha = 0.8f),
                            Color(0xFF3B82F6).copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (downloadProgress.isOtaUpdate) "ATUALIZAÇÃO DE SISTEMA" else "DOWNLOAD DE APP",
                        color = brandColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )

                    if (!downloadProgress.isDownloading) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Circular Progress Indicator / Status Box
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        downloadProgress.errorMessage != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(2.dp, Color(0xFFEF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Erro",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        downloadProgress.isDownloadComplete -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .border(2.dp, Color(0xFF10B981), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Concluído",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        downloadProgress.isDownloading -> {
                            if (downloadProgress.totalBytes > 0) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress.progressPercent / 100f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = brandColor,
                                    strokeWidth = 5.dp,
                                    trackColor = brandColor.copy(alpha = 0.15f)
                                )
                                Text(
                                    text = "${downloadProgress.progressPercent}%",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.fillMaxSize(),
                                    color = brandColor,
                                    strokeWidth = 5.dp,
                                    trackColor = brandColor.copy(alpha = 0.15f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Baixando",
                                    tint = brandColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(brandColor.copy(alpha = 0.15f))
                                    .border(2.dp, brandColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Iniciar",
                                    tint = brandColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // App/Update Title
                Text(
                    text = downloadProgress.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle / Progress Details
                val subtitleText = when {
                    downloadProgress.errorMessage != null -> downloadProgress.errorMessage
                    downloadProgress.isInstalling -> "Abrindo instalador..."
                    downloadProgress.isDownloadComplete -> "Pronto para instalar no dispositivo"
                    downloadProgress.isDownloading -> {
                        val mbDownloaded = downloadProgress.downloadedBytes / (1024f * 1024f)
                        val mbTotal = downloadProgress.totalBytes / (1024f * 1024f)
                        if (downloadProgress.totalBytes > 0) {
                            String.format(Locale.US, "%.1f MB / %.1f MB", mbDownloaded, mbTotal)
                        } else {
                            String.format(Locale.US, "%.1f MB baixados", mbDownloaded)
                        }
                    }
                    else -> "Preparando download..."
                }

                Text(
                    text = subtitleText,
                    color = when {
                        downloadProgress.errorMessage != null -> Color(0xFFFCA5A5)
                        downloadProgress.isDownloadComplete -> Color(0xFF34D399)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Frame container enclosing the buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = brandColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (downloadProgress.isDownloading) {
                            var isFocused by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isFocused) 1.03f else 1f,
                                label = "cancel_scale"
                            )

                            Button(
                                onClick = onCancel,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFocused) Color(0xFFEF4444) else Color(0xFFDC2626),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .scale(scale)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .border(
                                        width = if (isFocused) 2.5.dp else 1.dp,
                                        color = if (isFocused) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .focusRequester(primaryFocusRequester)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cancelar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (downloadProgress.isDownloadComplete) {
                            var isDismissFocused by remember { mutableStateOf(false) }
                            val dismissScale by animateFloatAsState(
                                targetValue = if (isDismissFocused) 1.03f else 1f,
                                label = "dismiss_scale"
                            )

                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .scale(dismissScale)
                                    .onFocusChanged { isDismissFocused = it.isFocused }
                                    .border(
                                        width = if (isDismissFocused) 2.5.dp else 1.dp,
                                        color = if (isDismissFocused) Color.White else Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Text("Fechar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            var isInstallFocused by remember { mutableStateOf(false) }
                            val installScale by animateFloatAsState(
                                targetValue = if (isInstallFocused) 1.03f else 1f,
                                label = "install_scale"
                            )

                            Button(
                                onClick = onInstall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isInstallFocused) Color(0xFF059669) else Color(0xFF10B981),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(42.dp)
                                    .scale(installScale)
                                    .onFocusChanged { isInstallFocused = it.isFocused }
                                    .border(
                                        width = if (isInstallFocused) 2.5.dp else 1.dp,
                                        color = if (isInstallFocused) Color.White else Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .focusRequester(primaryFocusRequester)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InstallMobile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Instalar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (downloadProgress.errorMessage != null) {
                            var isCloseFocused by remember { mutableStateOf(false) }
                            val closeScale by animateFloatAsState(
                                targetValue = if (isCloseFocused) 1.03f else 1f,
                                label = "close_scale"
                            )

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCloseFocused) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .scale(closeScale)
                                    .onFocusChanged { isCloseFocused = it.isFocused }
                                    .border(
                                        width = if (isCloseFocused) 2.5.dp else 1.dp,
                                        color = if (isCloseFocused) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .focusRequester(primaryFocusRequester)
                            ) {
                                Text("Fechar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


