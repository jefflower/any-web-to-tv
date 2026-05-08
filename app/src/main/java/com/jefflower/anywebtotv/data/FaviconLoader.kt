package com.jefflower.anywebtotv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object FaviconLoader {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private fun sha1(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun cacheFile(context: Context, url: String): File {
        val dir = File(context.filesDir, "favicons").apply { if (!exists()) mkdirs() }
        return File(dir, sha1(url) + ".png")
    }

    suspend fun fetch(context: Context, pageUrl: String): String? = withContext(Dispatchers.IO) {
        val target = cacheFile(context, pageUrl)
        if (target.exists() && target.length() > 0) return@withContext target.absolutePath

        val host = runCatching { URL(pageUrl).host }.getOrNull() ?: return@withContext null
        val candidates = listOf(
            "https://www.google.com/s2/favicons?domain=$host&sz=128",
            "https://$host/favicon.ico",
            "https://icons.duckduckgo.com/ip3/$host.ico"
        )
        for (u in candidates) {
            try {
                val req = Request.Builder().url(u)
                    .header("User-Agent", "Mozilla/5.0 AnyWebToTv")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val bytes = resp.body?.bytes() ?: return@use
                    if (bytes.size < 64) return@use
                    target.writeBytes(bytes)
                    return@withContext target.absolutePath
                }
            } catch (_: Exception) { /* try next */ }
        }
        null
    }
}
