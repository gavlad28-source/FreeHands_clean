package com.freehands.app.di

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.hilt.android.lifecycle.HiltViewModel
import com.freehands.app.ui.main.MainViewModel
import com.freehands.app.ui.main.SettingsViewModel
import com.freehands.app.ui.voice.VoiceViewModel

@Module
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {

    // Example bindings
    // TODO: add @Binds if needed
}
