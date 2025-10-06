#include "audio_processing.h"
#include <cmath>
#include <android/log.h>

#if HAVE_NEON
#include <arm_neon.h>
#endif

#define LOG_TAG "AudioProcessing"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Define NEON vector types for better readability
#if HAVE_NEON
typedef float32x4_t float32x4_t;
typedef float32x4x2_t float32x4x2_t;
#endif

// Define complex number structure for non-NEON implementation
struct Complex {
    float real;
    float imag;
};

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

#if HAVE_NEON
// NEON-optimized complex multiplication
static inline void complex_multiply(float* out, const float* a, const float* b) {
    // Load complex numbers as interleaved real/imaginary pairs
    float32x4x2_t a_vec = vld2q_f32(a);
    float32x4x2_t b_vec = vld2q_f32(b);
    
    // a.real * b.real - a.imag * b.imag
    float32x4_t real = vmulq_f32(a_vec.val[0], b_vec.val[0]);
    real = vmlsq_f32(real, a_vec.val[1], b_vec.val[1]);
    
    // a.real * b.imag + a.imag * b.real
    float32x4_t imag = vmulq_f32(a_vec.val[0], b_vec.val[1]);
    imag = vmlaq_f32(imag, a_vec.val[1], b_vec.val[0]);
    
    // Store results
    float32x4x2_t result = {real, imag};
    vst2q_f32(out, result);
}
#else
// Fallback complex multiplication for non-NEON
static inline void complex_multiply(float* out, const float* a, const float* b) {
    // Process two complex numbers at a time for better performance
    for (int i = 0; i < 2; i++) {
        float ar = a[2*i];
        float ai = a[2*i + 1];
        float br = b[2*i];
        float bi = b[2*i + 1];
        
        out[2*i] = ar * br - ai * bi;     // Real part
        out[2*i + 1] = ar * bi + ai * br; // Imaginary part
    }
}
#endif

// FFT implementation with NEON optimization when available
void fft(float* input, float* output, int n) {
    if (n <= 0 || (n & (n - 1)) != 0) {
        LOGE("FFT size must be a power of 2");
        return;
    }
    
    // Copy input to output and convert to complex format if needed
    for (int i = 0; i < n; i++) {
        output[2*i] = input[2*i];     // Real part
        output[2*i + 1] = 0.0f;       // Imaginary part (zero for real input)
    }

    // Bit-reverse ordering
    bit_reverse(output, n);

    // Iterate through the FFT stages
    for (int len = 2; len <= n; len <<= 1) {
        int half_len = len >> 1;
        float theta = -2.0f * M_PI / len;
        float w_step_real = cosf(theta);
        float w_step_imag = sinf(theta);

        #if HAVE_NEON
        // NEON-optimized version for larger FFTs
        if (len >= 4) {
            for (int i = 0; i < n; i += len) {
                float w_real = 1.0f;
                float w_imag = 0.0f;
                
                for (int j = 0; j < half_len; j += 4) {
                    // Process 4 complex numbers at a time with NEON
                    int idx = 2 * (i + j);
                    float32x4x2_t a = vld2q_f32(output + idx);
                    float32x4x2_t b = vld2q_f32(output + idx + 2 * half_len);
                    
                    // Compute twiddle factors for 4 points
                    float w[8];
                    for (int k = 0; k < 4; k++) {
                        w[2*k] = w_real;
                        w[2*k+1] = w_imag;
                        float new_real = w_real * w_step_real - w_imag * w_step_imag;
                        float new_imag = w_real * w_step_imag + w_imag * w_step_real;
                        w_real = new_real;
                        w_imag = new_imag;
                    }
                    
                    float32x4x2_t w_vec = vld2q_f32(w);
                    
                    // Complex multiplication: t = b * w
                    float32x4_t t_real = vsubq_f32(
                        vmulq_f32(b.val[0], w_vec.val[0]),
                        vmulq_f32(b.val[1], w_vec.val[1])
                    );
                    float32x4_t t_imag = vaddq_f32(
                        vmulq_f32(b.val[0], w_vec.val[1]),
                        vmulq_f32(b.val[1], w_vec.val[0])
                    );
                    
                    // Butterfly operation
                    float32x4_t out1_real = vaddq_f32(a.val[0], t_real);
                    float32x4_t out1_imag = vaddq_f32(a.val[1], t_imag);
                    float32x4_t out2_real = vsubq_f32(a.val[0], t_real);
                    float32x4_t out2_imag = vsubq_f32(a.val[1], t_imag);
                    
                    // Store results
                    float32x4x2_t out1 = {out1_real, out1_imag};
                    float32x4x2_t out2 = {out2_real, out2_imag};
                    vst2q_f32(output + 2 * (i + j), out1);
                    vst2q_f32(output + 2 * (i + j + half_len), out2);
                }
            }
            continue;  // Skip the scalar version for this iteration
        }
        #endif

        // Scalar implementation for non-NEON devices
        for (int i = 0; i < n; i += len) {
            for (int j = 0; j < half_len; j++) {
                float w_real = cosf(j * theta);
                float w_imag = sinf(j * theta);
                
                int idx1 = 2 * (i + j);
                int idx2 = 2 * (i + j + half_len);
                
                float t_real = w_real * output[idx2] - w_imag * output[idx2 + 1];
                float t_imag = w_real * output[idx2 + 1] + w_imag * output[idx2];
                
                float u_real = output[idx1];
                float u_imag = output[idx1 + 1];
                
                output[idx1] = u_real + t_real;
                output[idx1 + 1] = u_imag + t_imag;
                output[idx2] = u_real - t_real;
                output[idx2 + 1] = u_imag - t_imag;
            }
        }
    }
}

// Precompute twiddle factors for NEON optimization
static void precompute_twiddle_factors(int n, float** w_real, float** w_imag) {
    *w_real = new float[n/2];
    *w_imag = new float[n/2];
    
    for (int i = 0; i < n/2; i++) {
        float theta = -2.0f * M_PI * i / n;
        (*w_real)[i] = cosf(theta);
        (*w_imag)[i] = sinf(theta);
    }
}

// Free twiddle factors
static void free_twiddle_factors(float* w_real, float* w_imag) {
    delete[] w_real;
    delete[] w_imag;
}

// Inverse FFT (using forward FFT with sign change and scaling)
void ifft(float* input, float* output, int n) {
    if (n <= 0 || (n & (n - 1)) != 0) {
        LOGE("IFFT size must be a power of 2");
        return;
    }
    
    // Precompute twiddle factors for NEON optimization
    float* w_real = nullptr;
    float* w_imag = nullptr;
    #if HAVE_NEON
    precompute_twiddle_factors(n, &w_real, &w_imag);
    #endif
    
    // Copy input to output with sign change for imaginary parts
    #if HAVE_NEON
    for (int i = 0; i < 2 * n; i += 8) {
        float32x4x2_t data = vld2q_f32(&input[i]);
        data.val[1] = vnegq_f32(data.val[1]);  // Negate imaginary parts
        vst2q_f32(&output[i], data);
    }
    #else
    for (int i = 0; i < 2 * n; i += 2) {
        output[i] = input[i];
        output[i + 1] = -input[i + 1];
    }
    #endif
    
    // Compute forward FFT
    fft(output, output, n);
    
    // Scale the result and conjugate
    float scale = 1.0f / n;
    #if HAVE_NEON
    float32x4_t scale_vec = vdupq_n_f32(scale);
    for (int i = 0; i < 2 * n; i += 8) {
        float32x4x2_t data = vld2q_f32(&output[i]);
        data.val[0] = vmulq_f32(data.val[0], scale_vec);
        data.val[1] = vnegq_f32(vmulq_f32(data.val[1], scale_vec));
        vst2q_f32(&output[i], data);
    }
    #else
    for (int i = 0; i < 2 * n; i += 2) {
        output[i] *= scale;
        output[i + 1] = -output[i + 1] * scale;
    }
    #endif
}
