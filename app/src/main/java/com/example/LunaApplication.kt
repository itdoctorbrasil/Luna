package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.LunaApiService
import com.example.data.OtaVersionInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LunaApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val apiService = LunaApiService()

    companion object {
        private const val TAG = "LunaApplication"
        private val _otaUpdateFlow = MutableStateFlow<OtaVersionInfo?>(null)
        val otaUpdateFlow: StateFlow<OtaVersionInfo?> = _otaUpdateFlow.asStateFlow()

        fun getAppVersionCode(context: Context): Int {
            return try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode
                }
            } catch (_: Exception) {
                1
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startBackgroundOtaChecker()
    }

    private fun startBackgroundOtaChecker() {
        applicationScope.launch {
            while (isActive) {
                try {
                    val otaInfo = apiService.fetchOtaVersion()
                    if (otaInfo != null) {
                        val currentCode = getAppVersionCode(this@LunaApplication)
                        if (otaInfo.versionCode > currentCode) {
                            Log.d(TAG, "New OTA Update detected in background check: v${otaInfo.versionName} (code ${otaInfo.versionCode})")
                            _otaUpdateFlow.value = otaInfo
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Background OTA check error: ${e.message}")
                }
                // Check every 30 seconds even when in background
                delay(30_000)
            }
        }
    }
}
