package com.jefflower.anywebtotv.web

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
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
