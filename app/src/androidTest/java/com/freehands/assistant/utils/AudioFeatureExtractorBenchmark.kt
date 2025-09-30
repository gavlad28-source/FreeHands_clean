package com.freehands.assistant.utils

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Benchmark tests for AudioFeatureExtractor.
 *
 * Run with:
 * ./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.freehands.assistant.utils.AudioFeatureExtractorBenchmark
 */
@RunWith(AndroidJUnit4::class)
class AudioFeatureExtractorBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val extractor = AudioFeatureExtractor()
    
    // Generate test audio data (1 second of 16kHz audio)
    private val testAudio = ShortArray(16000) { i ->
        val t = i.toDouble() / 16000.0
        (Short.MAX_VALUE * 0.8 * sin(2 * Math.PI * 440 * t)).toInt().toShort()
    }

    @Test
    fun benchmarkFeatureExtraction() = runBlocking {
        benchmarkRule.measureRepeated {
            // Warm-up run
            extractor.extractFeatures(testAudio)
            
            // Actual measurement
            val features = extractor.extractFeatures(testAudio)
            
            // Ensure the result is used to prevent optimization
            android.util.Log.d("Benchmark", "Features extracted: ${features.size}")
        }
    }

    @Test
    fun benchmarkMemoryUsage() = runBlocking {
        val memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Run multiple iterations to trigger GC and get stable memory measurements
        repeat(100) {
            extractor.extractFeatures(testAudio)
        }
        
        System.gc()
        val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryUsed = memoryAfter - memoryBefore
        
        android.util.Log.d("Benchmark", "Memory used: ${memoryUsed / 1024} KB")
    }

    @Test
    fun benchmarkRealWorldScenario() = runBlocking {
        // Simulate real-world usage with multiple feature extractions
        benchmarkRule.measureRepeated {
            // Process multiple chunks of audio
            val chunkSize = 2048
            var position = 0
            
            while (position + chunkSize <= testAudio.size) {
                val chunk = testAudio.copyOfRange(position, position + chunkSize)
                val features = extractor.extractFeatures(chunk)
                position += chunkSize / 2 // 50% overlap
                
                // Ensure the result is used
                android.util.Log.v("Benchmark", "Processed chunk, features: ${features.size}")
            }
        }
    }

    private fun saveBenchmarkResults(results: Map<String, Long>) {
        val outputFile = File(context.getExternalFilesDir(null), "benchmark_results.json")
        val json = results.entries.joinToString(
            prefix = "{\n",
            postfix = "\n}",
            separator = ",\n"
        ) { "  \"${it.key}\": ${it.value}" }
        
        FileOutputStream(outputFile).use { it.write(json.toByteArray()) }
    }
}
