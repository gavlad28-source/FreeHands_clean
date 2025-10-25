package com.freehands.assistant.di

import com.freehands.assistant.service.VoiceAccessibilityService
import com.freehands.assistant.service.VoiceInteractionSessionService
import com.freehands.assistant.service.VoiceListeningService
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

@Module
@InstallIn(ServiceComponent::class)
@Suppress("unused")
abstract class ServiceModule {
    
    @ContributesAndroidInjector
    abstract fun contributeVoiceListeningService(): VoiceListeningService
    
    @ContributesAndroidInjector
    abstract fun contributeVoiceAccessibilityService(): VoiceAccessibilityService
    
    @ContributesAndroidInjector
    abstract fun contributeVoiceInteractionService(): VoiceInteractionSessionService
}
