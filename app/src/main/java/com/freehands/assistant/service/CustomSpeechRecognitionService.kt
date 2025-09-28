package com.freehands.assistant.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import com.freehands.assistant.utils.VoiceCommandProcessor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A custom speech recognition service that processes voice input and converts it into commands.
 * This service extends Android's RecognitionService to provide voice recognition capabilities.
 */
@AndroidEntryPoint
class CustomSpeechRecognitionService : RecognitionService() {

    @Inject
    lateinit var commandProcessor: VoiceCommandProcessor
    
    private val recognizer by lazy { SpeechRecognizer.createSpeechRecognizer(this) }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onStartListening(recognizerParams: RecognitionService.Callback) {
        // Configure and start listening for speech
        val intent = Intent(RecognitionService.SERVICE_INTERFACE).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
        }
        
        recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                recognizerParams.readyForSpeech(params)
            }
            
            override fun onBeginningOfSpeech() {
                recognizerParams.beginningOfSpeech()
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                recognizerParams.rmsChanged(rmsdB)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                recognizerParams.bufferReceived(buffer)
            }
            
            override fun onEndOfSpeech() {
                recognizerParams.endOfSpeech()
            }
            
            override fun onError(error: Int) {
                recognizerParams.error(error)
            }
            
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    // Process the recognized text
                    commandProcessor.processCommand(bestMatch) { success, response ->
                        // Handle the response from command processing
                        if (success) {
                            // Notify success if needed
                        } else {
                            // Handle error or unknown command
                        }
                    }
                }
                recognizerParams.results(results)
            }
            
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                recognizerParams.partialResults(partialResults)
            }
            
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {
                recognizerParams.event(eventType, params)
            }
        })
        
        recognizer.startListening(intent)
    }
    
    override fun onStopListening(recognizerParams: RecognitionService.Callback) {
        recognizer.stopListening()
    }
    
    override fun onCancel(recognizerParams: RecognitionService.Callback) {
        recognizer.cancel()
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        recognizer.destroy()
    }
}
