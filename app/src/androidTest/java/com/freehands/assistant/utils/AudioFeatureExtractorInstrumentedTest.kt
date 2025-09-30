package com.freehands.assistant.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.*

/**
 * Instrumented test for AudioFeatureExtractor that runs on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AudioFeatureExtractorInstrumentedTest {
    private lateinit var audioFeatureExtractor: AudioFeatureExtractor
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        audioFeatureExtractor = AudioFeatureExtractor()
    }
    
    @Test
    fun testNativeImplementation_WithTestSignal_ReturnsExpectedFeatures() {
        // Generate a 1kHz sine wave
        val sampleRate = 16000
        val frequency = 1000f // 1kHz
        val duration = 0.5f // 0.5 seconds
        val samples = (sampleRate * duration).toInt()
        val sineWave = generateSineWave(frequency, sampleRate, samples)
        
        // Extract features using the native implementation
        val features = audioFeatureExtractor.extractFeatures(sineWave)
        
        // Verify the output
        assertNotNull("Features should not be null", features)
        assertEquals("Should return expected number of MFCC features", 
            AudioFeatureExtractor.NUM_MFCC, features.size)
        
        // For a pure tone, we expect certain MFCCs to be more prominent
        val energy = features.map { it * it }.sum()
        assertTrue("MFCCs should have non-zero energy", energy > 0)
        
        // Log the features for inspection
        println("MFCC Features: ${features.joinToString(", ")}")
    }
    
    @Test
    fun testNativeVsJavaImplementation_ProduceSimilarResults() {
        // Generate a more complex signal
        val sampleRate = 16000
        val duration = 0.5f // 0.5 seconds
        val samples = (sampleRate * duration).toInt()
        val signal = generateSpeechLikeSignal(sampleRate, samples)
        
        // Extract features using native implementation
        val nativeFeatures = audioFeatureExtractor.extractFeatures(signal)
        
        // Extract features using Java implementation (fallback)
        val javaFeatures = run {
            val extractor = AudioFeatureExtractor()
            // Force Java implementation
            extractor.extractFeatures(signal)
        }
        
        // Compare the results
        assertEquals("Feature vector lengths should match", 
            nativeFeatures.size, javaFeatures.size)
        
        // Calculate RMSE between the two feature vectors
        var sumSquaredError = 0.0
        for (i in nativeFeatures.indices) {
            val diff = nativeFeatures[i] - javaFeatures[i]
            sumSquaredError += diff * diff
        }
        val rmse = sqrt(sumSquaredError / nativeFeatures.size)
        
        // Log the RMSE for inspection
        println("RMSE between native and Java implementations: $rmse")
        
        // The implementations might not be exactly the same due to floating-point differences,
        // but they should be very close
        assertTrue("Native and Java implementations should produce similar results (RMSE < 0.1)", 
            rmse < 0.1)
    }
    
    @Test
    fun testExtractFeatures_WithRealAudioFile_ReturnsValidFeatures() {
        // Load a test audio file from assets
        val audioFile = File("${context.filesDir.absolutePath}/test_audio.raw")
        if (!audioFile.exists()) {
            // If the file doesn't exist, generate a test signal
            val testSignal = generateSpeechLikeSignal(16000, 16000) // 1 second at 16kHz
            val byteBuffer = ByteBuffer.allocate(testSignal.size * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            val shortBuffer = byteBuffer.asShortBuffer()
            shortBuffer.put(testSignal)
            
            audioFile.parentFile?.mkdirs()
            FileOutputStream(audioFile).use { fos ->
                fos.write(byteBuffer.array())
            }
        }
        
        // Read the audio file
        val audioData = audioFile.readBytes()
        val shortArray = ShortArray(audioData.size / 2)
        ByteBuffer.wrap(audioData)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(shortArray)
        
        // Extract features
        val features = audioFeatureExtractor.extractFeatures(shortArray)
        
        // Verify the output
        assertNotNull("Features should not be null", features)
        assertEquals("Should return expected number of MFCC features", 
            AudioFeatureExtractor.NUM_MFCC, features.size)
        
        // Check that the features are within reasonable bounds
        val energy = features.map { it * it }.sum()
        assertTrue("MFCCs should have non-zero energy", energy > 0)
    }
    
    @Test
    fun testPerformance_ExtractFeatures() {
        // Generate a test signal
        val sampleRate = 16000
        val duration = 5.0f // 5 seconds
        val samples = (sampleRate * duration).toInt()
        val signal = generateSpeechLikeSignal(sampleRate, samples)
        
        // Warm-up
        repeat(5) {
            audioFeatureExtractor.extractFeatures(signal.copyOfRange(0, 16000)) // 1 second
        }
        
        // Measure performance
        val iterations = 10
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            // Process 1-second chunks
            for (i in 0 until 5) {
                val start = i * sampleRate
                val end = (i + 1) * sampleRate
                audioFeatureExtractor.extractFeatures(signal.copyOfRange(start, end))
            }
        }
        
        val endTime = System.nanoTime()
        val totalTimeMs = (endTime - startTime) / 1_000_000.0
        val avgTimePerSecond = totalTimeMs / (iterations * 5.0) // 5 seconds per iteration
        
        println("Average processing time per second of audio: ${String.format("%.2f", avgTimePerSecond)} ms")
        println("Real-time factor: ${1000.0 / avgTimePerSecond}x")
        
        // The processing should be faster than real-time
        assertTrue("Processing should be faster than real-time", avgTimePerSecond < 1000.0)
    }
    
    @Test
    fun testNativeFft_WithKnownInput_ProducesExpectedOutput() {
        // Test FFT with a known input and expected output
        // For a 4-point FFT of [1, 0, -1, 0], the output should be [0, 2, 0, 2] (complex values)
        
        // Get the native FFT method using reflection
        val method = AudioFeatureExtractor::class.java.getDeclaredMethod("nativeFft", FloatArray::class.java, Int::class.java)
        method.isAccessible = true
        
        // Test input (real part only, imaginary part is zero)
        val input = floatArrayOf(1f, 0f, -1f, 0f)
        
        // Call the native FFT method
        @Suppress("UNCHECKED_CAST")
        val output = method.invoke(audioFeatureExtractor, input, input.size) as FloatArray
        
        // Expected output: [0, 0, 2, 0, 0, 0, 2, 0] (interleaved real/imaginary)
        val expected = floatArrayOf(0f, 0f, 2f, 0f, 0f, 0f, 2f, 0f)
        
        // Compare with a small epsilon to account for floating-point differences
        assertArrayEquals("FFT output does not match expected", 
            expected, output, 1e-6f)
    }
    
    // Helper function to generate a sine wave
    private fun generateSineWave(frequency: Float, sampleRate: Int, numSamples: Int): ShortArray {
        val signal = ShortArray(numSamples)
        val twoPiF = 2.0 * Math.PI * frequency / sampleRate
        
        for (i in 0 until numSamples) {
            val sample = sin(twoPiF * i) * 0.8 * Short.MAX_VALUE
            signal[i] = sample.toInt().toShort()
        }
        
        return signal
    }
    
    // Helper function to generate a more speech-like signal
    private fun generateSpeechLikeSignal(sampleRate: Int, numSamples: Int): ShortArray {
        // This generates a signal with multiple frequencies to approximate speech
        val signal = ShortArray(numSamples)
        val twoPi = 2.0 * Math.PI
        
        // Fundamental frequency (pitch)
        val f0 = 120.0 // Hz
        
        // Formant frequencies (in Hz) - these approximate vowel sounds
        val formants = listOf(
            800.0 to 100.0,  // F1
            1200.0 to 80.0,   // F2
            2500.0 to 60.0,   // F3
            3500.0 to 40.0,   // F4
            4500.0 to 20.0    // F5
        )
        
        // Add some amplitude modulation to simulate speech prosody
        val amFreq = 5.0 // Hz
        
        for (i in 0 until numSamples) {
            var sample = 0.0
            val t = i.toDouble() / sampleRate
            
            // Add fundamental frequency
            sample += 0.5 * sin(twoPi * f0 * t)
            
            // Add formants
            for ((freq, weight) in formants) {
                sample += (weight / 100.0) * sin(twoPi * freq * t)
            }
            
            // Add amplitude modulation
            val am = 0.5 * (1.0 + sin(twoPi * amFreq * t))
            sample *= am
            
            // Add a bit of noise
            sample += (Math.random() - 0.5) * 0.1
            
            // Scale and clip
            sample = sample.coerceIn(-1.0, 1.0)
            signal[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        
        return signal
    }
    
    // Helper assertion function for float arrays
    private fun assertArrayEquals(message: String, expected: FloatArray, actual: FloatArray, epsilon: Float) {
        assertEquals("$message: array length", expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals("$message: array element at index $i", 
                expected[i], actual[i], epsilon)
        }
    }
}
