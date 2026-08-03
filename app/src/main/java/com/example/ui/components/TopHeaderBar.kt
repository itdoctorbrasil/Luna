package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SystemStatus

@Composable
fun TopHeaderBar(
    status: SystemStatus,
    onVoiceSearchClick: () -> Unit,
    onWifiClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Weather info in a frosted glass badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = "Clima",
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = status.weatherTemp,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status.weatherLocation,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Center: Premium Time Clock
        Text(
            text = status.currentTimeStr.ifEmpty { "12:00" },
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            modifier = Modifier.testTag("clock_text")
        )

        // Right: Premium System status icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                icon = Icons.Default.Mic,
                contentDescription = "Busca por Voz",
                onClick = onVoiceSearchClick,
                testTag = "btn_voice_search"
            )

            HeaderIconButton(
                icon = if (status.isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = "Wi-Fi",
                onClick = onWifiClick,
                testTag = "btn_wifi"
            )

            HeaderIconButton(
                icon = if (status.isBluetoothEnabled) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = "Bluetooth",
                onClick = onBluetoothClick,
                testTag = "btn_bluetooth"
            )

            HeaderIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Configurações",
                onClick = onSettingsClick,
                testTag = "btn_settings"
            )
        }
    }
}

@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.18f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "icon_scale"
    )

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .size(42.dp)
            .clip(shape)
            .background(
                brush = if (isFocused) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF818CF8)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f),
                shape = shape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp)
        )
    }
}
