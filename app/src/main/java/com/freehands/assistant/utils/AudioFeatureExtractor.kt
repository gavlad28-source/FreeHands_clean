package com.freehands.assistant.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/**
 * Extracts audio features for voice recognition and analysis.
 */
class AudioFeatureExtractor {
    
    companion object {
        private const val TAG = "AudioFeatureExtractor"
        private const val LIBRARY_NAME = "audio_processing"
        
        init {
            System.loadLibrary(LIBRARY_NAME)
        }
        
        @JvmStatic
        private external fun nativeInit(): Long
        
        @JvmStatic
        private external fun nativeRelease(handle: Long)
        
        @JvmStatic
        private external fun nativeComputeMfcc(
            audioData: FloatArray,
            numSamples: Int,
            sampleRate: Int,
            numMfcc: Int,
            numFilters: Int
        ): FloatArray
        
        @JvmStatic
        private external fun nativeFft(input: FloatArray, n: Int): FloatArray
        
        private const val SAMPLE_RATE = 16000 // 16kHz sample rate
        private const val WINDOW_SIZE = 1024 // FFT window size (must be power of 2)
        private const val HOP_SIZE = 512 // Hop size for FFT
        private const val NUM_MFCC = 13 // Number of MFCC coefficients to extract
        private const val NUM_FILTERS = 26 // Number of Mel filters
        private const val MIN_FREQ = 0.0f // Minimum frequency in Hz
        private const val MAX_FREQ = SAMPLE_RATE / 2.0f // Maximum frequency (Nyquist)
        
        // Constants for input validation
        private const val MIN_AUDIO_LENGTH_MS = 20L // Minimum audio length in milliseconds
        private const val MAX_AUDIO_LENGTH_MS = 30000L // Maximum audio length in milliseconds
        
        init {
            // Validate constants at class loading time
            require(WINDOW_SIZE > 0 && (WINDOW_SIZE and (WINDOW_SIZE - 1)) == 0) {
                "WINDOW_SIZE must be a power of 2"
            }
            require(HOP_SIZE > 0 && HOP_SIZE <= WINDOW_SIZE) {
                "HOP_SIZE must be positive and <= WINDOW_SIZE"
            }
            require(NUM_MFCC > 0) { "NUM_MFCC must be positive" }
            require(NUM_FILTERS > 0) { "NUM_FILTERS must be positive" }
            require(MIN_FREQ >= 0) { "MIN_FREQ must be non-negative" }
            require(MAX_FREQ > MIN_FREQ) { "MAX_FREQ must be greater than MIN_FREQ" }
        }
    }
    
    // Pre-computed Mel filter banks
    private val melFilterBank = createMelFilterBank()
    
    // Pre-computed DCT matrix for MFCC
    private val dctMatrix = createDctMatrix(NUM_MFCC, NUM_FILTERS)
    
    // Native handle
    private val nativeHandle: Long = nativeInit()
    
    // Pre-computed Hamming window
    private val hammingWindow: FloatArray

    init {
        // Pre-compute constants as compile-time values
        val window = FloatArray(WINDOW_SIZE)
        val twoPi = 6.283185307179586f  // 2 * PI
        val scale = 1.0f / (WINDOW_SIZE - 1)
        
        // Unroll the loop for better performance
        var i = 0
        while (i < WINDOW_SIZE) {
            val term = twoPi * i * scale
            val cosine = cos(term)
            window[i] = 0.54f - 0.46f * cosine
            i++
        }
        hammingWindow = window
    }
    
    /**
     * Extracts audio features from the given audio data.
     * @param audioData The audio samples (16-bit PCM)
     * @param sampleRate The sample rate of the audio data
     * @return A feature vector representing the audio
     */
    /**
     * Extracts audio features from the given audio data.
     * @param audioData The audio samples (16-bit PCM)
     * @param sampleRate The sample rate of the audio data (must be positive)
     * @return A feature vector representing the audio
     * @throws IllegalArgumentException if input parameters are invalid
     * @throws IllegalStateException if the audio processing fails
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun extractFeatures(audioData: ShortArray, sampleRate: Int = SAMPLE_RATE): FloatArray {
        // Input validation
        require(audioData.isNotEmpty()) { "Audio data cannot be empty" }
        require(sampleRate > 0) { "Sample rate must be positive" }
        
        val audioLengthMs = (audioData.size * 1000L) / sampleRate
        require(audioLengthMs >= MIN_AUDIO_LENGTH_MS) {
            "Audio is too short: ${audioLengthMs}ms (minimum $MIN_AUDIO_LENGTH_MS ms required)"
        }
        require(audioLengthMs <= MAX_AUDIO_LENGTH_MS) {
            "Audio is too long: ${audioLengthMs}ms (maximum $MAX_AUDIO_LENGTH_MS ms allowed)"
        }
        // Convert to float and normalize to [-1, 1] in a single pass
        val floatAudio = FloatArray(audioData.size) { audioData[it] / 32768.0f }
        
        // Apply pre-emphasis
        preEmphasis(floatAudio)
        
        // Use native implementation for MFCC computation
        return try {
            nativeComputeMfcc(
                audioData = floatAudio,
                numSamples = floatAudio.size,
                sampleRate = SAMPLE_RATE,
                numMfcc = NUM_MFCC,
                numFilters = NUM_FILTERS
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in native MFCC computation: ${e.message}")
            // Fallback to Kotlin implementation if native fails
            computeMfccFallback(floatAudio)
        }
    }
    
    private fun computeMfccFallback(audioData: FloatArray): FloatArray {
        // Frame the signal
        val frames = frameSignal(audioData, WINDOW_SIZE, HOP_SIZE)
        
        // Compute MFCC for each frame
        val mfccFrames = Array(frames.size) { FloatArray(NUM_MFCC) }
        for (i in frames.indices) {
            val frame = frames[i]
            
            // Apply Hamming window
            for (j in frame.indices) {
                frame[j] *= hammingWindow[j]
            }
            
            // Compute MFCC
            val mfcc = computeMfcc(frame, SAMPLE_RATE)
            System.arraycopy(mfcc, 0, mfccFrames[i], 0, NUM_MFCC)
        }
        
        // For now, just return the first frame's MFCCs
        return mfccFrames[0]
    }
    
    protected fun finalize() {
        nativeRelease(nativeHandle)
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
        // Use native FFT implementation
        val fftResult = nativeFft(frame, frame.size)
        val powerSpectrum = FloatArray(frame.size / 2)
        
        // Compute power spectrum
        var i = 0
        var j = 0
        while (i < powerSpectrum.size) {
            val re = fftResult[j++]
            val im = fftResult[j++]
            powerSpectrum[i++] = re * re + im * im
        }
        
        // Get arrays from pools
        val melEnergies = melEnergiesPool.acquire()
        val mfcc = mfccPool.acquire()
        
        try {
            // Compute Mel energies
            for (i in 0 until NUM_FILTERS) {
                var energy = 0.0f
                val filter = melFilterBank[i]
                for (j in powerSpectrum.indices) {
                    energy += powerSpectrum[j] * filter[j]
                }
                melEnergies[i] = ln(maxOf(energy, 1e-10f))
            }
            
            // Apply DCT to get MFCCs with optimized loop
            for (i in 0 until NUM_MFCC) {
                var sum = 0.0f
                val dctRow = dctMatrix[i]
                for (j in 0 until NUM_FILTERS) {
                    sum += dctRow[j] * melEnergies[j]
                }
                mfcc[i] = sum
            }

            // Create a copy to return, as we need to release our pooled array
            return mfcc.copyOf()
        } finally {
            // Release all arrays back to their pools
            melEnergiesPool.release(melEnergies)
            mfccPool.release(mfcc)
        }
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
    
    // Precomputed log constants as doubles to avoid Float division issues
    private val LOG2_E = 1.4426950408889634  // 1/ln(2)
    private val LOG10_E = 0.4342944819032518  // 1/ln(10)
    private val LN_2 = 0.6931471805599453    // ln(2)
    
    // Using strictfp and Java's Math for maximum compatibility
    @Strictfp
    private fun log2(x: Float): Float {
        return if (x <= 0f) {
            Float.NEGATIVE_INFINITY
        } else {
            val d = x.toDouble()
            (Math.log(d) * LOG2_E).toFloat()
        }
    }
    
    // Using bitwise operation for integer log2 (faster and more reliable)
    private fun log2(n: Int): Int {
        return when {
            n <= 0 -> 0
            else -> 31 - Integer.numberOfLeadingZeros(n)
        }
    }
    
    // Using Java's Math with explicit type handling
    @Strictfp
    private fun log10(x: Float): Float {
        return if (x <= 0f) {
            Float.NaN
        } else {
            val d = x.toDouble()
            Math.log10(d).toFloat()
        }
    }
}
