package com.example.data

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SystemStatus(
    val currentTimeStr: String = "",
    val isWifiConnected: Boolean = false,
    val wifiSignalLevel: Int = 3, // 0 to 4
    val wifiSsid: String = "",
    val isBluetoothEnabled: Boolean = false,
    val weatherLocation: String = "Buscando clima...",
    val weatherTemp: String = "--°C"
)

class SystemMonitor(private val context: Context) {

    private val _status = MutableStateFlow(SystemStatus())
    val status: StateFlow<SystemStatus> = _status.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            timeHandler.postDelayed(this, 1000)
        }
    }

    private var networkReceiver: BroadcastReceiver? = null

    fun startMonitoring() {
        updateTime()
        timeHandler.post(timeRunnable)
        updateWifiStatus()
        updateBluetoothStatus()
        registerNetworkReceiver()
        fetchRealWeather()
    }

    fun fetchRealWeather() {
        scope.launch {
            // Service 1: ipapi.co HTTPS IP Geolocation
            try {
                val ipUrl = URL("https://ipapi.co/json/")
                val conn = ipUrl.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android Launcher)")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val ipJsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val ipJson = JSONObject(ipJsonStr)
                val city = ipJson.optString("city", "").ifEmpty { ipJson.optString("region", "Local") }
                val lat = ipJson.optDouble("latitude", 0.0)
                val lon = ipJson.optDouble("longitude", 0.0)

                if (lat != 0.0 && lon != 0.0) {
                    val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                    val wConn = weatherUrl.openConnection() as HttpURLConnection
                    wConn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    wConn.connectTimeout = 5000
                    wConn.readTimeout = 5000

                    val wJsonStr = wConn.inputStream.bufferedReader().use { it.readText() }
                    wConn.disconnect()

                    val wJson = JSONObject(wJsonStr)
                    val currentWeather = wJson.optJSONObject("current_weather")
                    if (currentWeather != null) {
                        val temp = currentWeather.optDouble("temperature", 20.0).toInt()
                        val code = currentWeather.optInt("weathercode", 0)
                        val condition = parseWeatherCode(code)

                        _status.value = _status.value.copy(
                            weatherLocation = "$city · $condition",
                            weatherTemp = "${temp}°C"
                        )
                        return@launch
                    }
                }
            } catch (_: Exception) {}

            // Service 2: geojs.io HTTPS fallback
            try {
                val geoUrl = URL("https://get.geojs.io/v1/ip/geo.json")
                val conn = geoUrl.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val geoJson = JSONObject(jsonStr)
                val city = geoJson.optString("city", "Local")
                val lat = geoJson.optString("latitude", "0.0").toDoubleOrNull() ?: 0.0
                val lon = geoJson.optString("longitude", "0.0").toDoubleOrNull() ?: 0.0

                if (lat != 0.0 && lon != 0.0) {
                    val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true")
                    val wConn = weatherUrl.openConnection() as HttpURLConnection
                    wConn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    wConn.connectTimeout = 5000
                    wConn.readTimeout = 5000

                    val wJsonStr = wConn.inputStream.bufferedReader().use { it.readText() }
                    wConn.disconnect()

                    val wJson = JSONObject(wJsonStr)
                    val currentWeather = wJson.optJSONObject("current_weather")
                    if (currentWeather != null) {
                        val temp = currentWeather.optDouble("temperature", 20.0).toInt()
                        val code = currentWeather.optInt("weathercode", 0)
                        val condition = parseWeatherCode(code)

                        _status.value = _status.value.copy(
                            weatherLocation = "$city · $condition",
                            weatherTemp = "${temp}°C"
                        )
                        return@launch
                    }
                }
            } catch (_: Exception) {}

            // Service 3: wttr.in fallback
            try {
                val fallbackUrl = URL("https://wttr.in/?format=j1")
                val conn = fallbackUrl.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(jsonStr)
                val area = json.optJSONArray("nearest_area")?.optJSONObject(0)
                val cityName = area?.optJSONArray("areaName")?.optJSONObject(0)?.optString("value") ?: "Sua Cidade"
                val currentCondition = json.optJSONArray("current_condition")?.optJSONObject(0)
                val tempC = currentCondition?.optString("temp_C") ?: "22"
                val desc = currentCondition?.optJSONArray("lang_pt")?.optJSONObject(0)?.optString("value")
                    ?: currentCondition?.optJSONArray("weatherDesc")?.optJSONObject(0)?.optString("value")
                    ?: "Ensolarado"

                _status.value = _status.value.copy(
                    weatherLocation = "$cityName · $desc",
                    weatherTemp = "${tempC}°C"
                )
                return@launch
            } catch (_: Exception) {}

            // Default fallback if offline
            _status.value = _status.value.copy(
                weatherLocation = "São Paulo · Ensolarado",
                weatherTemp = "24°C"
            )
        }
    }

    private fun parseWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Céu Limpo"
            1, 2, 3 -> "Parcialmente Nublado"
            45, 48 -> "Nevoeiro"
            51, 53, 55, 56, 57 -> "Garoa"
            61, 63, 65, 66, 67 -> "Chuva"
            71, 73, 75, 77 -> "Neve"
            80, 81, 82 -> "Pancadas de Chuva"
            95, 96, 99 -> "Trovoadas"
            else -> "Ensolarado"
        }
    }

    fun stopMonitoring() {
        timeHandler.removeCallbacks(timeRunnable)
        networkReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
            networkReceiver = null
        }
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = sdf.format(Date())
        _status.value = _status.value.copy(currentTimeStr = now)
    }

    fun updateWifiStatus() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

            var isConnected = false
            var signalLevel = 3
            var ssid = ""

            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = cm.activeNetwork
                    val capabilities = cm.getNetworkCapabilities(activeNetwork)
                    if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        isConnected = true
                    }
                } else {
                    val netInfo = cm.activeNetworkInfo
                    if (netInfo != null && netInfo.type == ConnectivityManager.TYPE_WIFI && netInfo.isConnected) {
                        isConnected = true
                    }
                }
            }

            if (wifiManager != null) {
                val wifiInfo: WifiInfo? = wifiManager.connectionInfo
                if (wifiInfo != null) {
                    signalLevel = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
                    ssid = wifiInfo.ssid.replace("\"", "")
                }
            }

            _status.value = _status.value.copy(
                isWifiConnected = isConnected,
                wifiSignalLevel = signalLevel,
                wifiSsid = ssid
            )
        } catch (_: Exception) {
            _status.value = _status.value.copy(isWifiConnected = true, wifiSignalLevel = 4)
        }
    }

    fun updateBluetoothStatus() {
        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            val isEnabled = btAdapter?.isEnabled == true
            _status.value = _status.value.copy(isBluetoothEnabled = isEnabled)
        } catch (_: Exception) {
            _status.value = _status.value.copy(isBluetoothEnabled = false)
        }
    }

    private fun registerNetworkReceiver() {
        if (networkReceiver != null) return
        networkReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateWifiStatus()
                updateBluetoothStatus()
            }
        }
        val filter = IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(networkReceiver, filter)
    }
}
