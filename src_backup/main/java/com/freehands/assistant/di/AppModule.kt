package com.freehands.assistant.di

import android.content.Context
import com.freehands.assistant.utils.PreferencesManager
import com.freehands.assistant.utils.ThemeHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideThemeHelper(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): ThemeHelper {
        return ThemeHelper(context, preferencesManager)
    }
}
