package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LunaApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android TV; LunaLauncher/1.0)")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        private const val TAG = "LunaApiService"
        private const val BASE_URL = "https://itdoctorbrasil.site/Launchers/Luna/"
        private const val BANNERS_URL = "https://itdoctorbrasil.site/Launchers/Luna/banners/img.json"
        private const val APPS_URL_PRIMARY = "https://itdoctorbrasil.site/Launchers/Luna/apps.json"
        private const val APPS_URL_SECONDARY = "https://itdoctorbrasil.site/Launchers/Luna/app.json"
        private const val APPS_URL_TERTIARY = "https://itdoctorbrasil.site/Launchers/Luna/apps_fixed.json"
        private const val VERSION_URL = "https://itdoctorbrasil.site/Launchers/Luna/version.json"
    }

    private fun resolveUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.startsWith("/")) return "https://itdoctorbrasil.site$trimmed"
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
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.opt(i)
                    if (item is String && item.isNotBlank()) {
                        banners.add(BannerItem(id = "banner_$i", imageUrl = resolveUrl(item)))
                    } else if (item is JSONObject) {
                        val url = item.optString("url", item.optString("image", item.optString("img", item.optString("banner", item.optString("src", item.optString("path", ""))))))
                        val title = item.optString("title", item.optString("name", ""))
                        if (url.isNotBlank()) {
                            banners.add(BannerItem(id = "banner_$i", imageUrl = resolveUrl(url), title = title))
                        }
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                val imagesArray = jsonObject.optJSONArray("images")
                    ?: jsonObject.optJSONArray("banners")
                    ?: jsonObject.optJSONArray("data")
                    ?: jsonObject.optJSONArray("items")

                if (imagesArray != null) {
                    for (i in 0 until imagesArray.length()) {
                        val item = imagesArray.opt(i)
                        if (item is String && item.isNotBlank()) {
                            banners.add(BannerItem(id = "banner_$i", imageUrl = resolveUrl(item)))
                        } else if (item is JSONObject) {
                            val url = item.optString("url", item.optString("image", item.optString("img", item.optString("banner", item.optString("src", item.optString("path", ""))))))
                            val title = item.optString("title", item.optString("name", ""))
                            if (url.isNotBlank()) {
                                banners.add(BannerItem(id = "banner_$i", imageUrl = resolveUrl(url), title = title))
                            }
                        }
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

                val appsArray = jsonObject.optJSONArray("fixed_apps")
                    ?: jsonObject.optJSONArray("apps")
                    ?: jsonObject.optJSONArray("items")
                    ?: jsonObject.optJSONArray("dock")
                    ?: jsonObject.optJSONArray("fixed")
                    ?: jsonObject.optJSONArray("apps_fixed")
                    ?: jsonObject.optJSONArray("grid")
                    ?: jsonObject.optJSONArray("data")

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

            parsedList.sortBy { it.order }

            apps.clear()
            apps.addAll(parsedList.map { it.item })

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing dock apps JSON: ${e.message}", e)
        }
    }

    private fun parseSingleAppObject(obj: JSONObject, defaultIndex: Int): OrderedDockApp {
        val rawId = obj.optString("id", "")
        val title = obj.optString("name", obj.optString("title", obj.optString("label", obj.optString("app_name", obj.optString("appName", "App")))))
        val packageName = obj.optString("package_name", obj.optString("packageName", obj.optString("package", obj.optString("pkg", ""))))
        val rawIconUrl = obj.optString("icon", obj.optString("iconUrl", obj.optString("icon_url", obj.optString("image", obj.optString("img", obj.optString("logo", obj.optString("src", "")))))))
        val rawBannerUrl = obj.optString("banner", obj.optString("bannerUrl", obj.optString("banner_url", obj.optString("bg", ""))))
        val rawActionUrl = obj.optString("download_url", obj.optString("downloadUrl", obj.optString("download", obj.optString("url", obj.optString("link", obj.optString("file", obj.optString("apk", obj.optString("apk_url", obj.optString("action", obj.optString("actionUrl", obj.optString("path", "")))))))))))

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
            title = title,
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
                        val versionName = obj.optString("versionName", obj.optString("version_name", "1.0.0"))
                        val rawUrl = obj.optString("url", obj.optString("downloadUrl", obj.optString("link", "")))
                        val changelog = obj.optString("changelog", obj.optString("notes", obj.optString("description", "")))
                        val force = obj.optBoolean("force", obj.optBoolean("forceUpdate", false))

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
}
