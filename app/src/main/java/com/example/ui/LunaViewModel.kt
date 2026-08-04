package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppManager
import com.example.data.BannerItem
import com.example.data.DockAppItem
import com.example.data.InstalledApp
import com.example.data.LunaApiService
import com.example.data.OtaVersionInfo
import com.example.data.SystemMonitor
import com.example.data.SystemStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class LauncherSection {
    MAIN_DOCK,
    APPS_DRAWER
}

data class AppDownloadProgress(
    val title: String = "",
    val packageName: String = "",
    val downloadUrl: String = "",
    val isOtaUpdate: Boolean = false,
    val isVisible: Boolean = false,
    val isDownloading: Boolean = false,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedFile: java.io.File? = null,
    val isDownloadComplete: Boolean = false,
    val isInstalling: Boolean = false,
    val errorMessage: String? = null
)

class LunaViewModel(application: Application) : AndroidViewModel(application) {

    val appManager = AppManager(application)
    private val apiService = LunaApiService()
    val systemMonitor = SystemMonitor(application)

    val systemStatus: StateFlow<SystemStatus> = systemMonitor.status
    val installedApps: StateFlow<List<InstalledApp>> = appManager.installedApps

    private val _banners = MutableStateFlow<List<BannerItem>>(emptyList())
    val banners: StateFlow<List<BannerItem>> = _banners.asStateFlow()

    private val _currentBannerIndex = MutableStateFlow(0)
    val currentBannerIndex: StateFlow<Int> = _currentBannerIndex.asStateFlow()

    private val _dockApps = MutableStateFlow<List<DockAppItem>>(emptyList())
    val dockApps: StateFlow<List<DockAppItem>> = _dockApps.asStateFlow()

    private val _otaInfo = MutableStateFlow<OtaVersionInfo?>(null)
    val otaInfo: StateFlow<OtaVersionInfo?> = _otaInfo.asStateFlow()

    private val _showOtaDialog = MutableStateFlow(false)
    val showOtaDialog: StateFlow<Boolean> = _showOtaDialog.asStateFlow()

    private var isOtaDismissedForSession = false

    private val _appToUninstall = MutableStateFlow<InstalledApp?>(null)
    val appToUninstall: StateFlow<InstalledApp?> = _appToUninstall.asStateFlow()

    private val _showVoiceSearchDialog = MutableStateFlow(false)
    val showVoiceSearchDialog: StateFlow<Boolean> = _showVoiceSearchDialog.asStateFlow()

    private val _downloadState = MutableStateFlow(AppDownloadProgress())
    val downloadState: StateFlow<AppDownloadProgress> = _downloadState.asStateFlow()

    private val _showBootVideo = MutableStateFlow(checkHasBootVideo())
    val showBootVideo: StateFlow<Boolean> = _showBootVideo.asStateFlow()

    private fun checkHasBootVideo(): Boolean {
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("luna_launcher_prefs", android.content.Context.MODE_PRIVATE)
        val hasPlayed = prefs.getBoolean("boot_video_played", false)
        if (hasPlayed) {
            return false
        }
        val res1 = ctx.resources.getIdentifier("bootvideo", "raw", ctx.packageName)
        val res2 = ctx.resources.getIdentifier("welcome", "raw", ctx.packageName)
        return (res1 != 0 || res2 != 0)
    }

    fun dismissBootVideo() {
        _showBootVideo.value = false
        try {
            val ctx = getApplication<Application>()
            ctx.getSharedPreferences("luna_launcher_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("boot_video_played", true)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("LunaViewModel", "Error saving boot_video_played pref: ${e.message}")
        }
        checkPendingOtaDialog()
    }

    private fun checkPendingOtaDialog() {
        if (isOtaDismissedForSession) return
        val ota = _otaInfo.value ?: return
        val currentCode = com.example.LunaApplication.getAppVersionCode(getApplication())
        if (ota.versionCode > currentCode && !_showBootVideo.value && !_showOtaDialog.value && !_downloadState.value.isDownloading && !_downloadState.value.isDownloadComplete) {
            _showOtaDialog.value = true
        }
    }

    private var downloadJob: kotlinx.coroutines.Job? = null
    private val okHttpClient = com.example.data.NetworkClientHelper.okHttpClient

    private val _activeSection = MutableStateFlow(LauncherSection.MAIN_DOCK)
    val activeSection: StateFlow<LauncherSection> = _activeSection.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        systemMonitor.startMonitoring()
        loadData()
        startBannerCarouselTimer()
        startPeriodicSyncTimer()
        observeInstalledAppsForDownloadDismissal()
        observeBackgroundOtaUpdates()
    }

    private fun observeBackgroundOtaUpdates() {
        viewModelScope.launch {
            com.example.LunaApplication.otaUpdateFlow.collect { ota ->
                if (ota != null) {
                    val currentCode = com.example.LunaApplication.getAppVersionCode(getApplication())
                    if (ota.versionCode > currentCode) {
                        _otaInfo.value = ota
                        if (!isOtaDismissedForSession && !_showBootVideo.value && !_showOtaDialog.value && !_downloadState.value.isDownloading && !_downloadState.value.isDownloadComplete) {
                            _showOtaDialog.value = true
                        }
                    }
                }
            }
        }
    }

    private fun startPeriodicSyncTimer() {
        viewModelScope.launch {
            while (true) {
                delay(30000) // 30 seconds periodic check
                try {
                    syncAllRemoteData(isInitial = false)
                } catch (e: Exception) {
                    android.util.Log.e("LunaViewModel", "Periodic sync error: ${e.message}")
                }
            }
        }
    }

    private fun observeInstalledAppsForDownloadDismissal() {
        viewModelScope.launch {
            appManager.installedApps.collect { installedList ->
                val current = _downloadState.value
                if (current.isVisible && current.packageName.isNotEmpty()) {
                    val isNowInstalled = installedList.any { it.packageName == current.packageName }
                    if (isNowInstalled || current.isOtaUpdate) {
                        current.downloadedFile?.let { file ->
                            if (file.exists()) {
                                try { file.delete() } catch (_: Exception) {}
                            }
                        }
                        com.example.LunaApplication.cleanUpCachedApks(getApplication())
                        _downloadState.value = AppDownloadProgress()
                    }
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            syncAllRemoteData(isInitial = true)
            _isLoading.value = false
        }
    }

    private suspend fun syncAllRemoteData(isInitial: Boolean = false) {
        appManager.loadInstalledApps()

        // 1. Fetch remote banners (img.json)
        val remoteBanners = apiService.fetchBanners()
        if (remoteBanners.isNotEmpty()) {
            _banners.value = remoteBanners
        } else if (_banners.value.isEmpty()) {
            _banners.value = listOf(
                BannerItem(id = "fallback_1", imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1920&q=80"),
                BannerItem(id = "fallback_2", imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1920&q=80"),
                BannerItem(id = "fallback_3", imageUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=1920&q=80")
            )
        }

        // 2. Fetch dock apps (apps.json)
        val remoteDockApps = apiService.fetchDockApps()
        val rawDockApps = if (remoteDockApps.isNotEmpty()) {
            remoteDockApps
        } else if (_dockApps.value.isEmpty()) {
            listOf(
                DockAppItem(id = "unitv", title = "UniTV", packageName = "com.unitv.app"),
                DockAppItem(id = "dock_1", title = "YouTube", packageName = "com.google.android.youtube.tv"),
                DockAppItem(id = "dock_2", title = "Netflix", packageName = "com.netflix.ninja"),
                DockAppItem(id = "dock_3", title = "Prime Video", packageName = "com.amazon.amazonvideo.livingroom")
            )
        } else null

        if (rawDockApps != null) {
            val context = getApplication<Application>()
            val enrichedDockApps = rawDockApps.map { item ->
                val cleanId = item.id.lowercase().trim()
                val cleanTitle = item.title.lowercase().trim()
                val cleanPkg = item.packageName.lowercase().trim()

                val matchedName = when {
                    cleanId == "unitv" || cleanTitle.contains("unitv") -> "unitv"
                    cleanId.contains("youtube") || cleanTitle.contains("youtube") || cleanPkg.contains("youtube") -> "youtube"
                    cleanId.contains("netflix") || cleanTitle.contains("netflix") || cleanPkg.contains("netflix") -> "netflix"
                    cleanId.contains("prime") || cleanTitle.contains("prime") || cleanPkg.contains("amazon") -> "prime"
                    cleanId.contains("disney") || cleanTitle.contains("disney") || cleanPkg.contains("disney") -> "disney"
                    cleanId.contains("play") || cleanTitle.contains("play") || cleanPkg.contains("vending") -> "playstore"
                    else -> cleanId
                }

                val resId = if (matchedName.isNotEmpty()) {
                    context.resources.getIdentifier(matchedName, "drawable", context.packageName)
                } else 0

                val localDrawable = if (resId != 0) {
                    try {
                        androidx.core.content.ContextCompat.getDrawable(context, resId)
                    } catch (_: Exception) { null }
                } else null

                val realIcon = if (item.packageName.isNotEmpty()) {
                    appManager.getAppIconDrawable(item.packageName)
                } else null

                item.copy(iconDrawable = localDrawable ?: realIcon)
            }
            _dockApps.value = enrichedDockApps
        }

        // 3. Check OTA updates (version.json)
        val versionInfo = apiService.fetchOtaVersion()
        if (versionInfo != null) {
            _otaInfo.value = versionInfo
            val currentCode = com.example.LunaApplication.getAppVersionCode(getApplication())
            if (versionInfo.versionCode > currentCode && !isOtaDismissedForSession && !_showBootVideo.value && !_showOtaDialog.value && !_downloadState.value.isDownloading) {
                _showOtaDialog.value = true
            }
        }
    }

    private fun startBannerCarouselTimer() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // Rotate banner every 10 seconds
                val list = _banners.value
                if (list.isNotEmpty()) {
                    _currentBannerIndex.value = (_currentBannerIndex.value + 1) % list.size
                }
            }
        }
    }

    fun openAppsDrawer() {
        _activeSection.value = LauncherSection.APPS_DRAWER
    }

    fun closeAppsDrawer() {
        _activeSection.value = LauncherSection.MAIN_DOCK
    }

    fun toggleAppsDrawer() {
        if (_activeSection.value == LauncherSection.APPS_DRAWER) {
            _activeSection.value = LauncherSection.MAIN_DOCK
        } else {
            _activeSection.value = LauncherSection.APPS_DRAWER
        }
    }

    fun onVoiceSearchClick() {
        val launchedExternal = appManager.launchVoiceSearch()
        if (!launchedExternal) {
            _showVoiceSearchDialog.value = true
        }
    }

    fun dismissVoiceSearchDialog() {
        _showVoiceSearchDialog.value = false
    }

    fun performSearch(query: String) {
        appManager.performWebSearch(query)
    }

    fun onDockAppClick(app: DockAppItem) {
        if (app.packageName.isNotEmpty() && appManager.isAppInstalled(app.packageName)) {
            appManager.launchApp(app.packageName)
        } else {
            val rawUrl = app.actionUrl.trim()
            val url = when {
                rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                rawUrl.startsWith("/") -> "https://itdoctorbrasil.site$rawUrl"
                rawUrl.isNotEmpty() -> "https://itdoctorbrasil.site/Launchers/Luna/$rawUrl"
                app.packageName.isNotEmpty() -> "https://itdoctorbrasil.site/Launchers/Luna/apks/${app.packageName}.apk"
                else -> "https://itdoctorbrasil.site/Launchers/Luna/apks/${app.id}.apk"
            }
            startDownload(
                title = app.title,
                packageName = app.packageName,
                downloadUrl = url,
                isOta = false
            )
        }
    }

    fun checkIfOtaAlreadyDownloaded(): java.io.File? {
        val ota = _otaInfo.value ?: return null
        val currentCode = com.example.LunaApplication.getAppVersionCode(getApplication())
        if (ota.versionCode <= currentCode) {
            com.example.LunaApplication.cleanUpCachedApks(getApplication())
            return null
        }
        val downloadDir = java.io.File(getApplication<Application>().cacheDir, "apks")
        val candidateNames = listOf(
            "update_ota_${ota.versionCode}.apk",
            "LunaLauncher-v${ota.versionName}.apk",
            "LunaLauncher.apk",
            "update.apk"
        )
        val pm = getApplication<Application>().packageManager
        for (name in candidateNames) {
            val file = java.io.File(downloadDir, name)
            if (file.exists() && file.length() > 10240) {
                val pkgInfo = try {
                    pm.getPackageArchiveInfo(file.absolutePath, 0)
                } catch (_: Exception) { null }
                if (pkgInfo != null) {
                    val archiveVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pkgInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo.versionCode
                    }
                    if (archiveVersion <= currentCode) {
                        file.delete()
                    } else {
                        return file
                    }
                } else {
                    file.delete()
                }
            }
        }
        return null
    }

    fun startOtaDownload() {
        val ota = _otaInfo.value
        _showOtaDialog.value = false
        val existingFile = checkIfOtaAlreadyDownloaded()
        val ver = ota?.versionName?.ifBlank { "1.0.1" } ?: "1.0.1"
        if (existingFile != null) {
            _downloadState.value = AppDownloadProgress(
                title = "Atualização de Sistema v$ver",
                packageName = getApplication<Application>().packageName,
                downloadUrl = ota?.url ?: "",
                isOtaUpdate = true,
                isVisible = true,
                isDownloading = false,
                isDownloadComplete = true,
                progressPercent = 100,
                downloadedBytes = existingFile.length(),
                totalBytes = existingFile.length(),
                downloadedFile = existingFile
            )
            return
        }
        val url = ota?.url?.trim() ?: ""
        startDownload(
            title = "Atualização de Sistema v$ver",
            packageName = getApplication<Application>().packageName,
            downloadUrl = url,
            isOta = true
        )
    }

    fun startDownload(title: String, packageName: String, downloadUrl: String, isOta: Boolean = false) {
        downloadJob?.cancel()

        val fileName = if (isOta) {
            val otaVer = _otaInfo.value?.versionCode ?: "latest"
            "update_ota_$otaVer.apk"
        } else {
            "${packageName.ifEmpty { "app" }}.apk"
        }

        val downloadDir = java.io.File(getApplication<Application>().cacheDir, "apks")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        val outputFile = java.io.File(downloadDir, fileName)

        // Pre-check: Verify if APK is already downloaded and valid
        if (outputFile.exists() && outputFile.length() > 10240) {
            val pm = getApplication<Application>().packageManager
            val pkgInfo = try {
                pm.getPackageArchiveInfo(outputFile.absolutePath, 0)
            } catch (e: Exception) {
                null
            }

            if (pkgInfo != null) {
                _downloadState.value = AppDownloadProgress(
                    title = title,
                    packageName = packageName,
                    downloadUrl = downloadUrl,
                    isOtaUpdate = isOta,
                    isVisible = true,
                    isDownloading = false,
                    isDownloadComplete = true,
                    progressPercent = 100,
                    downloadedBytes = outputFile.length(),
                    totalBytes = outputFile.length(),
                    downloadedFile = outputFile
                )
                return
            } else {
                outputFile.delete()
            }
        }

        _downloadState.value = AppDownloadProgress(
            title = title,
            packageName = packageName,
            downloadUrl = downloadUrl,
            isOtaUpdate = isOta,
            isVisible = true,
            isDownloading = true,
            progressPercent = 0,
            downloadedBytes = 0,
            totalBytes = 0,
            downloadedFile = outputFile
        )

        downloadJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val scope = this
            var success = false
            var lastErrorCode = 0

            val candidateUrls = mutableListOf<String>()
            if (downloadUrl.isNotBlank()) {
                candidateUrls.add(downloadUrl)
            }

            if (isOta) {
                // For OTA, the download URL is retrieved dynamically from version.json
                if (candidateUrls.isEmpty()) {
                    val freshOta = apiService.fetchOtaVersion()
                    if (freshOta != null && freshOta.url.isNotBlank()) {
                        candidateUrls.add(freshOta.url)
                    }
                }
            } else {
                val pkg = packageName.trim()
                val cleanTitle = title.lowercase().trim().replace(" ", "")
                if (pkg.isNotEmpty()) {
                    candidateUrls.add("https://itdoctorbrasil.site/Launchers/Luna/apks/$pkg.apk")
                    candidateUrls.add("https://itdoctorbrasil.site/Launchers/Luna/$pkg.apk")
                }
                if (cleanTitle.isNotEmpty()) {
                    candidateUrls.add("https://itdoctorbrasil.site/Launchers/Luna/apks/$cleanTitle.apk")
                    candidateUrls.add("https://itdoctorbrasil.site/Launchers/Luna/$cleanTitle.apk")
                }
            }

            for (url in candidateUrls) {
                if (!scope.isActive) return@launch
                try {
                    if (outputFile.exists()) outputFile.delete()
                    android.util.Log.d("LunaViewModel", "Attempting download from candidate URL: $url")

                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 7.0; TV) AppleWebKit/537.36")
                        .build()

                    val response = okHttpClient.newCall(request).execute()

                    if (!response.isSuccessful) {
                        lastErrorCode = response.code
                        response.close()
                        continue
                    }

                    val body = response.body
                    if (body == null) {
                        response.close()
                        continue
                    }

                    val totalBytes = body.contentLength()
                    var downloadedBytes = 0L

                    body.byteStream().use { input ->
                        outputFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!scope.isActive) {
                                    outputFile.delete()
                                    response.close()
                                    return@launch
                                }
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                val progress = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0

                                _downloadState.value = _downloadState.value.copy(
                                    progressPercent = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                            }
                        }
                    }
                    response.close()

                    if (outputFile.exists() && outputFile.length() > 10240) {
                        success = true
                        break
                    } else {
                        if (outputFile.exists()) outputFile.delete()
                    }
                } catch (e: Exception) {
                    if (outputFile.exists()) outputFile.delete()
                    android.util.Log.e("LunaViewModel", "Error downloading from $url: ${e.message}")
                }
            }

            if (!scope.isActive) return@launch

            if (success) {
                _downloadState.value = _downloadState.value.copy(
                    isDownloading = false,
                    isDownloadComplete = true,
                    progressPercent = 100,
                    downloadedFile = outputFile
                )
            } else {
                if (outputFile.exists()) outputFile.delete()
                _downloadState.value = _downloadState.value.copy(
                    isDownloading = false,
                    errorMessage = if (lastErrorCode != 0) "Erro HTTP ($lastErrorCode)" else "Falha no download. Verifique a conexão."
                )
            }
        }
    }

    fun cancelDownload() {
        val state = _downloadState.value
        val file = state.downloadedFile
        if (state.isDownloading && file != null && file.exists()) {
            file.delete()
        }
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = AppDownloadProgress()
    }

    fun installDownloadedApk() {
        val state = _downloadState.value
        val apkFile = state.downloadedFile ?: return
        _downloadState.value = state.copy(isInstalling = true)
        appManager.installApk(apkFile)
    }

    fun dismissDownloadDialog() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = AppDownloadProgress()
    }

    fun onBackPressed(): Boolean {
        if (_showBootVideo.value) {
            _showBootVideo.value = false
            return true
        }
        if (_downloadState.value.isVisible) {
            cancelDownload()
            return true
        }
        if (_showVoiceSearchDialog.value) {
            _showVoiceSearchDialog.value = false
            return true
        }
        if (_appToUninstall.value != null) {
            _appToUninstall.value = null
            return true
        }
        if (_showOtaDialog.value) {
            dismissOtaDialog()
            return true
        }
        if (_activeSection.value == LauncherSection.APPS_DRAWER) {
            _activeSection.value = LauncherSection.MAIN_DOCK
            return true
        }
        // Always return true to consume back key and prevent exiting launcher
        return true
    }

    fun setAppToUninstall(app: InstalledApp?) {
        _appToUninstall.value = app
    }

    fun confirmUninstall() {
        val app = _appToUninstall.value
        if (app != null) {
            appManager.uninstallApp(app.packageName)
            _appToUninstall.value = null
        }
    }

    fun dismissOtaDialog() {
        isOtaDismissedForSession = true
        _showOtaDialog.value = false
    }

    override fun onCleared() {
        super.onCleared()
        systemMonitor.stopMonitoring()
        appManager.unregisterPackageReceiver()
    }
}
