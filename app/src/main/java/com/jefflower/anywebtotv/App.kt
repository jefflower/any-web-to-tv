package com.jefflower.anywebtotv

import android.app.Application
import com.jefflower.anywebtotv.data.AppDatabase

class App : Application() {
    val db: AppDatabase by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // No GeckoRuntime here. Strategy pivoted: instead of embedding a
        // browser engine (GeckoView spawned :tab/:gpu services that triggered
        // MIUI tvhome's `LocalAppBlockCreater` to forceStop our package), we
        // delegate rendering to a sideloaded Chromium-based browser
        // (Bromite/Firefox) via Intent.ACTION_VIEW. Our app stays a tiny
        // bookmark launcher with no native services — invisible to MIUI's
        // process compliance scan.
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
