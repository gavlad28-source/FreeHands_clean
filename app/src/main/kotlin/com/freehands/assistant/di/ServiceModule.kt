package com.freehands.assistant.di

import com.freehands.assistant.VoiceListeningService
import com.freehands.assistant.VoiceAccessibilityService
import com.freehands.assistant.VoiceInteractionService
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
@Suppress("unused")
abstract class ServiceModule {
    
    @ContributesAndroidInjector
    abstract fun contributeVoiceListeningService(): VoiceListeningService
    
    @ContributesAndroidInjector
    abstract fun contributeVoiceAccessibilityService(): VoiceAccessibilityService
    
    @ContributesAndroidInjector
    abstract fun contributeVoiceInteractionService(): VoiceInteractionService
}
