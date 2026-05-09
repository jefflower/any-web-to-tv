package com.jefflower.anywebtotv.web

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebView

@SuppressLint("SetJavaScriptEnabled")
class TvWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = true
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        isFocusable = true
        isFocusableInTouchMode = true
        // make horizontal/vertical scrollbars visible during scroll
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Fallback scroll when no DOM element consumes the key
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (!super.onKeyDown(keyCode, event)) { scrollBy(0, -120); true } else true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!super.onKeyDown(keyCode, event)) { scrollBy(0, 120); true } else true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
