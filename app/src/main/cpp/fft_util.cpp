#include "audio_processing.h"
#include <cmath>
#include <arm_neon.h>
#include <android/log.h>

#define LOG_TAG "AudioProcessing"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Bit-reverse function for FFT
static void bit_reverse(float* x, int n) {
    for (int i = 1, j = 0; i < n; i++) {
        int bit = n >> 1;
        for (; j >= bit; bit >>= 1) {
            j -= bit;
        }
        j += bit;
        if (i < j) {
            float tmp = x[i];
            x[i] = x[j];
            x[j] = tmp;
            
            tmp = x[i + 1];
            x[i + 1] = x[j + 1];
            x[j + 1] = tmp;
        }
    }
}

// NEON-optimized complex multiplication
static inline void complex_multiply(float* out, float* a, float* b) {
    float32x4_t a_vec = vld1q_f32(a);
    float32x4_t b_vec = vld1q_f32(b);
    
    // Real part: a.real * b.real - a.imag * b.imag
    // Imag part: a.real * b.imag + a.imag * b.real
    float32x4_t real = vmulq_f32(a_vec, vcombine_f32(vget_low_f32(b_vec), vget_low_f32(b_vec)));
    real = vmlsq_f32(real, vrev64q_f32(a_vec), vcombine_f32(vget_high_f32(b_vec), vget_high_f32(b_vec)));
    
    vst1q_f32(out, real);
}

// NEON-optimized FFT implementation
void fft(float* input, float* output, int n) {
    // Copy input to output (interleaved real/imaginary)
    for (int i = 0; i < n; i++) {
        output[2 * i] = input[i];
        output[2 * i + 1] = 0.0f;
    }
    
    // Bit-reverse the input
    bit_reverse(output, n);
    
    // Compute FFT
    for (int len = 2; len <= n; len <<= 1) {
        float half_len = len / 2;
        float theta = -M_PI / half_len;
        
        // Process in blocks of 4 for NEON optimization
        for (int i = 0; i < n; i += len) {
            // Twiddle factors
            float w_real = 1.0f;
            float w_imag = 0.0f;
            float w_step_real = cosf(theta);
            float w_step_imag = sinf(theta);
            
            for (int j = 0; j < half_len; j += 4) {
                // Load 4 complex numbers using NEON
                float32x4x2_t a = vld2q_f32(output + 2 * (i + j));
                float32x4x2_t b = vld2q_f32(output + 2 * (i + j + half_len));
                
                // Compute twiddle factors for 4 points
                float w[8] = {
                    w_real, w_imag,
                    (w_real * w_step_real - w_imag * w_step_imag),
                    (w_real * w_step_imag + w_imag * w_step_real),
                    0, 0, 0, 0
                };
                
                // Update w for next iteration
                float tmp_real = w[2];
                float tmp_imag = w[3];
                w[4] = tmp_real * w_step_real - tmp_imag * w_step_imag;
                w[5] = tmp_real * w_step_imag + tmp_imag * w_step_real;
                
                // Load twiddle factors
                float32x4x2_t w_vec = vld2q_f32(w);
                
                // Multiply b by twiddle factors
                float32x4_t t_real = vsubq_f32(
                    vmulq_f32(b.values[0], w_vec.val[0]),
                    vmulq_f32(b.values[1], w_vec.val[1])
                );
                float32x4_t t_imag = vaddq_f32(
                    vmulq_f32(b.values[0], w_vec.val[1]),
                    vmulq_f32(b.values[1], w_vec.val[0])
                );
                
                // Butterfly operation
                float32x4_t out1_real = vaddq_f32(a.val[0], t_real);
                float32x4_t out1_imag = vaddq_f32(a.val[1], t_imag);
                float32x4_t out2_real = vsubq_f32(a.val[0], t_real);
                float32x4_t out2_imag = vsubq_f32(a.val[1], t_imag);
                
                // Store results
                vst1q_f32(output + 2 * (i + j), out1_real);
                vst1q_f32(output + 2 * (i + j) + 4, out1_imag);
                vst1q_f32(output + 2 * (i + j + half_len), out2_real);
                vst1q_f32(output + 2 * (i + j + half_len) + 4, out2_imag);
                
                // Update twiddle factors for next iteration
                float new_real = w_real * w_step_real - w_imag * w_step_imag;
                float new_imag = w_real * w_step_imag + w_imag * w_step_real;
                w_real = new_real;
                w_imag = new_imag;
            }
        }
    }
}

// Inverse FFT (using forward FFT with sign change and scaling)
void ifft(float* input, float* output, int n) {
    // Take complex conjugate of input
    for (int i = 0; i < 2 * n; i += 2) {
        input[i + 1] = -input[i + 1];
    }
    
    // Compute forward FFT
    fft(input, output, n);
    
    // Take complex conjugate and scale
    float scale = 1.0f / n;
    for (int i = 0; i < 2 * n; i++) {
        output[i] *= scale;
    }
}
