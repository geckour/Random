package com.geckour.random

import android.app.Application
import com.geckour.random.ui.theme.SeedRepository
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        GlobalContext.getOrNull() ?: startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        get<SeedRepository>().setInitializedTime()
    }
}