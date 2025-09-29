package com.freehands.assistant.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.*

/**
 * Extracts audio features for voice recognition and analysis.
 */
class AudioFeatureExtractor {
    
    companion object {
        private const val TAG = "AudioFeatureExtractor"
        private const val SAMPLE_RATE = 16000 // 16kHz sample rate
        private const val WINDOW_SIZE = 1024 // FFT window size
        private const val HOP_SIZE = 512 // Hop size for FFT
        private const val NUM_MFCC = 13 // Number of MFCC coefficients to extract
        private const val NUM_FILTERS = 26 // Number of Mel filters
        private const val MIN_FREQ = 0.0f // Minimum frequency in Hz
        private const val MAX_FREQ = SAMPLE_RATE / 2.0f // Maximum frequency (Nyquist)
    }
    
    // Pre-computed Mel filter banks
    private val melFilterBank = createMelFilterBank()
    
    // Pre-computed DCT matrix for MFCC
    private val dctMatrix = createDctMatrix(NUM_MFCC, NUM_FILTERS)
    
    // Pre-computed Hamming window
    private val hammingWindow = FloatArray(WINDOW_SIZE) {
        0.54f - 0.46f * cos(2.0f * Math.PI.toFloat() * it / (WINDOW_SIZE - 1))
    }
    
    /**
     * Extracts audio features from the given audio data.
     * @param audioData The audio samples (16-bit PCM)
     * @param sampleRate The sample rate of the audio data
     * @return A feature vector representing the audio
     */
    fun extractFeatures(audioData: ShortArray, sampleRate: Int = SAMPLE_RATE): FloatArray {
        // Convert to float and normalize to [-1, 1]
        val floatSamples = audioData.map { it.toFloat() / 32768.0f }.toFloatArray()
        
        // Split into frames with 50% overlap
        val frames = frameSignal(floatSamples, WINDOW_SIZE, HOP_SIZE)
        
        // Apply Hamming window to each frame
        val windowedFrames = frames.map { frame ->
            frame.mapIndexed { i, sample ->
                sample * hammingWindow[i]
            }.toFloatArray()
        }
        
        // Compute MFCCs for each frame
        val mfccs = windowedFrames.map { frame ->
            computeMfcc(frame, sampleRate)
        }
        
        // Compute deltas and delta-deltas
        val deltas = computeDeltas(mfccs)
        val deltaDeltas = computeDeltas(deltas)
        
        // Concatenate all features
        val features = mutableListOf<Float>()
        for (i in mfccs.indices) {
            features.addAll(mfccs[i].toList())
            features.addAll(deltas[i].toList())
            features.addAll(deltaDeltas[i].toList())
        }
        
        return features.toFloatArray()
    }
    
    /**
     * Computes the similarity between two feature vectors.
     * @param features1 First feature vector
     * @param features2 Second feature vector
     * @return A similarity score between 0 and 1
     */
    fun calculateSimilarity(features1: FloatArray, features2: FloatArray): Float {
        // Simple cosine similarity for now
        val minLength = minOf(features1.size, features2.size)
        if (minLength == 0) return 0f
        
        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f
        
        for (i in 0 until minLength) {
            dotProduct += features1[i] * features2[i]
            norm1 += features1[i] * features1[i]
            norm2 += features2[i] * features2[i]
        }
        
        norm1 = sqrt(norm1)
        norm2 = sqrt(norm2)
        
        return if (norm1 > 0 && norm2 > 0) {
            (dotProduct / (norm1 * norm2)).coerceIn(-1f, 1f)
        } else {
            0f
        }
    }
    
    private fun frameSignal(
        signal: FloatArray,
        frameSize: Int,
        hopSize: Int
    ): List<FloatArray> {
        val frames = mutableListOf<FloatArray>()
        var position = 0
        
        while (position + frameSize <= signal.size) {
            val frame = signal.copyOfRange(position, position + frameSize)
            frames.add(frame)
            position += hopSize
        }
        
        return frames
    }
    
    private fun computeMfcc(frame: FloatArray, sampleRate: Int): FloatArray {
        // Compute power spectrum
        val fft = fft(frame)
        val powerSpectrum = FloatArray(fft.size / 2) { i ->
            val re = fft[2 * i]
            val im = fft[2 * i + 1]
            re * re + im * im
        }
        
        // Apply Mel filter bank
        val melEnergies = FloatArray(NUM_FILTERS) { 0.0f }
        
        for (i in 0 until NUM_FILTERS) {
            for (j in powerSpectrum.indices) {
                melEnergies[i] += powerSpectrum[j] * melFilterBank[i][j]
            }
            // Log mel energies
            melEnergies[i] = ln(max(1e-10f, melEnergies[i])).toFloat()
        }
        
        // Apply DCT to get MFCCs
        val mfcc = FloatArray(NUM_MFCC) { i ->
            var sum = 0.0f
            for (j in 0 until NUM_FILTERS) {
                sum += dctMatrix[i][j] * melEnergies[j]
            }
            sum
        }
        
        return mfcc
    }
    
    private fun fft(input: FloatArray): FloatArray {
        val n = input.size
        
        // Check if input is a power of 2
        if ((n and (n - 1)) != 0) {
            throw IllegalArgumentException("Input size must be a power of 2")
        }
        
        // Bit-reversal permutation
        val rev = IntArray(n)
        var j = 0
        
        for (i in 0 until n) {
            rev[i] = j
            var k = n shr 1
            while (j >= k) {
                j -= k
                k = k shr 1
            }
            j += k
        }
        
        // Copy with bit-reversed indices
        val output = FloatArray(2 * n)
        for (i in 0 until n) {
            output[2 * i] = input[rev[i]]
            output[2 * i + 1] = 0f
        }
        
        // Cooley-Tukey FFT
        for (s in 1..log2(n)) {
            val m = 1 shl s
            val m2 = m / 2
            
            // Precompute twiddle factors
            val wRe = cos(-2.0 * Math.PI / m).toFloat()
            val wIm = sin(-2.0 * Math.PI / m).toFloat()
            
            for (k in 0 until n step m) {
                var wReCurrent = 1.0f
                var wImCurrent = 0.0f
                
                for (j in 0 until m2) {
                    val tRe = wReCurrent * output[2 * (k + j + m2)] - 
                             wImCurrent * output[2 * (k + j + m2) + 1]
                    val tIm = wReCurrent * output[2 * (k + j + m2) + 1] + 
                             wImCurrent * output[2 * (k + j + m2)]
                    
                    val uRe = output[2 * (k + j)]
                    val uIm = output[2 * (k + j) + 1]
                    
                    // Butterfly operation
                    output[2 * (k + j)] = uRe + tRe
                    output[2 * (k + j) + 1] = uIm + tIm
                    output[2 * (k + j + m2)] = uRe - tRe
                    output[2 * (k + j + m2) + 1] = uIm - tIm
                    
                    // Update twiddle factors
                    val newWRe = wReCurrent * wRe - wImCurrent * wIm
                    val newWIm = wReCurrent * wIm + wImCurrent * wRe
                    wReCurrent = newWRe
                    wImCurrent = newWIm
                }
            }
        }
        
        return output
    }
    
    private fun createMelFilterBank(): Array<FloatArray> {
        val melMin = hzToMel(MIN_FREQ)
        val melMax = hzToMel(MAX_FREQ)
        
        // Create Mel-spaced frequencies
        val melPoints = FloatArray(NUM_FILTERS + 2) { i ->
            melMin + i * (melMax - melMin) / (NUM_FILTERS + 1)
        }
        
        // Convert Mel frequencies back to Hz
        val binPoints = melPoints.map { melToHz(it) }
        
        // Create filter bank
        val filterBank = Array(NUM_FILTERS) { FloatArray(WINDOW_SIZE / 2) }
        val fftFreqs = FloatArray(WINDOW_SIZE / 2) { i ->
            i * SAMPLE_RATE.toFloat() / WINDOW_SIZE
        }
        
        for (i in 0 until NUM_FILTERS) {
            val left = binPoints[i]
            val center = binPoints[i + 1]
            val right = binPoints[i + 2]
            
            for (j in fftFreqs.indices) {
                val freq = fftFreqs[j]
                
                if (freq <= left || freq >= right) {
                    filterBank[i][j] = 0.0f
                } else if (freq <= center) {
                    filterBank[i][j] = (freq - left) / (center - left)
                } else {
                    filterBank[i][j] = (right - freq) / (right - center)
                }
            }
        }
        
        return filterBank
    }
    
    private fun createDctMatrix(numFilters: Int, numCepstra: Int): Array<FloatArray> {
        return Array(numCepstra) { i ->
            FloatArray(numFilters) { j ->
                sqrt(2.0f / numFilters) * 
                cos(Math.PI * i * (2 * j + 1) / (2.0f * numFilters)).toFloat()
            }
        }
    }
    
    private fun hzToMel(hz: Float): Float {
        return 2595.0f * log10(1.0f + hz / 700.0f)
    }
    
    private fun melToHz(mel: Float): Float {
        return 700.0f * (exp(mel / 1127.0f) - 1.0f)
    }
    
    private fun computeDeltas(features: List<FloatArray>): List<FloatArray> {
        if (features.size < 2) {
            return features.map { FloatArray(it.size) }
        }
        
        val deltas = mutableListOf<FloatArray>()
        
        // First frame
        deltas.add(FloatArray(features[0].size) { 0f })
        
        // Middle frames
        for (i in 1 until features.size - 1) {
            val delta = FloatArray(features[i].size)
            for (j in delta.indices) {
                delta[j] = (features[i + 1][j] - features[i - 1][j]) / 2.0f
            }
            deltas.add(delta)
        }
        
        // Last frame
        if (features.size > 1) {
            deltas.add(FloatArray(features.last().size) { 0f })
        }
        
        return deltas
    }
    
    private fun log2(n: Int): Int {
        return 31 - Integer.numberOfLeadingZeros(n)
    }
    
    private fun log10(x: Float): Float {
        return (ln(x.toDouble()) / ln(10.0)).toFloat()
    }
}
