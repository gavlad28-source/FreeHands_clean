package com.freehands.assistant;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages voice profile data structures and operations.
 * This class is used by VoiceBiometricAuthenticator to handle voice profile management.
 */
public class VoiceProfileManager {
    private static final String TAG = "VoiceProfileManager";
    
    // Voice profile quality thresholds
    private static final double MIN_PROFILE_QUALITY = 0.6;
    private static final double MIN_SAMPLE_DURATION = 1.0; // seconds
    private static final int MIN_SAMPLES_FOR_PROFILE = 2;
    private static final int MAX_SAMPLES_PER_PROFILE = 5;
    
    public VoiceProfileManager() {
        if (BuildConfig.DEBUG) { Log.d(TAG, "VoiceProfileManager initialized"); }

    }
    
    /**
     * Create a voice profile from multiple voice samples
     */
    public VoiceProfile createProfile(List<VoiceSample> samples) {
        if (samples == null || samples.size() < MIN_SAMPLES_FOR_PROFILE) {
            Log.w(TAG, "Insufficient samples for profile creation");
            return null;
        }
        
        // Filter high-quality samples
        List<VoiceSample> qualitySamples = filterQualitySamples(samples);
        
        if (qualitySamples.size() < MIN_SAMPLES_FOR_PROFILE) {
            Log.w(TAG, "Insufficient quality samples for profile creation");
            return null;
        }
        
        // Limit to max samples to prevent overfitting
        if (qualitySamples.size() > MAX_SAMPLES_PER_PROFILE) {
            qualitySamples = qualitySamples.subList(0, MAX_SAMPLES_PER_PROFILE);
        }
        
        // Create averaged profile from samples
        VoiceProfile profile = generateAveragedProfile(qualitySamples);
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "Voice profile created with " + qualitySamples.size() + " samples"); }

        return profile;
    }
    
    /**
     * Update an existing profile with new samples
     */
    public VoiceProfile updateProfile(VoiceProfile existingProfile, List<VoiceSample> newSamples) {
        if (existingProfile == null) {
            return createProfile(newSamples);
        }
        
        List<VoiceSample> allSamples = new ArrayList<>(existingProfile.getSourceSamples());
        allSamples.addAll(filterQualitySamples(newSamples));
        
        // Keep only the most recent high-quality samples
        if (allSamples.size() > MAX_SAMPLES_PER_PROFILE) {
            // Sort by quality and timestamp, keep best ones
            allSamples.sort((a, b) -> {
                int qualityCompare = Double.compare(b.getQualityScore(), a.getQualityScore());
                if (qualityCompare != 0) return qualityCompare;
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            });
            
            allSamples = allSamples.subList(0, MAX_SAMPLES_PER_PROFILE);
        }
        
        return generateAveragedProfile(allSamples);
    }
    
    /**
     * Validate profile quality
     */
    public boolean validateProfile(VoiceProfile profile) {
        if (profile == null) {
            return false;
        }
        
        // Check profile completeness
        if (profile.getMfccTemplate() == null || profile.getMfccTemplate().length == 0) {
            Log.w(TAG, "Profile missing MFCC data");
            return false;
        }
        
        if (profile.getFundamentalFreqRange() == null || profile.getFundamentalFreqRange().length != 2) {
            Log.w(TAG, "Profile missing frequency range data");
            return false;
        }
        
        // Check quality score
        if (profile.getQualityScore() < MIN_PROFILE_QUALITY) {
            Log.w(TAG, "Profile quality too low: " + profile.getQualityScore());
            return false;
        }
        
        // Check sample count
        if (profile.getSourceSamples().size() < MIN_SAMPLES_FOR_PROFILE) {
            Log.w(TAG, "Profile has insufficient samples");
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate similarity between two voice profiles
     */
    public double calculateProfileSimilarity(VoiceProfile profile1, VoiceProfile profile2) {
        if (profile1 == null || profile2 == null) {
            return 0.0;
        }
        
        // MFCC similarity
        double mfccSimilarity = calculateArraySimilarity(
            profile1.getMfccTemplate(), 
            profile2.getMfccTemplate()
        );
        
        // Frequency range similarity
        double freqSimilarity = calculateFrequencyRangeSimilarity(
            profile1.getFundamentalFreqRange(),
            profile2.getFundamentalFreqRange()
        );
        
        // Spectral features similarity
        double spectralSimilarity = calculateArraySimilarity(
            profile1.getSpectralTemplate(),
            profile2.getSpectralTemplate()
        );
        
        // Weighted combination
        double totalSimilarity = (mfccSimilarity * 0.5) + 
                               (freqSimilarity * 0.3) + 
                               (spectralSimilarity * 0.2);
        
        return Math.max(0.0, Math.min(1.0, totalSimilarity));
    }
    
    /**
     * Filter samples by quality metrics
     */
    private List<VoiceSample> filterQualitySamples(List<VoiceSample> samples) {
        List<VoiceSample> qualitySamples = new ArrayList<>();
        
        for (VoiceSample sample : samples) {
            if (isQualitySample(sample)) {
                qualitySamples.add(sample);
            }
        }
        
        return qualitySamples;
    }
    
    /**
     * Check if a sample meets quality requirements
     */
    private boolean isQualitySample(VoiceSample sample) {
        // Check duration
        if (sample.getDuration() < MIN_SAMPLE_DURATION) {
            return false;
        }
        
        // Check signal quality
        if (sample.getSignalToNoiseRatio() < 10.0) { // 10 dB minimum
            return false;
        }
        
        // Check for audio clipping
        if (sample.isClipped()) {
            return false;
        }
        
        // Check energy level
        if (sample.getAverageEnergy() < 0.01) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Generate an averaged profile from multiple samples
     */
    private VoiceProfile generateAveragedProfile(List<VoiceSample> samples) {
        // Calculate averaged features
        double[] avgMfcc = calculateAverageFeatures(samples, VoiceSample::getMfccFeatures);
        double[] avgSpectral = calculateAverageFeatures(samples, VoiceSample::getSpectralFeatures);
        
        // Calculate frequency range
        double[] freqRange = calculateFrequencyRange(samples);
        
        // Calculate overall quality score
        double qualityScore = calculateOverallQuality(samples);
        
        // Calculate voice intensity statistics
        VoiceIntensityStats intensityStats = calculateIntensityStats(samples);
        
        return new VoiceProfile(
            avgMfcc,
            freqRange,
            avgSpectral,
            intensityStats,
            qualityScore,
            new ArrayList<>(samples),
            System.currentTimeMillis()
        );
    }
    
    /**
     * Calculate average features across samples
     */
    private double[] calculateAverageFeatures(List<VoiceSample> samples, FeatureExtractor extractor) {
        if (samples.isEmpty()) {
            return new double[0];
        }
        
        double[] firstFeatures = extractor.extract(samples.get(0));
        if (firstFeatures == null) {
            return new double[0];
        }
        
        double[] avgFeatures = new double[firstFeatures.length];
        
        for (VoiceSample sample : samples) {
            double[] features = extractor.extract(sample);
            if (features != null && features.length == avgFeatures.length) {
                for (int i = 0; i < features.length; i++) {
                    avgFeatures[i] += features[i];
                }
            }
        }
        
        // Average the features
        for (int i = 0; i < avgFeatures.length; i++) {
            avgFeatures[i] /= samples.size();
        }
        
        return avgFeatures;
    }
    
    /**
     * Calculate fundamental frequency range
     */
    private double[] calculateFrequencyRange(List<VoiceSample> samples) {
        double minFreq = Double.MAX_VALUE;
        double maxFreq = Double.MIN_VALUE;
        
        for (VoiceSample sample : samples) {
            double freq = sample.getFundamentalFrequency();
            if (freq > 0) { // Valid frequency
                minFreq = Math.min(minFreq, freq);
                maxFreq = Math.max(maxFreq, freq);
            }
        }
        
        if (minFreq == Double.MAX_VALUE) {
            return new double[]{100.0, 300.0}; // Default range
        }
        
        // Add some tolerance
        double tolerance = (maxFreq - minFreq) * 0.1;
        return new double[]{minFreq - tolerance, maxFreq + tolerance};
    }
    
    /**
     * Calculate overall profile quality
     */
    private double calculateOverallQuality(List<VoiceSample> samples) {
        double totalQuality = 0;
        
        for (VoiceSample sample : samples) {
            totalQuality += sample.getQualityScore();
        }
        
        double avgQuality = totalQuality / samples.size();
        
        // Bonus for multiple samples
        double sampleBonus = Math.min(0.1, samples.size() * 0.02);
        
        return Math.min(1.0, avgQuality + sampleBonus);
    }
    
    /**
     * Calculate voice intensity statistics
     */
    private VoiceIntensityStats calculateIntensityStats(List<VoiceSample> samples) {
        double totalIntensity = 0;
        double minIntensity = Double.MAX_VALUE;
        double maxIntensity = Double.MIN_VALUE;
        
        for (VoiceSample sample : samples) {
            double intensity = sample.getAverageEnergy();
            totalIntensity += intensity;
            minIntensity = Math.min(minIntensity, intensity);
            maxIntensity = Math.max(maxIntensity, intensity);
        }
        
        double avgIntensity = totalIntensity / samples.size();
        
        return new VoiceIntensityStats(avgIntensity, minIntensity, maxIntensity);
    }
    
    /**
     * Calculate similarity between two feature arrays
     */
    private double calculateArraySimilarity(double[] arr1, double[] arr2) {
        if (arr1 == null || arr2 == null || arr1.length != arr2.length) {
            return 0.0;
        }
        
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        for (int i = 0; i < arr1.length; i++) {
            dotProduct += arr1[i] * arr2[i];
            norm1 += arr1[i] * arr1[i];
            norm2 += arr2[i] * arr2[i];
        }
        
        double denominator = Math.sqrt(norm1 * norm2);
        if (denominator == 0) {
            return 0.0;
        }
        
        return dotProduct / denominator;
    }
    
    /**
     * Calculate similarity between frequency ranges
     */
    private double calculateFrequencyRangeSimilarity(double[] range1, double[] range2) {
        if (range1 == null || range2 == null || range1.length != 2 || range2.length != 2) {
            return 0.0;
        }
        
        // Calculate overlap
        double overlapStart = Math.max(range1[0], range2[0]);
        double overlapEnd = Math.min(range1[1], range2[1]);
        
        if (overlapStart >= overlapEnd) {
            return 0.0; // No overlap
        }
        
        double overlap = overlapEnd - overlapStart;
        double range1Size = range1[1] - range1[0];
        double range2Size = range2[1] - range2[0];
        
        double union = range1Size + range2Size - overlap;
        
        return overlap / union; // Jaccard similarity
    }
    
    // Functional interface for feature extraction
    private interface FeatureExtractor {
        double[] extract(VoiceSample sample);
    }
    
    // Data classes
    public static class VoiceProfile {
        private final double[] mfccTemplate;
        private final double[] fundamentalFreqRange;
        private final double[] spectralTemplate;
        private final VoiceIntensityStats intensityStats;
        private final double qualityScore;
        private final List<VoiceSample> sourceSamples;
        private final long createdTimestamp;
        
        public VoiceProfile(double[] mfccTemplate, double[] fundamentalFreqRange,
                           double[] spectralTemplate, VoiceIntensityStats intensityStats,
                           double qualityScore, List<VoiceSample> sourceSamples, 
                           long createdTimestamp) {
            this.mfccTemplate = mfccTemplate;
            this.fundamentalFreqRange = fundamentalFreqRange;
            this.spectralTemplate = spectralTemplate;
            this.intensityStats = intensityStats;
            this.qualityScore = qualityScore;
            this.sourceSamples = sourceSamples;
            this.createdTimestamp = createdTimestamp;
        }
        
        // Getters
        public double[] getMfccTemplate() { return mfccTemplate; }
        public double[] getFundamentalFreqRange() { return fundamentalFreqRange; }
        public double[] getSpectralTemplate() { return spectralTemplate; }
        public VoiceIntensityStats getIntensityStats() { return intensityStats; }
        public double getQualityScore() { return qualityScore; }
        public List<VoiceSample> getSourceSamples() { return sourceSamples; }
        public long getCreatedTimestamp() { return createdTimestamp; }
    }
    
    public static class VoiceSample {
        private final double[] mfccFeatures;
        private final double fundamentalFrequency;
        private final double[] spectralFeatures;
        private final double averageEnergy;
        private final double duration;
        private final double signalToNoiseRatio;
        private final boolean isClipped;
        private final double qualityScore;
        private final long timestamp;
        
        public VoiceSample(double[] mfccFeatures, double fundamentalFrequency,
                          double[] spectralFeatures, double averageEnergy, 
                          double duration, double signalToNoiseRatio,
                          boolean isClipped, double qualityScore) {
            this.mfccFeatures = mfccFeatures;
            this.fundamentalFrequency = fundamentalFrequency;
            this.spectralFeatures = spectralFeatures;
            this.averageEnergy = averageEnergy;
            this.duration = duration;
            this.signalToNoiseRatio = signalToNoiseRatio;
            this.isClipped = isClipped;
            this.qualityScore = qualityScore;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public double[] getMfccFeatures() { return mfccFeatures; }
        public double getFundamentalFrequency() { return fundamentalFrequency; }
        public double[] getSpectralFeatures() { return spectralFeatures; }
        public double getAverageEnergy() { return averageEnergy; }
        public double getDuration() { return duration; }
        public double getSignalToNoiseRatio() { return signalToNoiseRatio; }
        public boolean isClipped() { return isClipped; }
        public double getQualityScore() { return qualityScore; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class VoiceIntensityStats {
        private final double averageIntensity;
        private final double minIntensity;
        private final double maxIntensity;
        
        public VoiceIntensityStats(double averageIntensity, double minIntensity, double maxIntensity) {
            this.averageIntensity = averageIntensity;
            this.minIntensity = minIntensity;
            this.maxIntensity = maxIntensity;
        }
        
        public double getAverageIntensity() { return averageIntensity; }
        public double getMinIntensity() { return minIntensity; }
        public double getMaxIntensity() { return maxIntensity; }
    }
}
