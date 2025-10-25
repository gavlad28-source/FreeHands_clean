package com.freehands.app.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VoiceManager(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context, this)
    private val _lastSpoken = MutableStateFlow("""")
    val lastSpoken: StateFlow<String> get() = _lastSpoken

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        _lastSpoken.value = text
    }

    fun stop() = tts.stop()
}
