package com.freehands.assistant.di

import android.content.Context
import com.freehands.assistant.FreeHandsApp
import com.freehands.assistant.utils.PreferencesManager
import com.freehands.assistant.utils.ThemeHelper
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AppModule::class,
        ViewModelModule::class,
        ActivityModule::class,
        ServiceModule::class
    ]
)
interface AppComponent : AndroidInjector<FreeHandsApp> {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): AppComponent
    }

    fun preferencesManager(): PreferencesManager
    fun themeHelper(): ThemeHelper
}
