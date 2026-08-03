package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.OtaVersionInfo

@Composable
fun OtaUpdateDialog(
    otaInfo: OtaVersionInfo?,
    onStartUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    if (otaInfo == null) return

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .background(Color(0xFF0F172A), shape = RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Atualização OTA",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Atualização de Sistema Luna",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Versão ${otaInfo.versionName} (Build ${otaInfo.versionCode})",
                color = Color(0xFF00E5FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (otaInfo.changelog.isNotEmpty()) {
                Text(
                    text = otaInfo.changelog,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!otaInfo.forceUpdate) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_dismiss_ota")
                    ) {
                        Text("Depois", color = Color.White)
                    }
                }

                Button(
                    onClick = onStartUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_update_ota")
                ) {
                    Text("Atualizar Agora", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
