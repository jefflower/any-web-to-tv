package com.jefflower.anywebtotv.web

import android.content.Context
import android.util.AttributeSet
import com.jefflower.anywebtotv.App
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

/**
 * GeckoView-backed replacement for the legacy [TvWebView].
 *
 * Wraps a [GeckoSession] attached to the app-wide [App.geckoRuntime] and exposes a
 * WebView-like surface (loadUrl / canGoBack / goBack / stopLoading / setActive)
 * so the existing [TabManager] flow needs minimal adaptation.
 *
 * Per-session delegates ([GeckoSession.NavigationDelegate], [ProgressDelegate],
 * [ContentDelegate]) are wired up by [TabManager] — not here — because the
 * delegate callbacks need to mutate per-tab state (title, url, lastActiveAt).
 */
class TvGeckoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GeckoView(context, attrs) {

    val geckoSession: GeckoSession = GeckoSession(
        GeckoSessionSettings.Builder()
            .usePrivateMode(false)              // share cookies/localStorage across tabs
            .useTrackingProtection(false)        // TV use-case: don't surprise users
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .build()
    )

    /** Cached value driven by [GeckoSession.NavigationDelegate.onCanGoBack]. */
    @Volatile var canGoBack: Boolean = false
        internal set

    init {
        // NOTE: do NOT call geckoSession.open(runtime) here. The session is opened
        // by TabManager AFTER it has installed its delegates (navigationDelegate /
        // progressDelegate / contentDelegate). If we open in init(), Gecko enqueues
        // an implicit about:blank load before any delegate is attached, and that
        // queued navigation can race with our explicit loadUri() — observed v138
        // behaviour: about:blank wins, our URL is silently dropped.
        setSession(geckoSession)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    /** Called by [TabManager.open] after delegates are wired. Idempotent. */
    fun openSession() {
        if (!geckoSession.isOpen) geckoSession.open(App.geckoRuntime)
    }

    /** WebView-compat helpers so [TabManager] / [WebActivity] don't sprinkle session.* everywhere. */
    fun loadUrl(url: String) {
        geckoSession.loadUri(url)
    }

    fun goBack() {
        geckoSession.goBack()
    }

    fun stopLoading() {
        geckoSession.stop()
    }

    /** Called from Activity onPause/onResume — Gecko throttles inactive sessions. */
    fun setActiveSession(active: Boolean) {
        geckoSession.setActive(active)
    }

    /**
     * Called when the tab is being permanently closed.
     * Detach from the View first, then close the session (releases the content process).
     */
    fun destroyTv() {
        if (geckoSession.isOpen) {
            releaseSession()
            geckoSession.close()
        }
    }
}
