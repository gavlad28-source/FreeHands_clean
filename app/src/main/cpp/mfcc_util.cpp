#include "audio_processing.h"
#include <cmath>
#include <arm_neon.h>
#include <vector>
#include <algorithm>

// Convert frequency to Mel scale
static float hz_to_mel(float hz) {
    return 2595.0f * log10f(1.0f + hz / 700.0f);
}

// Convert Mel scale to frequency
static float mel_to_hz(float mel) {
    return 700.0f * (powf(10.0f, mel / 2595.0f) - 1.0f);
}

// Apply Hamming window to the input signal
void apply_hamming_window(float* data, int n) {
    const float alpha = 0.54f;
    const float beta = 0.46f;
    const float two_pi = 2.0f * M_PI;
    
    for (int i = 0; i < n; i++) {
        float w = alpha - beta * cosf(two_pi * i / (n - 1));
        data[i] *= w;
    }
}

// Compute power spectrum from FFT result
void power_spectrum(const float* fft_real, const float* fft_imag, float* power, int n) {
    // Process 4 elements at a time using NEON
    int i = 0;
    for (; i <= n - 4; i += 4) {
        float32x4_t real = vld1q_f32(fft_real + i);
        float32x4_t imag = vld1q_f32(fft_imag + i);
        
        // power = real*real + imag*imag
        float32x4_t power_vec = vaddq_f32(
            vmulq_f32(real, real),
            vmulq_f32(imag, imag)
        );
        
        vst1q_f32(power + i, power_vec);
    }
    
    // Process remaining elements
    for (; i < n; i++) {
        power[i] = fft_real[i] * fft_real[i] + fft_imag[i] * fft_imag[i];
    }
}

// Create Mel filter bank
static std::vector<std::vector<float>> create_mel_filter_bank(int num_filters, int fft_size, float sample_rate) {
    std::vector<std::vector<float>> filter_bank(num_filters, std::vector<float>(fft_size / 2 + 1, 0.0f));
    
    float mel_min = hz_to_mel(0);
    float mel_max = hz_to_mel(sample_rate / 2);
    
    // Create Mel-spaced frequencies
    std::vector<float> mel_points(num_filters + 2);
    for (int i = 0; i < num_filters + 2; i++) {
        mel_points[i] = mel_min + i * (mel_max - mel_min) / (num_filters + 1);
    }
    
    // Convert Mel frequencies to Hz
    std::vector<float> hz_points(num_filters + 2);
    for (int i = 0; i < num_filters + 2; i++) {
        hz_points[i] = mel_to_hz(mel_points[i]);
    }
    
    // Convert Hz to FFT bin indices
    std::vector<int> bin(num_filters + 2);
    for (int i = 0; i < num_filters + 2; i++) {
        bin[i] = static_cast<int>((fft_size + 1) * hz_points[i] / sample_rate);
        bin[i] = std::max(1, std::min(fft_size / 2, bin[i]));
    }
    
    // Create triangular filters
    for (int i = 0; i < num_filters; i++) {
        int left = bin[i];
        int center = bin[i + 1];
        int right = bin[i + 2];
        
        // Left side of the triangle
        for (int j = left; j < center; j++) {
            filter_bank[i][j] = static_cast<float>(j - left) / (center - left);
        }
        
        // Right side of the triangle
        for (int j = center; j < right; j++) {
            filter_bank[i][j] = 1.0f - static_cast<float>(j - center) / (right - center);
        }
    }
    
    return filter_bank;
}

// Apply Mel filter bank to power spectrum
void apply_mel_filter_bank(const float* power_spectrum, int fft_size, float sample_rate,
                          float* mel_energies, int num_filters) {
    static auto filter_bank = create_mel_filter_bank(num_filters, fft_size, sample_rate);
    
    for (int i = 0; i < num_filters; i++) {
        float energy = 0.0f;
        const auto& filter = filter_bank[i];
        
        // Process 4 elements at a time using NEON
        int j = 0;
        float32x4_t sum_vec = vdupq_n_f32(0.0f);
        
        for (; j <= fft_size/2 - 4; j += 4) {
            float32x4_t power_vec = vld1q_f32(power_spectrum + j);
            float32x4_t filter_vec = vld1q_f32(&filter[j]);
            sum_vec = vmlaq_f32(sum_vec, power_vec, filter_vec);
        }
        
        // Sum the vector
        float32x2_t sum_high = vget_high_f32(sum_vec);
        float32x2_t sum_low = vget_low_f32(sum_vec);
        float32x2_t sum_pair = vadd_f32(sum_high, sum_low);
        energy = vget_lane_f32(vpadd_f32(sum_pair, sum_pair), 0);
        
        // Process remaining elements
        for (; j <= fft_size/2; j++) {
            energy += power_spectrum[j] * filter[j];
        }
        
        // Avoid log(0)
        mel_energies[i] = logf(fmaxf(1e-10f, energy));
    }
}

// Discrete Cosine Transform (DCT-II)
void dct(const float* input, float* output, int n, int m) {
    float c = sqrtf(2.0f / n);
    
    for (int k = 0; k < m; k++) {
        float sum = 0.0f;
        float w = (k == 0) ? sqrtf(0.5f) : 1.0f;
        
        for (int i = 0; i < n; i++) {
            sum += input[i] * cosf(M_PI * k * (2.0f * i + 1) / (2.0f * n));
        }
        
        output[k] = c * w * sum;
    }
}

// Main MFCC computation function
void compute_mfcc(const float* audio_data, int num_samples, int sample_rate,
                 float* mfcc, int num_mfcc, int num_filters) {
    // Constants
    const int frame_size = 1024;  // Must match WINDOW_SIZE in Kotlin
    const int hop_size = 512;     // Must match HOP_SIZE in Kotlin
    
    // Apply Hamming window
    std::vector<float> windowed(frame_size);
    std::copy(audio_data, audio_data + frame_size, windowed.begin());
    apply_hamming_window(windowed.data(), frame_size);
    
    // Compute FFT
    std::vector<float> fft_real(frame_size);
    std::vector<float> fft_imag(frame_size, 0.0f);
    fft(windowed.data(), fft_real.data(), frame_size);
    
    // Compute power spectrum
    std::vector<float> power(frame_size / 2 + 1);
    power_spectrum(fft_real.data(), fft_imag.data(), power.data(), frame_size / 2 + 1);
    
    // Apply Mel filter bank
    std::vector<float> mel_energies(num_filters);
    apply_mel_filter_bank(power.data(), frame_size, sample_rate, mel_energies.data(), num_filters);
    
    // Apply DCT to get MFCCs
    dct(mel_energies.data(), mfcc, num_filters, num_mfcc);
}
