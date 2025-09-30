package com.freehands.assistant.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

@RunWith(AndroidJUnit4::class)
class AudioFeatureExtractorTest {
    private lateinit var audioFeatureExtractor: AudioFeatureExtractor
    
    @Before
    fun setup() {
        audioFeatureExtractor = AudioFeatureExtractor()
    }
    
    @After
    fun cleanup() {
        // Clean up any resources if needed
    }
    
    @Test
    fun testExtractFeatures_WithSilence_ReturnsExpectedFeatures() {
        // Create a silent audio buffer (all zeros)
        val silentAudio = ShortArray(16000) { 0 } // 1 second of silence at 16kHz
        
        // Extract features
        val features = audioFeatureExtractor.extractFeatures(silentAudio)
        
        // Verify the output
        assertNotNull("Features should not be null", features)
        assertEquals("Should return expected number of MFCC features", 
            AudioFeatureExtractor.NUM_MFCC, features.size)
        
        // For silence, MFCCs should be close to zero (except possibly the first one)
        for (i in 1 until features.size) {
            assertTrue("MFCC coefficient $i should be close to zero", 
                abs(features[i]) < 1e-6f)
        }
    }
    
    @Test
    fun testExtractFeatures_WithSineWave_ReturnsExpectedFeatures() {
        // Generate a 1kHz sine wave
        val sampleRate = 16000
        val frequency = 1000f // 1kHz
        val duration = 1.0f // 1 second
        val samples = (sampleRate * duration).toInt()
        val sineWave = generateSineWave(frequency, sampleRate, samples)
        
        // Extract features
        val features = audioFeatureExtractor.extractFeatures(sineWave)
        
        // Verify the output
        assertNotNull("Features should not be null", features)
        assertEquals("Should return expected number of MFCC features", 
            AudioFeatureExtractor.NUM_MFCC, features.size)
        
        // For a pure tone, we expect certain MFCCs to be more prominent
        // This is a basic check - in practice, you'd want to verify the exact expected values
        val energy = features.map { it * it }.sum()
        assertTrue("MFCCs should have non-zero energy", energy > 0)
    }
    
    @Test
    fun testExtractFeatures_WithDifferentInputLengths_HandlesGracefully() {
        // Test with various input lengths, including edge cases
        val testLengths = listOf(
            256,  // Very short
            1024, // One FFT window
            2048, // Multiple windows
            16000 // 1 second at 16kHz
        )
        
        for (length in testLengths) {
            val testSignal = ShortArray(length) { (Short.MAX_VALUE * sin(2 * Math.PI * 1000 * it / 16000)).toInt().toShort() }
            
            try {
                val features = audioFeatureExtractor.extractFeatures(testSignal)
                assertNotNull("Features should not be null for length $length", features)
                assertEquals("Should return expected number of MFCC features for length $length",
                    AudioFeatureExtractor.NUM_MFCC, features.size)
            } catch (e: Exception) {
                fail("Should not throw exception for length $length: ${e.message}")
            }
        }
    }
    
    @Test
    fun testExtractFeatures_WithRealAudioData_ReturnsValidFeatures() {
        // This test requires a recorded audio file or a way to generate realistic test data
        // For now, we'll use a generated signal that approximates speech
        val sampleRate = 16000
        val duration = 1.0f // 1 second
        val samples = (sampleRate * duration).toInt()
        
        // Generate a more complex signal that approximates speech
        val speechLikeSignal = generateSpeechLikeSignal(sampleRate, samples)
        
        // Extract features
        val features = audioFeatureExtractor.extractFeatures(speechLikeSignal)
        
        // Verify the output
        assertNotNull("Features should not be null", features)
        assertEquals("Should return expected number of MFCC features", 
            AudioFeatureExtractor.NUM_MFCC, features.size)
        
        // Check that the features are within reasonable bounds
        for (i in features.indices) {
            assertTrue("MFCC coefficient $i is within reasonable bounds",
                features[i].isFinite() && abs(features[i]) < 1000f)
        }
    }
    
    @Test
    fun testExtractFeatures_WithExtremeValues_HandlesGracefully() {
        // Test with maximum and minimum possible values
        val maxSignal = ShortArray(16000) { Short.MAX_VALUE }
        val minSignal = ShortArray(16000) { Short.MIN_VALUE }
        
        // Test with maximum values
        val maxFeatures = audioFeatureExtractor.extractFeatures(maxSignal)
        assertNotNull("Features should not be null for max signal", maxFeatures)
        
        // Test with minimum values
        val minFeatures = audioFeatureExtractor.extractFeatures(minSignal)
        assertNotNull("Features should not be null for min signal", minFeatures)
        
        // The features should be different for max and min signals
        var different = false
        for (i in maxFeatures.indices) {
            if (abs(maxFeatures[i] - minFeatures[i]) > 1e-6f) {
                different = true
                break
            }
        }
        assertTrue("Features should be different for max and min signals", different)
    }
    
    @Test
    fun testExtractFeatures_WithRandomNoise_ReturnsValidFeatures() {
        // Generate random noise
        val random = java.util.Random(42) // Fixed seed for reproducibility
        val noiseSignal = ShortArray(16000) { (random.nextGaussian() * 10000).toInt().toShort() }
        
        // Extract features
        val features = audioFeatureExtractor.extractFeatures(noiseSignal)
        
        // Verify the output
        assertNotNull("Features should not be null", features)
        assertEquals("Should return expected number of MFCC features", 
            AudioFeatureExtractor.NUM_MFCC, features.size)
        
        // Check that the features are within reasonable bounds
        val energy = features.map { it * it }.sum()
        assertTrue("MFCCs should have non-zero energy for noise input", energy > 0)
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
    
    @Test
    fun testPreEmphasis_WithTestSignal_AppliesFilterCorrectly() {
        // Create a test signal: [1, 2, 3, 4, 5, 4, 3, 2, 1]
        val input = shortArrayOf(1000, 2000, 3000, 4000, 5000, 4000, 3000, 2000, 1000)
        val expected = FloatArray(input.size)
        
        // Apply pre-emphasis with alpha = 0.97 (default)
        val alpha = 0.97f
        expected[0] = input[0].toFloat()
        for (i in 1 until input.size) {
            expected[i] = input[i] - alpha * input[i-1]
        }
        
        // Convert to float for processing
        val floatInput = FloatArray(input.size) { input[it].toFloat() }
        
        // Apply pre-emphasis using the class method (needs to be made internal/visible for testing)
        val method = AudioFeatureExtractor::class.java.getDeclaredMethod("preEmphasis", FloatArray::class.java)
        method.isAccessible = true
        method.invoke(audioFeatureExtractor, floatInput as Any)
        
        // Compare results
        assertArrayEquals("Pre-emphasis not applied correctly", expected, floatInput, 1e-6f)
    }
    
    @Test
    fun testFrameSignal_WithTestSignal_DividesCorrectly() {
        // Create a test signal: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        val signal = FloatArray(10) { (it + 1).toFloat() }
        val frameSize = 4
        val hopSize = 2
        
        // Expected frames:
        // Frame 1: [1, 2, 3, 4]
        // Frame 2: [3, 4, 5, 6]
        // Frame 3: [5, 6, 7, 8]
        // Frame 4: [7, 8, 9, 10]
        
        // Get the method using reflection (needs to be made internal/visible for testing)
        val method = AudioFeatureExtractor::class.java.getDeclaredMethod(
            "frameSignal", 
            FloatArray::class.java, 
            Int::class.java, 
            Int::class.java
        )
        method.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val frames = method.invoke(audioFeatureExtractor, signal, frameSize, hopSize) as List<FloatArray>
        
        // Verify the number of frames
        assertEquals("Incorrect number of frames", 4, frames.size)
        
        // Verify each frame
        assertArrayEquals("Frame 1 is incorrect", 
            floatArrayOf(1f, 2f, 3f, 4f), frames[0], 1e-6f)
        assertArrayEquals("Frame 2 is incorrect", 
            floatArrayOf(3f, 4f, 5f, 6f), frames[1], 1e-6f)
        assertArrayEquals("Frame 3 is incorrect", 
            floatArrayOf(5f, 6f, 7f, 8f), frames[2], 1e-6f)
        assertArrayEquals("Frame 4 is incorrect", 
            floatArrayOf(7f, 8f, 9f, 10f), frames[3], 1e-6f)
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
