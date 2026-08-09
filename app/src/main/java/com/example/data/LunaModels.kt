package com.example.data

import android.graphics.drawable.Drawable
import com.google.gson.annotations.SerializedName

// -------------------------------------------------------------
// MODELOS DA API REST DO PAINEL (LUNA LAUNCHER)
// -------------------------------------------------------------

data class LauncherConfigResponse(
    val status: String = "success",
    val message: String? = null,
    val reseller: ResellerInfo? = null,
    val client: ClientInfo? = null,
    val background: BackgroundConfig? = null,
    val apps: List<DockAppItem> = emptyList()
)

data class ResellerInfo(
    val name: String = "",
    val token: String = ""
)

data class ClientInfo(
    val id: String = "",
    val name: String = "",
    val macAddress: String = "",
    val deviceToken: String = "",
    val expirationDate: String = "",
    val isActive: Boolean = true,
    val daysRemaining: Int = 0
)

data class BackgroundConfig(
    @SerializedName("url", alternate = ["bg_url", "img_url"])
    val url: String = "",

    val overlayOpacity: Float = 0.35f,

    // Lista contendo todas as imagens de fundo enviadas pelo Painel Web
    val images: List<String> = emptyList(),

    // Lista de banners estruturados
    val banners: List<BannerItem> = emptyList()
)

// -------------------------------------------------------------
// MODELOS DE INTERFACE E APPS LOCAIS
// -------------------------------------------------------------

data class BannerItem(
    val id: String = "",
    @SerializedName("imageUrl", alternate = ["image_url", "url"])
    val imageUrl: String = "",
    val title: String = ""
)

data class DockAppItem(
    val id: String = "",
    @SerializedName("title", alternate = ["name", "app_name"])
    val title: String = "",
    @SerializedName("packageName", alternate = ["package_name", "package"])
    val packageName: String = "",
    @SerializedName("iconUrl", alternate = ["icon_url", "icon"])
    val iconUrl: String = "",
    val bannerUrl: String = "",
    @SerializedName("actionUrl", alternate = ["deepLink", "url"])
    val actionUrl: String = "",
    val iconDrawable: Drawable? = null
)

data class InstalledApp(
    val title: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false
)

data class OtaVersionInfo(
    @SerializedName("versionCode", alternate = ["version_code", "code"])
    val versionCode: Int = 1,
    @SerializedName("versionName", alternate = ["version_name", "version"])
    val versionName: String = "1.0.0",
    @SerializedName("url", alternate = ["downloadUrl", "download_url", "apk_url"])
    val url: String = "",
    @SerializedName("changelog", alternate = ["releaseNotes", "release_notes", "notes"])
    val changelog: String = "",
    @SerializedName("forceUpdate", alternate = ["force_update"])
    val forceUpdate: Boolean = false
)