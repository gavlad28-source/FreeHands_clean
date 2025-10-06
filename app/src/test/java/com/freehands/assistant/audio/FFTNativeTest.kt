package com.freehands.assistant.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class FFTNativeTest {
    
    companion object {
        init {
            System.loadLibrary("audio_processing")
        }
    }
    
    private external fun nativeFFT(
        input: FloatArray,
        output: FloatArray,
        n: Int
    )
    
    @Before
    fun setup() {
        // Initialize any required native resources
    }
    
    @Test
    fun testFFT_WithSineWave() {
        val n = 8
        val input = FloatArray(n * 2)
        val output = FloatArray(n * 2)
        
        // Create a simple sine wave
        for (i in 0 until n) {
            input[2 * i] = Math.sin(2.0 * Math.PI * i / n).toFloat()
            input[2 * i + 1] = 0f // Imaginary part
        }
        
        // Perform FFT
        nativeFFT(input, output, n)
        
        // Basic validation
        assertNotEquals(0f, output[0], 0.001f) // DC component
        assertNotEquals(0f, output[2], 0.001f) // First frequency bin
    }
    
    @Test
    fun testFFT_WithImpulse() {
        val n = 8
        val input = FloatArray(n * 2) { 0f }
        val output = FloatArray(n * 2)
        
        // Impulse at t=0
        input[0] = 1f
        
        // Perform FFT
        nativeFFT(input, output, n)
        
        // All frequency components should be equal for an impulse
        for (i in 0 until n) {
            assertEquals(1f, Math.hypot(output[2 * i].toDouble(), output[2 * i + 1].toDouble()).toFloat(), 0.001f)
        }
    }
}
