package com.example.data

import android.content.Context
import android.os.Build
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkClientHelper {

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
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 7.0; TV Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
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
