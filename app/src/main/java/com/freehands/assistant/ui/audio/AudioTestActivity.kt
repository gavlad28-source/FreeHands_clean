package com.freehands.assistant.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.freehands.assistant.R
import com.freehands.assistant.utils.AudioFeatureExtractor
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

class AudioTestActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var recordButton: Button
    private lateinit var processButton: Button
    private lateinit var benchmarkButton: Button
    
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val audioData = mutableListOf<Short>()
    private val featureExtractor = AudioFeatureExtractor()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private val RECORD_AUDIO_PERMISSION_CODE = 1001
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, 
        CHANNEL_CONFIG, 
        AUDIO_FORMAT
    ) * 2
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_test)
        
        statusText = findViewById(R.id.status_text)
        recordButton = findViewById(R.id.record_button)
        processButton = findViewById(R.id.process_button)
        benchmarkButton = findViewById(R.id.benchmark_button)
        
        recordButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                toggleRecording()
            }
        }
        
        processButton.setOnClickListener {
            processAudio()
        }
        
        benchmarkButton.setOnClickListener {
            runBenchmark()
        }
        
        updateUI()
    }
    
    private fun checkAndRequestPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
            return false
        }
        return true
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleRecording()
            } else {
                statusText.text = "Microphone permission denied"
            }
        }
    }
    
    private fun toggleRecording() {
        isRecording = !isRecording
        
        if (isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
        
        updateUI()
    }
    
    private fun startRecording() {
        audioData.clear()
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )
        
        audioRecord?.startRecording()
        
        recordingThread = Thread({
            val buffer = ShortArray(BUFFER_SIZE / 2)
            
            while (isRecording) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    synchronized(audioData) {
                        audioData.addAll(buffer.take(bytesRead).toList())
                    }
                    
                    // Update UI with audio level
                    val rms = calculateRms(buffer, bytesRead)
                    val db = 20 * log10(rms.toDouble() / 32768.0)
                    
                    runOnUiThread {
                        statusText.text = "Recording... ${String.format("%.1f", db)} dB"
                    }
                }
            }
        }, "AudioRecorder Thread")
        
        recordingThread?.start()
    }
    
    private fun stopRecording() {
        isRecording = false
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            recordingThread?.join()
        } catch (e: Exception) {
            Log.e("AudioTest", "Error stopping recording", e)
        } finally {
            audioRecord = null
            recordingThread = null
        }
        
        statusText.text = "Recorded ${audioData.size} samples (${audioData.size / SAMPLE_RATE} seconds)"
    }
    
    private fun processAudio() {
        if (audioData.isEmpty()) {
            statusText.text = "No audio data to process"
            return
        }
        
        scope.launch(Dispatchers.Main) {
            statusText.text = "Processing audio..."
            
            val features = withContext(Dispatchers.Default) {
                try {
                    featureExtractor.extractFeatures(audioData.toShortArray())
                } catch (e: Exception) {
                    Log.e("AudioTest", "Error extracting features", e)
                    null
                }
            }
            
            features?.let {
                val featuresStr = it.joinToString(", ") { "%.4f".format(it) }
                statusText.text = "MFCC Features:\n$featuresStr"
                
                // Save features to file for analysis
                saveFeaturesToFile(it)
            } ?: run {
                statusText.text = "Failed to extract features"
            }
        }
    }
    
    private fun runBenchmark() {
        if (audioData.isEmpty()) {
            statusText.text = "Record some audio first"
            return
        }
        
        scope.launch(Dispatchers.Main) {
            statusText.text = "Running benchmark..."
            
            val results = withContext(Dispatchers.Default) {
                val audio = audioData.toShortArray()
                val iterations = 100
                val startTime = System.currentTimeMillis()
                
                // Warm-up
                repeat(10) {
                    featureExtractor.extractFeatures(audio)
                }
                
                // Run benchmark
                val startBenchmark = System.currentTimeMillis()
                repeat(iterations) {
                    featureExtractor.extractFeatures(audio)
                }
                val endBenchmark = System.currentTimeMillis()
                
                val totalTime = endBenchmark - startTime
                val benchmarkTime = endBenchmark - startBenchmark
                val avgTime = benchmarkTime.toFloat() / iterations
                
                mapOf(
                    "iterations" to iterations,
                    "total_time_ms" to totalTime,
                    "avg_time_ms" to avgTime,
                    "fps" to (1000f / avgTime)
                )
            }
            
            val resultText = """
                Benchmark Results:
                Iterations: ${results["iterations"]}
                Total time: ${results["total_time_ms"]} ms
                Average time: ${String.format("%.2f", results["avg_time_ms"])} ms
                FPS: ${String.format("%.2f", results["fps"])}
            """.trimIndent()
            
            statusText.text = resultText
            
            // Save benchmark results
            saveBenchmarkResults(resultText)
        }
    }
    
    private fun calculateRms(buffer: ShortArray, length: Int): Double {
        var sum = 0.0
        for (i in 0 until length) {
            sum += buffer[i] * buffer[i]
        }
        return kotlin.math.sqrt(sum / length)
    }
    
    private fun saveFeaturesToFile(features: FloatArray) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "audio_features_${timeStamp}.txt"
            )
            
            file.bufferedWriter().use { writer ->
                writer.write("MFCC Features:\n")
                features.forEachIndexed { index, value ->
                    writer.write("${index + 1}: $value\n")
                }
            }
            
            Log.d("AudioTest", "Features saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("AudioTest", "Error saving features to file", e)
        }
    }
    
    private fun saveBenchmarkResults(results: String) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "benchmark_results_${timeStamp}.txt"
            )
            
            file.writeText(results)
            Log.d("AudioTest", "Benchmark results saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("AudioTest", "Error saving benchmark results", e)
        }
    }
    
    private fun updateUI() {
        recordButton.text = if (isRecording) "Stop Recording" else "Start Recording"
        processButton.isEnabled = !isRecording && audioData.isNotEmpty()
        benchmarkButton.isEnabled = !isRecording && audioData.isNotEmpty()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        scope.cancel()
    }
}
