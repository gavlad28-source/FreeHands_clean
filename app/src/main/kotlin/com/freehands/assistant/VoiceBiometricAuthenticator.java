package com.freehands.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceBiometricAuthenticator {
    private static final String TAG = "VoiceBiometricAuth";
    private static final String PREFS_NAME = "voice_biometric_prefs";
    private static final String KEY_VOICE_PROFILE = "voice_profile_data";
    private static final String KEY_PROFILE_INITIALIZED = "profile_initialized";
    
    // Audio configuration for voice profiling
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int PROFILE_DURATION_MS = 3000; // 3 seconds for profiling
    private static final int AUTH_DURATION_MS = 2000; // 2 seconds for authentication
    
    // Voice authentication thresholds
    private static final float AUTHENTICATION_THRESHOLD = 0.75f;
    private static final float SIMILARITY_THRESHOLD = 0.8f;
    
    private final Context context;
    private final ExecutorService executorService;
    private SharedPreferences encryptedPrefs;
    private VoiceProfileManager profileManager;
    
    public interface InitializationCallback {
        void onInitialized();
        void onError(String error);
    }
    
    public interface AuthenticationCallback {
        void onAuthenticationSucceeded();
        void onAuthenticationFailed(String reason);
        void onError(String error);
    }
    
    public VoiceBiometricAuthenticator(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.profileManager = new VoiceProfileManager();
        
        initializeEncryptedPreferences();
    }
    
    private void initializeEncryptedPreferences() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            
            encryptedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize encrypted preferences", e);
            // Fallback to regular SharedPreferences (less secure)
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }
    
    public void initializeVoiceProfile(InitializationCallback callback) {
        // Check if profile already exists
        if (encryptedPrefs.getBoolean(KEY_PROFILE_INITIALIZED, false)) {
            if (BuildConfig.DEBUG) { Log.d(TAG, "Voice profile already initialized"); }

            callback.onInitialized();
            return;
        }
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "Starting voice profile initialization"); }

        
        executorService.execute(() -> {
            try {
                // Create voice profile through multiple samples
                createVoiceProfile(callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error during voice profile initialization", e);
                callback.onError("Failed to initialize voice profile: " + e.getMessage());
            }
        });
    }
    
    private void createVoiceProfile(InitializationCallback callback) {
        List<VoiceFeature> voiceFeatures = new ArrayList<>();
        int samplesNeeded = 3; // Multiple samples for better accuracy
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "Creating voice profile with " + samplesNeeded + " samples"); }

        
        for (int i = 0; i < samplesNeeded; i++) {
            if (BuildConfig.DEBUG) { Log.d(TAG, "Recording voice sample " + (i + 1) + "/" + samplesNeeded); }

            
            VoiceFeature feature = recordVoiceSample(PROFILE_DURATION_MS);
            if (feature != null) {
                voiceFeatures.add(feature);
            } else {
                callback.onError("Failed to record voice sample " + (i + 1));
                return;
            }
            
            // Short pause between samples
            try {
                // REMOVED Thread.sleep for responsiveness
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                callback.onError("Voice profiling interrupted");
                return;
            }
        }
        
        if (voiceFeatures.size() >= 2) {
            // Create and store voice profile
            VoiceProfile profile = new VoiceProfile(voiceFeatures);
            saveVoiceProfile(profile);
            
            encryptedPrefs.edit()
                .putBoolean(KEY_PROFILE_INITIALIZED, true)
                .apply();
            
            if (BuildConfig.DEBUG) { Log.d(TAG, "Voice profile created successfully"); }

            callback.onInitialized();
        } else {
            callback.onError("Insufficient voice samples for profile creation");
        }
    }
    
    private VoiceFeature recordVoiceSample(int durationMs) {
        AudioRecord audioRecord = null;
        
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return null;
            }
            
            audioRecord.startRecording();
            
            int totalSamples = (SAMPLE_RATE * durationMs) / 1000;
            short[] audioData = new short[totalSamples];
            int samplesRead = 0;
            
            while (samplesRead < totalSamples) {
                int remaining = totalSamples - samplesRead;
                int toRead = Math.min(bufferSize / 2, remaining);
                
                int result = audioRecord.read(audioData, samplesRead, toRead);
                if (result > 0) {
                    samplesRead += result;
                } else {
                    Log.w(TAG, "AudioRecord read returned: " + result);
                    break;
                }
            }
            
            audioRecord.stop();
            
            if (samplesRead > 0) {
                return extractVoiceFeatures(Arrays.copyOf(audioData, samplesRead));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording voice sample", e);
        } finally {
            if (audioRecord != null) {
                try {
                    audioRecord.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing AudioRecord", e);
                }
            }
        }
        
        return null;
    }
    
    private VoiceFeature extractVoiceFeatures(short[] audioData) {
        try {
            // Convert to double array for processing
            double[] samples = new double[audioData.length];
            for (int i = 0; i < audioData.length; i++) {
                samples[i] = audioData[i] / 32768.0; // Normalize to [-1, 1]
            }
            
            // Extract voice characteristics
            double[] mfccFeatures = extractMFCC(samples);
            double fundamentalFreq = extractFundamentalFrequency(samples);
            double[] spectralFeatures = extractSpectralFeatures(samples);
            double voiceIntensity = calculateRMS(samples);
            
            return new VoiceFeature(mfccFeatures, fundamentalFreq, spectralFeatures, voiceIntensity);
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting voice features", e);
            return null;
        }
    }
    
    private double[] extractMFCC(double[] samples) {
        // Simplified MFCC extraction
        // In a production app, you would use a proper MFCC library
        
        int numCoefficients = 13;
        double[] mfcc = new double[numCoefficients];
        
        // Basic spectral analysis as MFCC approximation
        int frameSize = Math.min(1024, samples.length);
        double[] frame = Arrays.copyOf(samples, frameSize);
        
        // Apply window function
        for (int i = 0; i < frameSize; i++) {
            frame[i] *= 0.5 * (1 - Math.cos(2 * Math.PI * i / (frameSize - 1))); // Hamming window
        }
        
        // Compute power spectrum (simplified)
        for (int i = 0; i < numCoefficients; i++) {
            double sum = 0;
            int start = (i * frameSize) / numCoefficients;
            int end = ((i + 1) * frameSize) / numCoefficients;
            
            for (int j = start; j < end && j < frameSize; j++) {
                sum += frame[j] * frame[j];
            }
            
            mfcc[i] = Math.log(sum + 1e-10); // Log energy
        }
        
        return mfcc;
    }
    
    private double extractFundamentalFrequency(double[] samples) {
        // Simplified pitch detection using autocorrelation
        
        int minPeriod = SAMPLE_RATE / 800; // 800 Hz max
        int maxPeriod = SAMPLE_RATE / 80;  // 80 Hz min
        
        double maxCorrelation = 0;
        int bestPeriod = minPeriod;
        
        for (int period = minPeriod; period < maxPeriod && period < samples.length / 2; period++) {
            double correlation = 0;
            
            for (int i = 0; i < samples.length - period; i++) {
                correlation += samples[i] * samples[i + period];
            }
            
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation;
                bestPeriod = period;
            }
        }
        
        return (double) SAMPLE_RATE / bestPeriod; // Fundamental frequency in Hz
    }
    
    private double[] extractSpectralFeatures(double[] samples) {
        // Extract basic spectral features
        double[] features = new double[4];
        
        // Spectral centroid, bandwidth, rolloff, etc.
        int frameSize = Math.min(1024, samples.length);
        double[] spectrum = new double[frameSize / 2];
        
        // Simplified power spectrum
        for (int i = 0; i < spectrum.length; i++) {
            if (i < samples.length) {
                spectrum[i] = samples[i] * samples[i];
            }
        }
        
        // Spectral centroid
        double weightedSum = 0, totalPower = 0;
        for (int i = 0; i < spectrum.length; i++) {
            weightedSum += i * spectrum[i];
            totalPower += spectrum[i];
        }
        features[0] = totalPower > 0 ? weightedSum / totalPower : 0;
        
        // Other features (simplified)
        features[1] = calculateRMS(samples); // Energy
        features[2] = calculateZeroCrossingRate(samples);
        features[3] = totalPower; // Total spectral power
        
        return features;
    }
    
    private double calculateRMS(double[] samples) {
        double sum = 0;
        for (double sample : samples) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / samples.length);
    }
    
    private double calculateZeroCrossingRate(double[] samples) {
        int crossings = 0;
        for (int i = 1; i < samples.length; i++) {
            if ((samples[i] >= 0) != (samples[i - 1] >= 0)) {
                crossings++;
            }
        }
        return (double) crossings / samples.length;
    }
    
    private void saveVoiceProfile(VoiceProfile profile) {
        try {
            Gson gson = new Gson();
            String profileJson = gson.toJson(profile);
            
            encryptedPrefs.edit()
                .putString(KEY_VOICE_PROFILE, profileJson)
                .apply();
            
            if (BuildConfig.DEBUG) { Log.d(TAG, "Voice profile saved successfully"); }

            
        } catch (Exception e) {
            Log.e(TAG, "Error saving voice profile", e);
        }
    }
    
    private VoiceProfile loadVoiceProfile() {
        try {
            String profileJson = encryptedPrefs.getString(KEY_VOICE_PROFILE, null);
            if (profileJson != null) {
                Gson gson = new Gson();
                return gson.fromJson(profileJson, VoiceProfile.class);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading voice profile", e);
        }
        return null;
    }
    
    public void authenticateVoice(AuthenticationCallback callback) {
        VoiceProfile storedProfile = loadVoiceProfile();
        if (storedProfile == null) {
            callback.onError("No voice profile found. Please initialize first.");
            return;
        }
        
        executorService.execute(() -> {
            try {
                if (BuildConfig.DEBUG) { Log.d(TAG, "Starting voice authentication"); }

                
                VoiceFeature currentFeature = recordVoiceSample(AUTH_DURATION_MS);
                if (currentFeature == null) {
                    callback.onError("Failed to record voice sample for authentication");
                    return;
                }
                
                float similarity = calculateVoiceSimilarity(storedProfile, currentFeature);
                if (BuildConfig.DEBUG) { Log.d(TAG, "Voice similarity: " + similarity); }

                
                if (similarity >= AUTHENTICATION_THRESHOLD) {
                    if (BuildConfig.DEBUG) { Log.d(TAG, "Voice authentication successful"); }

                    callback.onAuthenticationSucceeded();
                } else {
                    Log.w(TAG, "Voice authentication failed - similarity too low: " + similarity);
                    callback.onAuthenticationFailed("Voice pattern does not match registered user");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error during voice authentication", e);
                callback.onError("Authentication error: " + e.getMessage());
            }
        });
    }
    
    private float calculateVoiceSimilarity(VoiceProfile storedProfile, VoiceFeature currentFeature) {
        float totalSimilarity = 0;
        int comparisons = 0;
        
        // Compare against all stored features and take the best match
        for (VoiceFeature storedFeature : storedProfile.getFeatures()) {
            float similarity = compareVoiceFeatures(storedFeature, currentFeature);
            totalSimilarity += similarity;
            comparisons++;
        }
        
        return comparisons > 0 ? totalSimilarity / comparisons : 0;
    }
    
    private float compareVoiceFeatures(VoiceFeature stored, VoiceFeature current) {
        float mfccSimilarity = calculateArraySimilarity(stored.getMfccFeatures(), current.getMfccFeatures());
        float freqSimilarity = calculateFrequencySimilarity(stored.getFundamentalFreq(), current.getFundamentalFreq());
        float spectralSimilarity = calculateArraySimilarity(stored.getSpectralFeatures(), current.getSpectralFeatures());
        float intensitySimilarity = calculateIntensitySimilarity(stored.getVoiceIntensity(), current.getVoiceIntensity());
        
        // Weighted combination of features
        return (mfccSimilarity * 0.4f) + (freqSimilarity * 0.3f) + (spectralSimilarity * 0.2f) + (intensitySimilarity * 0.1f);
    }
    
    private float calculateArraySimilarity(double[] arr1, double[] arr2) {
        if (arr1.length != arr2.length) {
            return 0;
        }
        
        double sum1 = 0, sum2 = 0, sum12 = 0;
        for (int i = 0; i < arr1.length; i++) {
            sum1 += arr1[i] * arr1[i];
            sum2 += arr2[i] * arr2[i];
            sum12 += arr1[i] * arr2[i];
        }
        
        double denominator = Math.sqrt(sum1 * sum2);
        if (denominator == 0) return 0;
        
        return (float) Math.max(0, sum12 / denominator);
    }
    
    private float calculateFrequencySimilarity(double freq1, double freq2) {
        if (freq1 == 0 || freq2 == 0) return 0;
        
        double ratio = Math.min(freq1, freq2) / Math.max(freq1, freq2);
        return (float) ratio;
    }
    
    private float calculateIntensitySimilarity(double intensity1, double intensity2) {
        if (intensity1 == 0 && intensity2 == 0) return 1;
        if (intensity1 == 0 || intensity2 == 0) return 0;
        
        double ratio = Math.min(intensity1, intensity2) / Math.max(intensity1, intensity2);
        return (float) ratio;
    }
    
    public boolean isProfileInitialized() {
        return encryptedPrefs.getBoolean(KEY_PROFILE_INITIALIZED, false);
    }
    
    public void resetVoiceProfile() {
        encryptedPrefs.edit()
            .remove(KEY_VOICE_PROFILE)
            .remove(KEY_PROFILE_INITIALIZED)
            .apply();
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "Voice profile reset"); }

    }
    
    // Data classes for voice profile management
    private static class VoiceProfile {
        private final List<VoiceFeature> features;
        private final long createdTimestamp;
        
        public VoiceProfile(List<VoiceFeature> features) {
            this.features = features;
            this.createdTimestamp = System.currentTimeMillis();
        }
        
        public List<VoiceFeature> getFeatures() {
            return features;
        }
        
        public long getCreatedTimestamp() {
            return createdTimestamp;
        }
    }
    
    private static class VoiceFeature {
        private final double[] mfccFeatures;
        private final double fundamentalFreq;
        private final double[] spectralFeatures;
        private final double voiceIntensity;
        
        public VoiceFeature(double[] mfccFeatures, double fundamentalFreq, 
                           double[] spectralFeatures, double voiceIntensity) {
            this.mfccFeatures = mfccFeatures;
            this.fundamentalFreq = fundamentalFreq;
            this.spectralFeatures = spectralFeatures;
            this.voiceIntensity = voiceIntensity;
        }
        
        public double[] getMfccFeatures() { return mfccFeatures; }
        public double getFundamentalFreq() { return fundamentalFreq; }
        public double[] getSpectralFeatures() { return spectralFeatures; }
        public double getVoiceIntensity() { return voiceIntensity; }
    }
}
