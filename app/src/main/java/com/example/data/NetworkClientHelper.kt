package com.example.data

import android.content.Context
import android.os.Build
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.net.NetworkInterface
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkClientHelper {

    private var appContext: Context? = null

    /**
     * Inicializa o contexto para leitura de SharedPreferences/Hardware
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Tenta ler o MAC Address real do dispositivo através de 3 camadas de segurança:
     * 1. Leitura direta de arquivos do sistema Linux (/sys/class/net/) - Ideal para Allwinner H3/Amlogic
     * 2. Leitura via interfaces de rede padrão (eth0 / wlan0)
     * 3. Fallback via Android ID único da placa
     */
    fun getDeviceMacAddress(): String {
        // 1. Leitura direta via sistema de arquivos Linux (Funciona sem bloqueio de API em TV Boxes)
        val sysPaths = listOf(
            "/sys/class/net/eth0/address",
            "/sys/class/net/wlan0/address"
        )
        for (path in sysPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val mac = file.readText().trim().uppercase()
                    if (mac.isNotEmpty() && mac != "02:00:00:00:00:00") {
                        android.util.Log.d("NetworkClientHelper", "MAC lido via sysfs Linux: $mac")
                        return mac
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkClientHelper", "Erro ao ler $path: ${e.message}")
            }
        }

        // 2. Leitura via NetworkInterface (Fallback padrão)
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces?.hasMoreElements() == true) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.name.equals("eth0", ignoreCase = true) ||
                    networkInterface.name.equals("wlan0", ignoreCase = true)) {

                    val macBytes = networkInterface.hardwareAddress ?: continue
                    val res = StringBuilder()
                    for (b in macBytes) {
                        res.append(String.format("%02X:", b))
                    }
                    if (res.isNotEmpty()) {
                        res.deleteCharAt(res.length - 1)
                    }
                    if (res.isNotBlank()) return res.toString()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkClientHelper", "Erro ao obter MAC via NetworkInterface: ${e.message}")
        }

        // 3. Fallback via Android ID único
        appContext?.let { ctx ->
            try {
                val androidId = android.provider.Settings.Secure.getString(
                    ctx.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                    return androidId.uppercase()
                }
            } catch (_: Exception) {}
        }

        return ""
    }

    val trustAllCerts: Array<TrustManager> = arrayOf(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )

    val sslSocketFactory: SSLSocketFactory by lazy {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        sslContext.socketFactory
    }

    val trustManager: X509TrustManager by lazy {
        trustAllCerts[0] as X509TrustManager
    }

    val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            // Interceptor que injeta automaticamente o parâmetro "token" com o MAC/Token da Box
            .addInterceptor { chain ->
                val original = chain.request()
                val originalUrl = original.url

                // Tenta buscar o token das configurações locais ou pega o MAC real do hardware
                val prefs = appContext?.getSharedPreferences("luna_prefs", Context.MODE_PRIVATE)
                val savedToken = prefs?.getString("custom_token", "") ?: ""
                val deviceMac = getDeviceMacAddress()

                // Prioriza o Token salvo; caso esteja vazio, usa o MAC Address do dispositivo
                val activeIdentifier = if (savedToken.isNotEmpty()) savedToken else deviceMac

                // Se a URL ainda não tiver o parâmetro "token" ou "mac", anexa automaticamente
                val newUrlBuilder = originalUrl.newBuilder()
                if (originalUrl.queryParameter("token") == null && originalUrl.queryParameter("mac") == null && activeIdentifier.isNotEmpty()) {
                    newUrlBuilder.addQueryParameter("token", activeIdentifier)
                }

                val request = original.newBuilder()
                    .url(newUrlBuilder.build())
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 7.0; TV Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .header("Cache-Control", "no-cache, no-store")
                    .build()

                chain.proceed(request)
            }

        try {
            builder.sslSocketFactory(sslSocketFactory, trustManager)
            builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
        } catch (e: Exception) {
            android.util.Log.e("NetworkClientHelper", "Error configuring SSL: ${e.message}")
        }

        builder.build()
    }

    fun configureHttpURLConnection(connection: java.net.HttpURLConnection) {
        if (connection is javax.net.ssl.HttpsURLConnection) {
            try {
                connection.sslSocketFactory = sslSocketFactory
                connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
            } catch (_: Exception) {}
        }
    }
}