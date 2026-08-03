package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppManager(private val context: Context) {

    companion object {
        private const val TAG = "AppManager"
    }

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private var packageReceiver: BroadcastReceiver? = null

    init {
        registerPackageReceiver()
    }

    fun getAppIconDrawable(packageName: String): android.graphics.drawable.Drawable? {
        if (packageName.isEmpty()) return null
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            }

            val launcherApps = pm.queryIntentActivities(mainIntent, 0)
            val leanbackApps = pm.queryIntentActivities(leanbackIntent, 0)

            val combined = (launcherApps + leanbackApps).distinctBy { it.activityInfo.packageName }

            val appList = mutableListOf<InstalledApp>()
            val ownPackageName = context.packageName

            for (resolveInfo in combined) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == ownPackageName) continue

                try {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    appList.add(
                        InstalledApp(
                            title = label,
                            packageName = pkgName,
                            icon = icon,
                            isSystemApp = isSystem
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading app info for $pkgName: ${e.message}")
                }
            }

            appList.sortBy { it.title.lowercase() }
            _installedApps.value = appList
        } catch (e: Exception) {
            Log.e(TAG, "Error loading installed apps: ${e.message}", e)
        }
    }

    private fun registerPackageReceiver() {
        if (packageReceiver != null) return

        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == Intent.ACTION_PACKAGE_ADDED ||
                    action == Intent.ACTION_PACKAGE_REMOVED ||
                    action == Intent.ACTION_PACKAGE_CHANGED ||
                    action == Intent.ACTION_PACKAGE_REPLACED
                ) {
                    kotlinx.coroutines.MainScope().launch {
                        loadInstalledApps()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(packageReceiver, filter)
        }
    }

    fun unregisterPackageReceiver() {
        packageReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister package receiver: ${e.message}")
            }
            packageReceiver = null
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun installApk(apkFile: java.io.File): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri: Uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching APK installer: ${e.message}", e)
            false
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
                ?: pm.getLeanbackLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app $packageName: ${e.message}")
            false
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering uninstall for $packageName: ${e.message}")
        }
    }

    fun openSettings() {
        val tvSettingsPkg = "com.android.tv.settings"
        val standardSettingsPkg = "com.android.settings"

        if (launchApp(tvSettingsPkg)) return
        if (launchApp(standardSettingsPkg)) return

        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings: ${e.message}")
        }
    }

    fun openWifiSettings() {
        val tvSettingsPkg = "com.android.tv.settings"
        val standardSettingsPkg = "com.android.settings"

        if (isAppInstalled(tvSettingsPkg) && launchApp(tvSettingsPkg)) return
        if (isAppInstalled(standardSettingsPkg) && launchApp(standardSettingsPkg)) return

        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openSettings()
        }
    }

    fun openBluetoothSettings() {
        val tvSettingsPkg = "com.android.tv.settings"
        val standardSettingsPkg = "com.android.settings"

        if (isAppInstalled(tvSettingsPkg) && launchApp(tvSettingsPkg)) return
        if (isAppInstalled(standardSettingsPkg) && launchApp(standardSettingsPkg)) return

        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openSettings()
        }
    }

    fun launchVoiceSearch(): Boolean {
        val intentsToTry = listOf(
            Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            },
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            },
            Intent("android.intent.action.VOICE_COMMAND"),
            Intent("android.intent.action.VOICE_ASSIST"),
            Intent("android.speech.action.RECOGNIZE_SPEECH")
        )

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                if (resolveInfo != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error trying intent ${intent.action}: ${e.message}")
            }
        }
        return false
    }

    fun performWebSearch(query: String) {
        if (query.isBlank()) return
        
        // First check if query matches an installed app
        val matchedApp = _installedApps.value.find { 
            it.title.equals(query.trim(), ignoreCase = true) ||
            it.title.lowercase().contains(query.trim().lowercase())
        }
        
        if (matchedApp != null) {
            launchApp(matchedApp.packageName)
            return
        }

        try {
            val encodedQuery = Uri.encode(query)
            val searchUri = Uri.parse("https://www.google.com/search?q=$encodedQuery")
            val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing web search: ${e.message}")
        }
    }
}
