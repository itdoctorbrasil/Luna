package com.example.data

import android.graphics.drawable.Drawable

data class BannerItem(
    val id: String = "",
    val imageUrl: String = "",
    val title: String = ""
)

data class DockAppItem(
    val id: String = "",
    val title: String = "",
    val packageName: String = "",
    val iconUrl: String = "",
    val bannerUrl: String = "",
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
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val url: String = "",
    val changelog: String = "",
    val forceUpdate: Boolean = false
)
