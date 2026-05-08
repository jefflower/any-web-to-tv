package com.jefflower.anywebtotv.web

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class TabManager(
    private val ctx: Context,
    private val container: FrameLayout,
    private val maxTabs: Int = 5
) {

    data class Tab(val webView: TvWebView, var title: String, var url: String, var lastActiveAt: Long = System.currentTimeMillis())

    private val tabs = mutableListOf<Tab>()
    private var currentIndex = -1

    var onTitleChanged: ((Int, String) -> Unit)? = null
    var onProgress: ((Int) -> Unit)? = null

    fun open(url: String, title: String? = null): Int {
        if (tabs.size >= maxTabs) {
            // LRU: drop the least-recently-active (excluding current)
            val victim = tabs.withIndex()
                .filter { it.index != currentIndex }
                .minByOrNull { it.value.lastActiveAt }
            if (victim != null) close(victim.index)
        }
        val wv = TvWebView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        val tab = Tab(wv, title ?: url, url)
        tabs.add(tab)
        container.addView(wv)

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                handleUrl(view, request.url.toString())

            @Deprecated("Required for API < 24")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                handleUrl(view, url)

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                // Suppress the ugly system error page for unknown-scheme main-frame nav,
                // which can leak through on some Chromium builds despite shouldOverrideUrlLoading.
                if (request.isForMainFrame && error.errorCode == ERROR_UNSUPPORTED_SCHEME) {
                    view.stopLoading()
                    return
                }
                super.onReceivedError(view, request, error)
            }

            override fun onPageFinished(view: WebView, finishedUrl: String) {
                tab.url = finishedUrl
                view.title?.let {
                    tab.title = it
                    onTitleChanged?.invoke(tabs.indexOf(tab), it)
                }
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgress?.invoke(newProgress)
            }
            override fun onReceivedTitle(view: WebView, t: String) {
                tab.title = t
                onTitleChanged?.invoke(tabs.indexOf(tab), t)
            }
        }
        wv.loadUrl(url)
        switchTo(tabs.size - 1)
        return tabs.size - 1
    }

    fun switchTo(i: Int) {
        if (i !in tabs.indices) return
        tabs.getOrNull(currentIndex)?.webView?.visibility = View.GONE
        tabs.getOrNull(currentIndex)?.webView?.onPause()
        tabs[i].webView.visibility = View.VISIBLE
        tabs[i].webView.onResume()
        tabs[i].lastActiveAt = System.currentTimeMillis()
        tabs[i].webView.requestFocus()
        currentIndex = i
    }

    fun close(i: Int) {
        if (i !in tabs.indices) return
        val tab = tabs[i]
        container.removeView(tab.webView)
        tab.webView.stopLoading()
        tab.webView.loadUrl("about:blank")
        tab.webView.destroy()
        tabs.removeAt(i)
        if (tabs.isEmpty()) {
            currentIndex = -1
        } else {
            val newIdx = if (currentIndex == i) (i.coerceAtMost(tabs.size - 1))
                         else if (currentIndex > i) currentIndex - 1
                         else currentIndex
            switchTo(newIdx)
        }
    }

    private fun handleUrl(view: WebView, url: String): Boolean {
        val lower = url.lowercase()
        // Standard schemes: let WebView load them
        if (lower.startsWith("http://") || lower.startsWith("https://") ||
            lower.startsWith("about:") || lower.startsWith("data:") ||
            lower.startsWith("javascript:") || lower.startsWith("file://") ||
            lower.startsWith("blob:")) return false

        // Custom scheme (deep link, e.g. baiduboxapp://, bilibili://, weixin://, intent://)
        // 1) Try to launch a real Android app that handles it.
        // 2) If none, look for an intent:// fallback URL and load that in WebView.
        // 3) Otherwise, swallow silently — never show ERR_UNKNOWN_URL_SCHEME.
        runCatching {
            val intent = if (lower.startsWith("intent://")) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

            if (intent.resolveActivity(ctx.packageManager) != null) {
                ctx.startActivity(intent)
            } else {
                intent.getStringExtra("browser_fallback_url")?.let { fallback ->
                    view.loadUrl(fallback)
                }
            }
        }
        return true
    }

    fun current(): Tab? = tabs.getOrNull(currentIndex)
    fun currentIndex(): Int = currentIndex
    fun all(): List<Tab> = tabs.toList()
    fun size(): Int = tabs.size

    fun switchRelative(delta: Int) {
        if (tabs.size < 2) return
        val n = tabs.size
        val next = ((currentIndex + delta) % n + n) % n
        switchTo(next)
    }

    fun destroyAll() {
        tabs.forEach {
            container.removeView(it.webView)
            it.webView.stopLoading()
            it.webView.destroy()
        }
        tabs.clear()
        currentIndex = -1
    }
}
