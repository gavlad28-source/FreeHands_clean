package com.freehands.assistant.di

import com.freehands.assistant.presentation.MainActivity
import com.freehands.assistant.ui.settings.SettingsActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
@Suppress("unused")
abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MainActivity
    
    @ContributesAndroidInjector
    abstract fun contributeSettingsActivity(): SettingsActivity
}
