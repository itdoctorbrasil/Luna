package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import com.example.ui.components.AppDownloadDialog
import com.example.ui.components.AppUninstallDialog
import com.example.ui.components.AppsGridDrawer
import com.example.ui.components.BackgroundCarousel
import com.example.ui.components.MainDock
import com.example.ui.components.OtaUpdateDialog
import com.example.ui.components.TopHeaderBar
import com.example.ui.components.VoiceSearchDialog

@Composable
fun LunaLauncherScreen(
    viewModel: LunaViewModel,
    modifier: Modifier = Modifier
) {
    val banners by viewModel.banners.collectAsState()
    val currentBannerIndex by viewModel.currentBannerIndex.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    val activeSection by viewModel.activeSection.collectAsState()
    val showOtaDialog by viewModel.showOtaDialog.collectAsState()
    val otaInfo by viewModel.otaInfo.collectAsState()
    val appToUninstall by viewModel.appToUninstall.collectAsState()
    val showVoiceSearchDialog by viewModel.showVoiceSearchDialog.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    val installedPackageSet = remember(installedApps) {
        installedApps.map { it.packageName }.toSet()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Full screen Apple TV style background carousel
        BackgroundCarousel(
            banners = banners,
            currentIndex = currentBannerIndex
        )

        // Main Launcher UI content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header Bar
            TopHeaderBar(
                status = systemStatus,
                onVoiceSearchClick = { viewModel.onVoiceSearchClick() },
                onWifiClick = { viewModel.appManager.openWifiSettings() },
                onSettingsClick = { viewModel.appManager.openSettings() },
                onBluetoothClick = { viewModel.appManager.openBluetoothSettings() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Main Dock at bottom
            MainDock(
                dockApps = dockApps,
                installedPackageNames = installedPackageSet,
                onAppsGridClick = { viewModel.openAppsDrawer() },
                onDockAppClick = { dockApp ->
                    viewModel.onDockAppClick(dockApp)
                },
                onNavigateDownToGrid = { viewModel.openAppsDrawer() }
            )
        }

        // Bottom Apps Grid Drawer (Grade de apps inferior)
        AppsGridDrawer(
            isVisible = activeSection == LauncherSection.APPS_DRAWER,
            installedApps = installedApps,
            onAppClick = { app ->
                viewModel.appManager.launchApp(app.packageName)
            },
            onAppMenuUninstall = { app ->
                viewModel.setAppToUninstall(app)
            },
            onClose = { viewModel.closeAppsDrawer() }
        )

        // Uninstall confirmation dialog
        AppUninstallDialog(
            app = appToUninstall,
            onConfirm = { viewModel.confirmUninstall() },
            onDismiss = { viewModel.setAppToUninstall(null) }
        )

        // OTA Update Dialog
        if (showOtaDialog) {
            OtaUpdateDialog(
                otaInfo = otaInfo,
                onStartUpdate = { viewModel.startOtaDownload() },
                onDismiss = { viewModel.dismissOtaDialog() }
            )
        }

        // App/OTA Download & Install Pop-up Dialog
        AppDownloadDialog(
            downloadProgress = downloadState,
            onCancel = { viewModel.cancelDownload() },
            onInstall = { viewModel.installDownloadedApk() },
            onDismiss = { viewModel.dismissDownloadDialog() }
        )

        // Voice Search Dialog (works built-in even without Google app installed)
        VoiceSearchDialog(
            isVisible = showVoiceSearchDialog,
            onSearch = { query -> viewModel.performSearch(query) },
            onDismiss = { viewModel.dismissVoiceSearchDialog() }
        )
    }
}
