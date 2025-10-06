package com.freehands.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface OnRecognitionListener {
    fun onReadyForSpeech(params: Bundle?)
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onBufferReceived(buffer: ByteArray?)
    fun onEndOfSpeech()
    fun onError(error: Int)
    fun onResults(results: Bundle?)
    fun onPartialResults(partialResults: Bundle?)
}

@Singleton
class VoiceRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: OnRecognitionListener? = null

    fun setListener(listener: OnRecognitionListener) {
        this.listener = listener
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listener?.onReadyForSpeech(params)
                }

                override fun onBeginningOfSpeech() {
                    listener?.onBeginningOfSpeech()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    listener?.onRmsChanged(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    listener?.onBufferReceived(buffer)
                }

                override fun onEndOfSpeech() {
                    listener?.onEndOfSpeech()
                }

                override fun onError(error: Int) {
                    listener?.onError(error)
                }

                override fun onResults(results: Bundle?) {
                    listener?.onResults(results)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    listener?.onPartialResults(partialResults)
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
        } else {
            Timber.e("Speech recognition not available")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun cancel() {
        speechRecognizer?.cancel()
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }
}
