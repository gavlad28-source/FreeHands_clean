package com.freehands.assistant.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freehands.assistant.utils.AudioFeatureExtractor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

/**
 * Benchmark for measuring the performance of AudioFeatureExtractor.
 *
 * Run this benchmark on a physical device for accurate results.
 */
@RunWith(AndroidJUnit4::class)
class AudioFeatureExtractorBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    private val sampleRate = 16000
    private val extractor = AudioFeatureExtractor()
    
    @Test
    fun benchmarkExtractFeatures_Silence() = benchmarkExtractFeatures(
        "silence",
        generateSilence(5 * sampleRate) // 5 seconds
    )
    
    @Test
    fun benchmarkExtractFeatures_SineWave() = benchmarkExtractFeatures(
        "sine_wave",
        generateSineWave(1000f, sampleRate, 5 * sampleRate) // 1kHz sine wave, 5 seconds
    )
    
    @Test
    fun benchmarkExtractFeatures_SpeechLike() = benchmarkExtractFeatures(
        "speech_like",
        generateSpeechLikeSignal(sampleRate, 5 * sampleRate) // 5 seconds
    )
    
    @Test
    fun benchmarkExtractFeatures_Noise() = benchmarkExtractFeatures(
        "noise",
        generateNoise(5 * sampleRate) // 5 seconds
    )
    
    private fun benchmarkExtractFeatures(testCase: String, signal: ShortArray) {
        // Warm-up
        repeat(5) {
            extractor.extractFeatures(signal.copyOfRange(0, sampleRate)) // 1 second
        }
        
        // Measure
        val iterations = 5
        val chunkSize = sampleRate // 1 second chunks
        val totalChunks = signal.size / chunkSize
        
        val results = mutableListOf<Double>()
        
        repeat(iterations) { iter ->
            var totalTimeNs = 0L
            
            // Process each 1-second chunk
            for (chunk in 0 until totalChunks) {
                val start = chunk * chunkSize
                val end = start + chunkSize
                val chunkSignal = signal.copyOfRange(start, end)
                
                val startTime = System.nanoTime()
                val features = extractor.extractFeatures(chunkSignal)
                val endTime = System.nanoTime()
                
                // Ensure the result is used to prevent optimization
                check(features.isNotEmpty())
                
                totalTimeNs += (endTime - startTime)
            }
            
            val avgTimeMs = totalTimeNs / (1_000_000.0 * totalChunks)
            results.add(avgTimeMs)
            
            println("Iteration ${iter + 1}: Average processing time = ${String.format("%.2f", avgTimeMs)} ms")
        }
        
        // Calculate statistics
        val min = results.minOrNull() ?: 0.0
        val max = results.maxOrNull() ?: 0.0
        val avg = results.average()
        val stdDev = results.map { (it - avg) * (it - avg) }.average().let { Math.sqrt(it) }
        
        println("\n=== $testCase ===")
        println("Min: ${String.format("%.2f", min)} ms")
        println("Max: ${String.format("%.2f", max)} ms")
        println("Avg: ${String.format("%.2f", avg)} ms")
        println("StdDev: ${String.format("%.4f", stdDev)} ms")
        println("Real-time factor: ${String.format("%.2f", 1000.0 / avg)}x")
        println("Memory usage: ${getMemoryUsage()}")
    }
    
    private fun generateSilence(numSamples: Int): ShortArray {
        return ShortArray(numSamples) { 0 }
    }
    
    private fun generateSineWave(frequency: Float, sampleRate: Int, numSamples: Int): ShortArray {
        val signal = ShortArray(numSamples)
        val twoPiF = 2.0 * PI * frequency / sampleRate
        
        for (i in 0 until numSamples) {
            val sample = sin(twoPiF * i) * 0.8 * Short.MAX_VALUE
            signal[i] = sample.toInt().toShort()
        }
        
        return signal
    }
    
    private fun generateSpeechLikeSignal(sampleRate: Int, numSamples: Int): ShortArray {
        val signal = ShortArray(numSamples)
        val twoPi = 2.0 * PI
        
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
            sample += (Random.nextDouble() - 0.5) * 0.1
            
            // Scale and clip
            sample = sample.coerceIn(-1.0, 1.0)
            signal[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        
        return signal
    }
    
    private fun generateNoise(numSamples: Int): ShortArray {
        return ShortArray(numSamples) { (Random.nextDouble(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }
    }
    
    private fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory
        
        return "Used: ${usedMemory}MB / ${maxMemory}MB (${(usedMemory.toDouble() / maxMemory * 100).toInt()}%)"
    }
}
