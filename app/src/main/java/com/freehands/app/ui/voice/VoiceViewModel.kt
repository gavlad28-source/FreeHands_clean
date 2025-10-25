package com.freehands.app.ui.voice

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.freehands.app.ai.AIManager
import com.freehands.app.ai.voice.VoiceManager

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val aiManager: AIManager,
    private val voiceManager: VoiceManager
): ViewModel() {}
