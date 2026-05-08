package com.jefflower.anywebtotv.web

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jefflower.anywebtotv.R

class WebActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    private lateinit var container: FrameLayout
    private lateinit var progress: ProgressBar
    private lateinit var hintToast: TextView
    private lateinit var tabOverlay: TabOverlayView
    private lateinit var tabManager: TabManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        container = findViewById(R.id.web_container)
        progress = findViewById(R.id.progress)
        hintToast = findViewById(R.id.hint_toast)
        tabOverlay = findViewById(R.id.tab_overlay)

        tabManager = TabManager(this, container).apply {
            onProgress = { p -> progress.visibility = if (p in 1..99) View.VISIBLE else View.GONE }
        }

        tabOverlay.onTabSelected = { idx ->
            tabManager.switchTo(idx)
            tabOverlay.hide()
        }
        tabOverlay.onTabDeleteRequested = { idx ->
            tabManager.close(idx)
            if (tabManager.size() == 0) finish()
            else tabOverlay.show(tabManager.all(), tabManager.currentIndex())
        }

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) { finish(); return }
        tabManager.open(url, intent.getStringExtra(EXTRA_TITLE))
        showHint()
    }

    private fun showHint() {
        hintToast.visibility = View.VISIBLE
        hintToast.postDelayed({ hintToast.visibility = View.GONE }, 4000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                if (tabOverlay.isOpen()) tabOverlay.hide()
                else tabOverlay.show(tabManager.all(), tabManager.currentIndex())
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (event.repeatCount == 0) {
                    event.startTracking()
                    return true
                }
                return true
            }
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                tabManager.switchRelative(+1); return true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                tabManager.switchRelative(-1); return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.isTracking && !event.isCanceled) {
                if (tabOverlay.isOpen()) { tabOverlay.hide(); return true }
                val cur = tabManager.current()?.webView
                if (cur != null && cur.canGoBack()) { cur.goBack(); return true }
                finish()
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        tabManager.current()?.webView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        tabManager.current()?.webView?.onResume()
    }

    override fun onDestroy() {
        tabManager.destroyAll()
        super.onDestroy()
    }
}
