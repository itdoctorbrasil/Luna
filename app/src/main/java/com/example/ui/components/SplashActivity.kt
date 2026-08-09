package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.MainActivity
import com.example.R

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Infla o layout PRIMEIRO para garantir que a DecorView e a janela existem
        setContentView(R.layout.activity_splash)

        // 2. Oculta a interface do sistema com segurança após carregar a View
        hideSystemUI()

        val videoView = findViewById<VideoView>(R.id.videoView)

        // Aponta para res/raw/bootvideo.mp4
        val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.bootvideo)
        videoView.setVideoURI(videoUri)

        // Quando o vídeo de boot terminar, vai para a MainActivity
        videoView.setOnCompletionListener {
            navigateToMain()
        }

        // Caso ocorra falha de reprodução, abre a MainActivity sem travar o app
        videoView.setOnErrorListener { _, _, _ ->
            navigateToMain()
            true
        }

        videoView.start()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finaliza a SplashActivity para não retornar a ela no botão Voltar
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Utilização de WindowCompat para evitar aceso direto a referências nulas do DecorView
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }
}