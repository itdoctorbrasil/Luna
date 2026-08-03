package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AppDownloadProgress
import java.util.Locale

@Composable
fun AppDownloadDialog(
    downloadProgress: AppDownloadProgress,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!downloadProgress.isVisible) return

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
                .width(460.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF818CF8)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(28.dp)
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
                        text = if (downloadProgress.isOtaUpdate) "Atualização do Sistema" else "Download de Aplicativo",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!downloadProgress.isDownloading) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Status Icon Box
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                downloadProgress.errorMessage != null -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                downloadProgress.isDownloadComplete -> Color(0xFF10B981).copy(alpha = 0.2f)
                                else -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                downloadProgress.errorMessage != null -> Color(0xFFEF4444)
                                downloadProgress.isDownloadComplete -> Color(0xFF10B981)
                                else -> Color(0xFF38BDF8)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            downloadProgress.errorMessage != null -> Icons.Default.ErrorOutline
                            downloadProgress.isDownloadComplete -> Icons.Default.CheckCircle
                            else -> Icons.Default.Download
                        },
                        contentDescription = "Status",
                        tint = when {
                            downloadProgress.errorMessage != null -> Color(0xFFEF4444)
                            downloadProgress.isDownloadComplete -> Color(0xFF10B981)
                            else -> Color(0xFF38BDF8)
                        },
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App/Update Title
                Text(
                    text = downloadProgress.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle / Progress Info
                val subtitleText = when {
                    downloadProgress.errorMessage != null -> downloadProgress.errorMessage
                    downloadProgress.isInstalling -> "Abrindo instalador do Android..."
                    downloadProgress.isDownloadComplete -> "Download concluído! Clique em Instalar."
                    downloadProgress.isDownloading -> {
                        val mbDownloaded = downloadProgress.downloadedBytes / (1024f * 1024f)
                        val mbTotal = downloadProgress.totalBytes / (1024f * 1024f)
                        if (downloadProgress.totalBytes > 0) {
                            String.format(Locale.US, "%.1f MB de %.1f MB (%d%%)", mbDownloaded, mbTotal, downloadProgress.progressPercent)
                        } else {
                            String.format(Locale.US, "%.1f MB baixados", mbDownloaded)
                        }
                    }
                    else -> "Preparando download..."
                }

                Text(
                    text = subtitleText,
                    color = when {
                        downloadProgress.errorMessage != null -> Color(0xFFEF4444)
                        downloadProgress.isDownloadComplete -> Color(0xFF10B981)
                        else -> Color.White.copy(alpha = 0.75f)
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Bar
                if (downloadProgress.isDownloading || downloadProgress.isDownloadComplete) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { downloadProgress.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (downloadProgress.isDownloadComplete) Color(0xFF10B981) else Color(0xFF38BDF8),
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (downloadProgress.isDownloading) {
                        // Cancel button during active download
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cancelar Download",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (downloadProgress.isDownloadComplete) {
                        // Install Button + Dismiss Button
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Fechar", fontSize = 15.sp)
                        }

                        Button(
                            onClick = onInstall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InstallMobile,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Instalar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (downloadProgress.errorMessage != null) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Fechar", fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
