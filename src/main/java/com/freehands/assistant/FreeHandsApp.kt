package com.freehands.assistant

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.freehands.assistant.di.AppComponent
import com.freehands.assistant.di.DaggerAppComponent
import com.freehands.assistant.utils.CrashReportingTree
import com.freehands.assistant.utils.ThemeHelper
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class FreeHandsApp : Application() {

    @Inject
    lateinit var themeHelper: ThemeHelper

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Setup logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        
        // Apply theme
        AppCompatDelegate.setDefaultNightMode(themeHelper.getThemeMode())
    }

    companion object {
        private lateinit var instance: FreeHandsApp
        
        fun getAppContext(): Context = instance.applicationContext
    }
}
