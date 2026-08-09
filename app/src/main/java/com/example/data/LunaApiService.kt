package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class LunaApiService {

    private val client: OkHttpClient = NetworkClientHelper.okHttpClient

    companion object {
        private const val TAG = "LunaApiService"

        // Apontamento principal para o servidor Render Multitenant
        private const val BASE_URL = "https://painel-luna.itdoctorbrasil.site/"
        private const val BANNERS_URL = "https://painel-luna.itdoctorbrasil.site/img.json"
        private const val APPS_URL_PRIMARY = "https://painel-luna.itdoctorbrasil.site/apps.json"
        private const val APPS_URL_SECONDARY = "https://painel-luna.itdoctorbrasil.site/api/v1/launcher/apps.json"
        private const val APPS_URL_TERTIARY = "https://painel-luna.itdoctorbrasil.site/apps.json"
        private const val VERSION_URL = "https://painel-luna.itdoctorbrasil.site/version.json"
    }

    private fun resolveUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.startsWith("/")) return "https://painel-luna.itdoctorbrasil.site$trimmed"
        return "$BASE_URL$trimmed"
    }

    suspend fun fetchBanners(): List<BannerItem> = withContext(Dispatchers.IO) {
        val banners = mutableListOf<BannerItem>()
        try {
            val request = Request.Builder().url(BANNERS_URL).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        parseBannersJson(bodyString, banners)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching banners: ${e.message}", e)
        }
        return@withContext banners
    }

    private fun parseBannersJson(jsonStr: String, banners: MutableList<BannerItem>) {
        try {
            val trimmed = jsonStr.trim()
            banners.clear() // Limpa duplicatas para garantir a atualização em tempo real do background

            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.opt(i)
                    if (item is String && item.isNotBlank()) {
                        banners.add(BannerItem(id = "banner_$i", imageUrl = resolveUrl(item)))
                    } else if (item is JSONObject) {
                        val url = optString(item, "url", "image", "img", "banner", "src", "path", "imageUrl", "image_url")
                        val title = optString(item, "title", "name")
                        if (url.isNotBlank()) {
                            banners.add(BannerItem(id = "banner_$i", imageUrl = resolveUrl(url), title = title))
                        }
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                val imagesArray = optArray(jsonObject, "images", "banners", "data", "items")

                if (imagesArray != null && imagesArray.length() > 0) {
                    for (i in 0 until imagesArray.length()) {
                        val item = imagesArray.opt(i)
                        if (item is String && item.isNotBlank()) {
                            banners.add(BannerItem(id = "banner_$i", imageUrl = resolveUrl(item)))
                        } else if (item is JSONObject) {
                            val url = optString(item, "url", "image", "img", "banner", "src", "path", "imageUrl", "image_url")
                            val title = optString(item, "title", "name")
                            if (url.isNotBlank()) {
                                banners.add(BannerItem(id = "banner_$i", imageUrl = resolveUrl(url), title = title))
                            }
                        }
                    }
                } else {
                    val singleUrl = optString(jsonObject, "backgroundUrl", "bg_url", "url", "image")
                    if (singleUrl.isNotBlank()) {
                        banners.add(BannerItem(id = "banner_0", imageUrl = resolveUrl(singleUrl)))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing banners JSON: ${e.message}", e)
        }
    }

    private data class OrderedDockApp(
        val order: Int,
        val item: DockAppItem
    )

    suspend fun fetchDockApps(): List<DockAppItem> = withContext(Dispatchers.IO) {
        val dockApps = mutableListOf<DockAppItem>()
        val urlsToTry = listOf(APPS_URL_PRIMARY, APPS_URL_SECONDARY, APPS_URL_TERTIARY)

        for (url in urlsToTry) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank()) {
                            parseAppsJson(bodyString, dockApps)
                            if (dockApps.isNotEmpty()) {
                                return@withContext dockApps
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching dock apps from $url: ${e.message}")
            }
        }
        return@withContext dockApps
    }

    private fun parseAppsJson(jsonStr: String, apps: MutableList<DockAppItem>) {
        try {
            val trimmed = jsonStr.trim()
            val parsedList = mutableListOf<OrderedDockApp>()

            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i)
                    if (obj != null) {
                        val parsed = parseSingleAppObject(obj, defaultIndex = i)
                        if (parsed != null) parsedList.add(parsed)
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)

                val appsArray = optArray(jsonObject, "apps", "fixed_apps", "items", "dock", "fixed", "apps_fixed", "grid", "data")

                if (appsArray != null) {
                    for (i in 0 until appsArray.length()) {
                        val obj = appsArray.optJSONObject(i)
                        if (obj != null) {
                            val parsed = parseSingleAppObject(obj, defaultIndex = i)
                            if (parsed != null) parsedList.add(parsed)
                        }
                    }
                } else {
                    val keys = jsonObject.keys()
                    var indexCount = 0
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = jsonObject.optJSONObject(key)
                        if (obj != null) {
                            val keyAsInt = key.toIntOrNull() ?: indexCount
                            val parsed = parseSingleAppObject(obj, defaultIndex = keyAsInt)
                            if (parsed != null) parsedList.add(parsed)
                        }
                        indexCount++
                    }
                }
            }

            // Ordena estritamente pelo campo "order" / "position" enviado pelo servidor
            parsedList.sortBy { it.order }

            apps.clear()
            apps.addAll(parsedList.map { it.item })

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing dock apps JSON: ${e.message}", e)
        }
    }

    private fun parseSingleAppObject(obj: JSONObject, defaultIndex: Int): OrderedDockApp {
        val rawId = optString(obj, "id")
        val title = optString(obj, "name", "title", "label", "app_name", "appName", "App")
        val packageName = optString(obj, "package_name", "packageName", "package", "pkg")
        val rawIconUrl = optString(obj, "iconUrl", "icon_url", "icon", "image", "img", "logo", "src")
        val rawBannerUrl = optString(obj, "bannerUrl", "banner_url", "banner", "bg")
        val rawActionUrl = optString(obj, "download_url", "downloadUrl", "download", "url", "link", "file", "apk", "apk_url", "action", "actionUrl", "path", "deepLink")

        val finalId = if (rawId.isNotEmpty()) rawId else "dock_app_${packageName.ifEmpty { defaultIndex.toString() }}"

        val iconUrl = if (rawIconUrl.isNotBlank()) resolveUrl(rawIconUrl) else ""
        val bannerUrl = if (rawBannerUrl.isNotBlank()) resolveUrl(rawBannerUrl) else ""
        val actionUrl = if (rawActionUrl.isNotBlank()) resolveUrl(rawActionUrl) else ""

        var order = defaultIndex
        if (obj.has("order")) {
            order = obj.optInt("order", defaultIndex)
        } else if (obj.has("position")) {
            order = obj.optInt("position", defaultIndex)
        } else if (obj.has("index")) {
            order = obj.optInt("index", defaultIndex)
        } else if (obj.has("pos")) {
            order = obj.optInt("pos", defaultIndex)
        } else if (obj.has("sort")) {
            order = obj.optInt("sort", defaultIndex)
        }

        val item = DockAppItem(
            id = finalId,
            title = if (title.isBlank()) "App" else title,
            packageName = packageName,
            iconUrl = iconUrl,
            bannerUrl = bannerUrl,
            actionUrl = actionUrl
        )

        return OrderedDockApp(order = order, item = item)
    }

    suspend fun fetchOtaVersion(): OtaVersionInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(VERSION_URL).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val obj = JSONObject(bodyString.trim())
                        val versionCode = obj.optInt("versionCode", obj.optInt("version_code", obj.optInt("version", 1)))
                        val versionName = optString(obj, "versionName", "version_name", "version").ifEmpty { "1.0.0" }
                        val rawUrl = optString(obj, "apkUrl", "apk_url", "url", "downloadUrl", "download_url", "link", "apk", "file", "path", "actionUrl", "action")
                        val changelog = optString(obj, "changelog", "notes", "description", "releaseNotes")
                        val force = obj.optBoolean("forceUpdate", obj.optBoolean("force_update", obj.optBoolean("force", false)))

                        return@withContext OtaVersionInfo(
                            versionCode = versionCode,
                            versionName = versionName,
                            url = if (rawUrl.isNotBlank()) resolveUrl(rawUrl) else "",
                            changelog = changelog,
                            forceUpdate = force
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching OTA version info: ${e.message}", e)
        }
        return@withContext null
    }

    private fun optString(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            if (obj.has(key)) {
                val value = obj.optString(key, "")
                if (value.isNotBlank() && value != "null") return value
            }
        }
        return ""
    }

    private fun optArray(obj: JSONObject, vararg keys: String): JSONArray? {
        for (key in keys) {
            val array = obj.optJSONArray(key)
            if (array != null) return array
        }
        return null
    }
}