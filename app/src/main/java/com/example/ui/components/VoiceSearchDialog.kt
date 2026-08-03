package com.example.ui.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun VoiceSearchDialog(
    isVisible: Boolean,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var speechText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Toque no microfone ou fale...") }
    var typedQuery by remember { mutableStateOf("") }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            statusText = "Pesquisa por voz embutida ativa. Digite ou use a busca."
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale algo para pesquisar...")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                statusText = "Ouvindo... Fale agora"
            }

            override fun onBeginningOfSpeech() {
                statusText = "Processando sua voz..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                statusText = "Analisando..."
            }

            override fun onError(error: Int) {
                isListening = false
                statusText = "Não entendi bem. Tente falar novamente ou digite abaixo."
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val result = matches[0]
                    speechText = result
                    typedQuery = result
                    statusText = "Encontrado: $result"
                    onSearch(result)
                    onDismiss()
                } else {
                    statusText = "Nenhum resultado encontrado."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    speechText = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer.startListening(intent)
            isListening = true
            statusText = "Ouvindo... Fale agora"
        } catch (e: Exception) {
            isListening = false
            statusText = "Digite sua busca abaixo:"
        }
    }

    LaunchedEffect(Unit) {
        startListening()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Busca por Voz",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Glowing Mic Button
                Box(
                    modifier = Modifier
                        .scale(if (isListening) micScale else 1.0f)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isListening) listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF3B82F6)
                                ) else listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.4f),
                                    Color(0xFF1E293B)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = if (isListening) Color(0xFF00E5FF) else Color(0xFF38BDF8).copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { startListening() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microfone",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (speechText.isNotEmpty()) speechText else statusText,
                    color = if (isListening) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Text Search Input Fallback
                OutlinedTextField(
                    value = typedQuery,
                    onValueChange = { typedQuery = it },
                    placeholder = {
                        Text(
                            text = "Ou digite o que procura...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (typedQuery.isNotBlank()) {
                                onSearch(typedQuery)
                                onDismiss()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Pesquisar",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (typedQuery.isNotBlank()) {
                            onSearch(typedQuery)
                            onDismiss()
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Search Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickSearchChip(text = "YouTube") {
                        onSearch("YouTube")
                        onDismiss()
                    }
                    QuickSearchChip(text = "Google") {
                        onSearch("Google")
                        onDismiss()
                    }
                    QuickSearchChip(text = "Configurações") {
                        onSearch("Configurações")
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
fun QuickSearchChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
