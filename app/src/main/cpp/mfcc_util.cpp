#include "audio_processing.h"
#include <cmath>
#include <vector>
#include <algorithm>
#include <android/log.h>

#if HAVE_NEON
#include <arm_neon.h>
#endif

#define LOG_TAG "MFCC_Util"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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
    if (n <= 0) {
        LOGE("Invalid input size for power spectrum calculation");
        return;
    }
    
    int i = 0;
    
    #if HAVE_NEON
    // Process 4 elements at a time with NEON
    for (; i <= n - 4; i += 4) {
        float32x4_t real = vld1q_f32(fft_real + i);
        float32x4_t imag = vld1q_f32(fft_imag + i);
        
        // Calculate power: real^2 + imag^2
        float32x4_t power_vec = vaddq_f32(
            vmulq_f32(real, real),
            vmulq_f32(imag, imag)
        );
        
        // Add small epsilon to avoid log(0) in MFCC computation
        float32x4_t epsilon = vdupq_n_f32(1e-10f);
        power_vec = vaddq_f32(power_vec, epsilon);
        
        vst1q_f32(power + i, power_vec);
    }
    #endif
    
    // Process remaining elements or all elements if NEON is not available
    for (; i < n; i++) {
        // Add small epsilon to avoid log(0) in MFCC computation
        power[i] = fft_real[i] * fft_real[i] + fft_imag[i] * fft_imag[i] + 1e-10f;
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
    if (!power_spectrum || !mel_energies || fft_size <= 0 || num_filters <= 0) {
        LOGE("Invalid input parameters for Mel filter bank");
        return;
    }
    
    // Create Mel filter bank
    auto filter_bank = create_mel_filter_bank(num_filters, fft_size, sample_rate);
    if (filter_bank.empty() || filter_bank[0].size() != static_cast<size_t>(fft_size)) {
        LOGE("Failed to create Mel filter bank");
        return;
    }
    
    // Apply each filter to the power spectrum
    for (int i = 0; i < num_filters; i++) {
        float energy = 0.0f;
        
        #if HAVE_NEON
        // Process 4 elements at a time with NEON
        int j = 0;
        float32x4_t sum_vec = vdupq_n_f32(0.0f);
        
        for (; j <= fft_size - 4; j += 4) {
            float32x4_t power_vec = vld1q_f32(power_spectrum + j);
            float32x4_t filter_vec = vld1q_f32(&filter_bank[i][j]);
            
            // Multiply and accumulate
            sum_vec = vmlaq_f32(sum_vec, power_vec, filter_vec);
        }
        
        // Horizontal sum of the vector
        float32x2_t sum_high = vget_high_f32(sum_vec);
        float32x2_t sum_low = vget_low_f32(sum_vec);
        float32x2_t sum_pair = vadd_f32(sum_high, sum_low);
        energy = vget_lane_f32(vpadd_f32(sum_pair, sum_pair), 0);
        
        // Process remaining elements
        for (; j < fft_size; j++) {
            energy += power_spectrum[j] * filter_bank[i][j];
        }
        #else
        // Scalar version with loop unrolling
        const float* power_ptr = power_spectrum;
        const float* filter_ptr = filter_bank[i].data();
        int j = 0;
        
        // Process 4 elements at a time for better performance
        for (; j <= fft_size - 4; j += 4) {
            energy += power_ptr[j] * filter_ptr[j] +
                     power_ptr[j+1] * filter_ptr[j+1] +
                     power_ptr[j+2] * filter_ptr[j+2] +
                     power_ptr[j+3] * filter_ptr[j+3];
        }
        
        // Process remaining elements
        for (; j < fft_size; j++) {
            energy += power_ptr[j] * filter_ptr[j];
        }
        #endif
        
        // Take log of energy (add small value to avoid log(0))
        mel_energies[i] = logf(energy + 1e-10f);
    }
}

// Discrete Cosine Transform (DCT-II)
void dct(const float* input, float* output, int n, int m) {
    if (!input || !output || n <= 0 || m <= 0) {
        LOGE("Invalid input parameters for DCT");
        return;
    }
    
    const float scale = sqrtf(2.0f / m);
    
    for (int k = 0; k < n; k++) {
        float sum = 0.0f;
        const float k_scale = (k == 0) ? 1.0f / sqrtf(2.0f) : 1.0f;
        
        #if HAVE_NEON
        // Process 4 elements at a time with NEON
        float32x4_t sum_vec = vdupq_n_f32(0.0f);
        const float* input_ptr = input;
        int i = 0;
        
        for (; i <= m - 4; i += 4) {
            // Load 4 input elements
            float32x4_t x = vld1q_f32(input_ptr + i);
            
            // Compute cos terms for 4 elements
            float32x4_t cos_terms;
            for (int j = 0; j < 4; j++) {
                float angle = M_PI * k * (2 * (i + j) + 1) / (2.0f * m);
                cos_terms[j] = cosf(angle);
            }
            
            // Multiply and accumulate
            sum_vec = vmlaq_f32(sum_vec, x, cos_terms);
        }
        
        // Horizontal sum of the vector
        float32x2_t sum_high = vget_high_f32(sum_vec);
        float32x2_t sum_low = vget_low_f32(sum_vec);
        float32x2_t sum_pair = vadd_f32(sum_high, sum_low);
        sum = vget_lane_f32(vpadd_f32(sum_pair, sum_pair), 0);
        
        // Process remaining elements
        for (; i < m; i++) {
            float angle = M_PI * k * (2 * i + 1) / (2.0f * m);
            sum += input[i] * cosf(angle);
        }
        #else
        // Scalar version with loop unrolling
        int i = 0;
        for (; i <= m - 4; i += 4) {
            sum += input[i] * cosf(M_PI * k * (2 * i + 1) / (2.0f * m)) +
                   input[i+1] * cosf(M_PI * k * (2 * (i+1) + 1) / (2.0f * m)) +
                   input[i+2] * cosf(M_PI * k * (2 * (i+2) + 1) / (2.0f * m)) +
                   input[i+3] * cosf(M_PI * k * (2 * (i+3) + 1) / (2.0f * m));
        }
        
        // Process remaining elements
        for (; i < m; i++) {
            sum += input[i] * cosf(M_PI * k * (2 * i + 1) / (2.0f * m));
        }
        #endif
        
        output[k] = scale * k_scale * sum;
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
