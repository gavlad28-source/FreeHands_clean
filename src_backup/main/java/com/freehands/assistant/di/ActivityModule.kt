package com.freehands.assistant.di

import com.freehands.assistant.MainActivity
import com.freehands.assistant.SettingsActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
@Suppress("unused")
abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MainActivity
    
    @ContributesAndroidInjector
    abstract fun contributeSettingsActivity(): SettingsActivity
}
