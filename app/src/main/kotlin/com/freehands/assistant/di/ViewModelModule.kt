package com.freehands.assistant.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.freehands.assistant.presentation.MainViewModel
import com.freehands.assistant.presentation.SettingsViewModel
import com.freehands.assistant.presentation.viewmodel.VoiceViewModel
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@Module
@InstallIn(SingletonComponent::class)
@Suppress("UNUSED")
abstract class ViewModelModule {
    
    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel
    
    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel
    
    @Binds
    @IntoMap
    @ViewModelKey(VoiceViewModel::class)
    abstract fun bindVoiceViewModel(viewModel: VoiceViewModel): ViewModel
    
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)
