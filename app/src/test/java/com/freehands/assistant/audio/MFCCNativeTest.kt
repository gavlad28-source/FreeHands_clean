package com.freehands.assistant.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MFCCNativeTest {
    
    companion object {
        init {
            System.loadLibrary("audio_processing")
        }
    }
    
    private external fun nativeExtractMFCC(
        audioData: FloatArray,
        sampleRate: Int,
        numCoefficients: Int,
        result: FloatArray
    )
    
    @Before
    fun setup() {
        // Initialize any required native resources
    }
    
    @Test
    fun testMFCC_WithSilence() {
        val frameSize = 512
        val numCoefficients = 13
        val sampleRate = 16000
        
        val silence = FloatArray(frameSize) { 0f }
        val mfccResult = FloatArray(numCoefficients)
        
        nativeExtractMFCC(silence, sampleRate, numCoefficients, mfccResult)
        
        // With silence, coefficients should be very small but not NaN or Inf
        mfccResult.forEach { coeff ->
            assertFalse(coeff.isNaN())
            assertFalse(coeff.isInfinite())
        }
    }
    
    @Test
    fun testMFCC_WithTone() {
        val frameSize = 512
        val numCoefficients = 13
        val sampleRate = 16000
        val frequency = 1000f // 1kHz tone
        
        // Generate a 1kHz sine wave
        val tone = FloatArray(frameSize) { i ->
            Math.sin(2.0 * Math.PI * frequency * i / sampleRate).toFloat()
        }
        
        val mfccResult = FloatArray(numCoefficients)
        nativeExtractMFCC(tone, sampleRate, numCoefficients, mfccResult)
        
        // We expect the energy (first coefficient) to be higher than silence
        assertTrue(mfccResult[0] > -50) // In log scale, should be closer to 0
        
        // Other coefficients should have reasonable values
        for (i in 1 until numCoefficients) {
            assertFalse(mfccResult[i].isNaN())
            assertFalse(mfccResult[i].isInfinite())
        }
    }
}
