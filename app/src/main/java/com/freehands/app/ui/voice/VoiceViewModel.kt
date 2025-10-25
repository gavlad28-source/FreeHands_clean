package com.freehands.app.ui.voice

import androidx.lifecycle.ViewModel
import com.freehands.app.ai.AIManager
import com.freehands.app.ai.voice.VoiceManager

class VoiceViewModel(
    private val aiManager: AIManager,
    private val voiceManager: VoiceManager
) : ViewModel() {}
