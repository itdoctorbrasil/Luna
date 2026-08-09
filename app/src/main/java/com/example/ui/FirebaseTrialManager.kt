package com.example.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class FirebaseTrialManager(private val context: Context) {

    val deviceId: String by lazy {
        try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
        } catch (e: Exception) {
            "UNKNOWN_DEVICE"
        }
    }

    private val apiBaseUrl = "https://painel-ayxi.onrender.com"

    interface TrialCallback {
        fun onTrialActive()
        fun onTrialExpired()
        fun onError(e: Exception)
    }

    fun checkTrialStatus(callback: TrialCallback) {
        val mainHandler = Handler(Looper.getMainLooper())

        thread {
            var connection: HttpURLConnection? = null
            try {
                val urlString = "$apiBaseUrl/api/v1/launcher/config?token=$deviceId"
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonResponse = JSONObject(response.toString())

                    // Se existir o objeto client, valida o status de expiração
                    val clientObject = jsonResponse.optJSONObject("client")
                    val isActive = clientObject?.optBoolean("isActive", true) ?: true

                    mainHandler.post {
                        if (isActive) {
                            callback.onTrialActive()
                        } else {
                            callback.onTrialExpired()
                        }
                    }
                } else {
                    // FALLBACK: Se o dispositivo não for encontrado (404) ou der outro código HTTP,
                    // libera o app para evitar que o Launcher entre em crash/looping.
                    mainHandler.post {
                        callback.onTrialActive()
                    }
                }

            } catch (e: Exception) {
                // FALLBACK: Em falhas de rede/timeout, autoriza a abertura normal do app
                mainHandler.post {
                    callback.onTrialActive()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }
}