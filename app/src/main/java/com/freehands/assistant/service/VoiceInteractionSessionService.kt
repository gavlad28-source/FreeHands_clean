package com.freehands.assistant.service

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.freehands.assistant.R
import com.freehands.assistant.data.service.VoiceAssistantService
import com.freehands.assistant.databinding.OverlayVoiceCommandBinding

@RequiresApi(Build.VERSION_CODES.M)
class VoiceInteractionSessionService : android.service.voice.VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return VoiceCommandSession(this)
    }

    inner class VoiceCommandSession(context: Context) : VoiceInteractionSession(context) {
        private var overlayView: View? = null
        private var windowManager: WindowManager? = null
        private var binding: OverlayVoiceCommandBinding? = null

        init {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }

        override fun onHandleAssist(
            data: Bundle?,
            structure: AssistStructure?,
            content: AssistContent?
        ) {
            // Handle assist data if needed
        }

        override fun onShow(args: Bundle?, showFlags: Int) {
            super.onShow(args, showFlags)
            showOverlay()
        }

        override fun onHide() {
            super.onHide()
            hideOverlay()
        }

        private fun showOverlay() {
            if (overlayView != null) return

            val inflater = LayoutInflater.from(this@VoiceInteractionSessionService)
            binding = OverlayVoiceCommandBinding.inflate(inflater, null, false)
            overlayView = binding?.root

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                else 
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 100
            }

            windowManager?.addView(overlayView, params)
            startVoiceRecognition()
        }

        private fun hideOverlay() {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
                binding = null
            }
        }

        private fun startVoiceRecognition() {
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(
                this@VoiceInteractionSessionService
            )

            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding?.statusText?.text = "Listening..."
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    hide()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    text?.let { processVoiceCommand(it) }
                    hide()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            speechRecognizer.setRecognitionListener(listener)

            val intent = android.speech.RecognizerIntent.getVoiceDetailsIntent(applicationContext).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer.startListening(intent)
        }

        private fun processVoiceCommand(command: String) {
            // Process the voice command here
            // You can send it to your VoiceCommandProcessor or ViewModel
            // For example:
            // viewModel.processCommand(command)
        }

        override fun onDestroy() {
            hideOverlay()
            super.onDestroy()
        }
    }
}
