package com.jefflower.anywebtotv.util

import android.webkit.URLUtil
import java.net.URL

object UrlUtil {

    fun normalize(input: String): String? {
        val raw = input.trim().ifBlank { return null }
        val withScheme = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        return runCatching {
            val u = URL(withScheme)
            if (u.host.isNullOrBlank() || !u.host.contains(".")) null else withScheme
        }.getOrNull()
    }

    fun isValid(input: String): Boolean = URLUtil.isValidUrl(normalize(input) ?: "")

    fun deriveName(url: String): String =
        runCatching { URL(url).host?.removePrefix("www.") ?: url }.getOrDefault(url)
}
