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
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "LunaApiService"
        private const val BANNERS_URL = "https://itdoctorbrasil.site/Launchers/Luna/banners/img.json"
        private const val APPS_URL_PRIMARY = "https://itdoctorbrasil.site/Launchers/Luna/apps.json"
        private const val APPS_URL_SECONDARY = "https://itdoctorbrasil.site/Launchers/Luna/app.json"
        private const val VERSION_URL = "https://itdoctorbrasil.site/Launchers/Luna/version.json"
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
                    if (item is String) {
                        banners.add(BannerItem(id = "banner_$i", imageUrl = item))
                    } else if (item is JSONObject) {
                        val url = item.optString("url", item.optString("image", item.optString("img", "")))
                        val title = item.optString("title", item.optString("name", ""))
                        if (url.isNotEmpty()) {
                            banners.add(BannerItem(id = "banner_$i", imageUrl = url, title = title))
                        }
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                val imagesArray = jsonObject.optJSONArray("images") ?: jsonObject.optJSONArray("banners")
                if (imagesArray != null) {
                    for (i in 0 until imagesArray.length()) {
                        val item = imagesArray.opt(i)
                        if (item is String) {
                            banners.add(BannerItem(id = "banner_$i", imageUrl = item))
                        } else if (item is JSONObject) {
                            val url = item.optString("url", item.optString("image", ""))
                            val title = item.optString("title", "")
                            if (url.isNotEmpty()) {
                                banners.add(BannerItem(id = "banner_$i", imageUrl = url, title = title))
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
        val urlsToTry = listOf(APPS_URL_PRIMARY, APPS_URL_SECONDARY)

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

                val appsArray = jsonObject.optJSONArray("apps")
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
        val title = obj.optString("title", obj.optString("name", obj.optString("label", obj.optString("app_name", "App"))))
        val packageName = obj.optString("packageName", obj.optString("package", obj.optString("pkg", obj.optString("package_name", ""))))
        val iconUrl = obj.optString("icon", obj.optString("iconUrl", obj.optString("icon_url", obj.optString("image", ""))))
        val bannerUrl = obj.optString("banner", obj.optString("bannerUrl", obj.optString("banner_url", "")))
        val actionUrl = obj.optString("url", obj.optString("action", obj.optString("actionUrl", obj.optString("apk", obj.optString("downloadUrl", "")))))

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
            id = "dock_app_${packageName.ifEmpty { defaultIndex.toString() }}",
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
                        val url = obj.optString("url", obj.optString("downloadUrl", obj.optString("link", "")))
                        val changelog = obj.optString("changelog", obj.optString("notes", obj.optString("description", "")))
                        val force = obj.optBoolean("force", obj.optBoolean("forceUpdate", false))

                        return@withContext OtaVersionInfo(
                            versionCode = versionCode,
                            versionName = versionName,
                            url = url,
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
