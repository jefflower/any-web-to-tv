package com.jefflower.anywebtotv

import android.app.Application
import com.jefflower.anywebtotv.data.AppDatabase
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class App : Application() {
    val db: AppDatabase by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // GeckoRuntime is a process-wide singleton — every GeckoSession in the app
        // attaches to this one runtime. Application.onCreate runs exactly once per
        // process, so no isInitialized guard is needed.
        //
        // `extras` are extra environment variables passed to Gecko's content
        // processes. Mi TV (Android 11, custom MIUI ROM) enforces SELinux policies
        // that block netlink_route_socket bind from untrusted_app domain
        // (b/155595000). When Gecko's NetworkChangeListener tries to bind, it
        // currently aborts the content process. Setting MOZ_DISABLE_CONTENT_SANDBOX
        // bypasses Gecko's content-process sandbox setup, which avoids the abort.
        // Trade-off: web content runs without an additional sandbox layer (still
        // sandboxed by Android's app sandbox). Acceptable for a kiosk launcher
        // that only loads user-trusted sites.
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .remoteDebuggingEnabled(false)         // Set true + adb forward tcp:6000 if you need DevTools.
            .consoleOutput(true)                   // Pipes web `console.*` to logcat tag `GeckoConsole`.
            .aboutConfigEnabled(false)
            .webManifest(false)
            .extras(android.os.Bundle().apply {
                putString("MOZ_DISABLE_CONTENT_SANDBOX", "1")
            })
            .build()
        geckoRuntime = GeckoRuntime.create(this, settings)
    }

    companion object {
        lateinit var instance: App
            private set

        lateinit var geckoRuntime: GeckoRuntime
            private set
    }
}
