package com.freehands.assistant;

import android.content.Context;
import android.util.Log;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WakeWordDetector {
    private static final String TAG = "WakeWordDetector";
    
    private final Context context;
    private final String wakeWord;
    private final ExecutorService executorService;
    
    // Audio processing parameters
    private static final int SAMPLE_RATE = 16000;
    private static final int FRAME_SIZE = 1024;
    private static final float DETECTION_THRESHOLD = 0.7f;
    
    // Wake word detection state
    private final AudioBuffer audioBuffer;
    private final WakeWordMatcher wakeWordMatcher;
    
    public interface WakeWordCallback {
        void onWakeWordDetected(float confidence);
        void onError(String error);
    }
    
    public WakeWordDetector(Context context, String wakeWord) {
        this.context = context;
        this.wakeWord = wakeWord.toLowerCase();
        this.executorService = Executors.newSingleThreadExecutor();
        this.audioBuffer = new AudioBuffer(SAMPLE_RATE * 5); // 5 second buffer
        this.wakeWordMatcher = new WakeWordMatcher(this.wakeWord);
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "WakeWordDetector initialized for: " + wakeWord); }

    }
    
    public void processAudio(byte[] audioData, int length, WakeWordCallback callback) {
        if (audioData == null || length <= 0) {
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Convert byte array to short array
                short[] audioSamples = new short[length / 2];
                for (int i = 0; i < audioSamples.length && (i * 2 + 1) < length; i++) {
                    audioSamples[i] = (short) ((audioData[i * 2 + 1] << 8) | (audioData[i * 2] & 0xFF));
                }
                
                // Add to circular buffer
                audioBuffer.addSamples(audioSamples);
                
                // Process for wake word detection
                detectWakeWord(callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing audio for wake word detection", e);
                callback.onError("Audio processing error: " + e.getMessage());
            }
        });
    }
    
    private void detectWakeWord(WakeWordCallback callback) {
        try {
            // Get recent audio data
            short[] recentAudio = audioBuffer.getRecentSamples(SAMPLE_RATE * 3); // Last 3 seconds
            
            if (recentAudio.length < FRAME_SIZE) {
                return; // Not enough data
            }
            
            // Extract features and match against wake word pattern
            float confidence = wakeWordMatcher.matchWakeWord(recentAudio);
            
            if (confidence >= DETECTION_THRESHOLD) {
                if (BuildConfig.DEBUG) { Log.d(TAG, "Wake word detected with confidence: " + confidence); }

                callback.onWakeWordDetected(confidence);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in wake word detection", e);
            callback.onError("Wake word detection error: " + e.getMessage());
        }
    }
    
    /**
     * Circular audio buffer for storing recent audio samples
     */
    private static class AudioBuffer {
        private final short[] buffer;
        private int writeIndex = 0;
        private boolean filled = false;
        
        public AudioBuffer(int size) {
            this.buffer = new short[size];
        }
        
        public synchronized void addSamples(short[] samples) {
            for (short sample : samples) {
                buffer[writeIndex] = sample;
                writeIndex = (writeIndex + 1) % buffer.length;
                
                if (writeIndex == 0) {
                    filled = true;
                }
            }
        }
        
        public synchronized short[] getRecentSamples(int count) {
            if (!filled && writeIndex < count) {
                return Arrays.copyOf(buffer, writeIndex);
            }
            
            count = Math.min(count, buffer.length);
            short[] result = new short[count];
            
            int startIndex = filled ? 
                (writeIndex - count + buffer.length) % buffer.length : 
                Math.max(0, writeIndex - count);
            
            for (int i = 0; i < count; i++) {
                result[i] = buffer[(startIndex + i) % buffer.length];
            }
            
            return result;
        }
    }
    
    /**
     * Wake word pattern matching using audio features
     */
    private static class WakeWordMatcher {
        private final String targetPhrase;
        private final double[][] phoneticPatterns;
        
        public WakeWordMatcher(String wakeWord) {
            this.targetPhrase = wakeWord;
            this.phoneticPatterns = generatePhoneticPatterns(wakeWord);
        }
        
        private double[][] generatePhoneticPatterns(String phrase) {
            // Simplified phonetic pattern generation
            // In production, this would use proper phonetic analysis
            
            String[] words = phrase.split("\\s+");
            double[][] patterns = new double[words.length][];
            
            for (int i = 0; i < words.length; i++) {
                patterns[i] = generateWordPattern(words[i]);
            }
            
            return patterns;
        }
        
        private double[] generateWordPattern(String word) {
            // Generate a simple spectral pattern based on word characteristics
            double[] pattern = new double[13]; // MFCC-like features
            
            // Simple pattern based on word characteristics
            int wordHash = word.hashCode();
            for (int i = 0; i < pattern.length; i++) {
                pattern[i] = Math.sin((wordHash + i * 37) * 0.01) * 0.5 + 0.5;
            }
            
            return pattern;
        }
        
        public float matchWakeWord(short[] audioSamples) {
            try {
                // Extract audio features
                double[][] audioFeatures = extractAudioFeatures(audioSamples);
                
                if (audioFeatures.length < phoneticPatterns.length) {
                    return 0.0f;
                }
                
                // Find best match using dynamic time warping (simplified)
                float bestMatch = 0.0f;
                
                int searchWindowSize = audioFeatures.length - phoneticPatterns.length + 1;
                
                for (int offset = 0; offset < searchWindowSize; offset += 5) { // Step by 5 for efficiency
                    float match = calculatePatternMatch(audioFeatures, offset);
                    bestMatch = Math.max(bestMatch, match);
                }
                
                return bestMatch;
                
            } catch (Exception e) {
                Log.e(TAG, "Error matching wake word", e);
                return 0.0f;
            }
        }
        
        private double[][] extractAudioFeatures(short[] audioSamples) {
            // Extract MFCC-like features from audio
            int frameCount = audioSamples.length / FRAME_SIZE;
            double[][] features = new double[frameCount][];
            
            for (int frame = 0; frame < frameCount; frame++) {
                int startIndex = frame * FRAME_SIZE;
                int endIndex = Math.min(startIndex + FRAME_SIZE, audioSamples.length);
                
                features[frame] = extractFrameFeatures(audioSamples, startIndex, endIndex);
            }
            
            return features;
        }
        
        private double[] extractFrameFeatures(short[] audioSamples, int startIndex, int endIndex) {
            double[] features = new double[13];
            
            // Convert to double and apply window
            double[] frame = new double[endIndex - startIndex];
            for (int i = 0; i < frame.length; i++) {
                frame[i] = audioSamples[startIndex + i] / 32768.0;
                // Apply Hamming window
                frame[i] *= 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (frame.length - 1));
            }
            
            // Compute spectral features (simplified)
            for (int i = 0; i < features.length; i++) {
                double energy = 0;
                int binStart = (i * frame.length) / features.length;
                int binEnd = ((i + 1) * frame.length) / features.length;
                
                for (int j = binStart; j < binEnd && j < frame.length; j++) {
                    energy += frame[j] * frame[j];
                }
                
                features[i] = Math.log(energy + 1e-10);
            }
            
            return features;
        }
        
        private float calculatePatternMatch(double[][] audioFeatures, int offset) {
            float totalSimilarity = 0;
            int validComparisons = 0;
            
            for (int patternIndex = 0; patternIndex < phoneticPatterns.length; patternIndex++) {
                int audioIndex = offset + patternIndex;
                
                if (audioIndex < audioFeatures.length) {
                    float similarity = calculateFeatureSimilarity(
                        phoneticPatterns[patternIndex], 
                        audioFeatures[audioIndex]
                    );
                    totalSimilarity += similarity;
                    validComparisons++;
                }
            }
            
            return validComparisons > 0 ? totalSimilarity / validComparisons : 0;
        }
        
        private float calculateFeatureSimilarity(double[] pattern, double[] features) {
            if (pattern.length != features.length) {
                return 0;
            }
            
            double dotProduct = 0;
            double patternNorm = 0;
            double featureNorm = 0;
            
            for (int i = 0; i < pattern.length; i++) {
                dotProduct += pattern[i] * features[i];
                patternNorm += pattern[i] * pattern[i];
                featureNorm += features[i] * features[i];
            }
            
            double denominator = Math.sqrt(patternNorm * featureNorm);
            if (denominator == 0) {
                return 0;
            }
            
            return (float) Math.max(0, dotProduct / denominator);
        }
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}
