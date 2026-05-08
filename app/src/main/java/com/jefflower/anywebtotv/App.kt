package com.jefflower.anywebtotv

import android.app.Application
import com.jefflower.anywebtotv.data.AppDatabase

class App : Application() {
    val db: AppDatabase by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
