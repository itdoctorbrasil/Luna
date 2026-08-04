package com.example.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

object SecurityUtils {

    fun getDomainUrl(): String = "https://itdoctorbrasil.site"
    fun getLunaBaseUrl(): String = "https://itdoctorbrasil.site/Launchers/Luna/"

    fun isDebuggerAttached(): Boolean = false

    fun isEmulatorOrTampered(): Boolean = false

    fun getApplicationInfoReflection(context: Context, packageName: String): ApplicationInfo? {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
        } catch (_: Exception) {
            null
        }
    }

    fun getLaunchIntentReflection(context: Context, packageName: String): android.content.Intent? {
        return try {
            val pm = context.packageManager
            pm.getLaunchIntentForPackage(packageName)
                ?: pm.getLeanbackLaunchIntentForPackage(packageName)
        } catch (_: Exception) {
            null
        }
    }
}


