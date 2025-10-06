#include "audio_processing.h"
#include <cmath>
#include <vector>
#include <algorithm>

#if HAVE_NEON
#include <arm_neon.h>
#endif

// Constants for audio processing
constexpr float PI = 3.14159265358979323846f;
constexpr float TWO_PI = 2.0f * PI;
constexpr float LOG_2 = 0.6931471805599453f;
constexpr float LOG_10 = 2.302585092994046f;

// NEON-optimized logarithm functions
static inline float fast_log2(float x) {
    // Fast log2 approximation using bit manipulation
    union { float f; uint32_t i; } vx = { x };
    union { uint32_t i; float f; } mx = { (vx.i & 0x007FFFFF) | 0x3f000000 };
    float y = vx.i * 1.1920928955078125e-7f; // 1/2^23
    return y - 124.22551499f - 1.498030302f * mx.f - 1.72587999f / (0.3520887068f + mx.f);
}

static inline float fast_log10(float x) {
    return fast_log2(x) * 0.3010299956639812f; // log10(2)
}

#if HAVE_NEON
// NEON-optimized exponential function
static inline float32x4_t exp_ps(float32x4_t x) {
    // Implementation of exp using Taylor series approximation
    const float32x4_t a0 = vdupq_n_f32(1.0f);
    const float32x4_t a1 = vdupq_n_f32(1.0f);
    const float32x4_t a2 = vdupq_n_f32(0.5f);
    const float32x4_t a3 = vdupq_n_f32(0.1666666666666667f);
    const float32x4_t a4 = vdupq_n_f32(0.0416666666666667f);
    
    // Clamp x to avoid overflow
    x = vminq_f32(x, vdupq_n_f32(88.0f));
    
    // Taylor series approximation
    float32x4_t result = a0;
    result = vmlaq_f32(result, x, a1);
    result = vmlaq_f32(result, vmulq_f32(x, x), a2);
    result = vmlaq_f32(result, vmulq_f32(x, vmulq_f32(x, x)), a3);
    result = vmlaq_f32(result, vmulq_f32(x, vmulq_f32(x, vmulq_f32(x, x))), a4);
    
    return result;
}
#else
// Fallback implementation for non-NEON
static inline float exp_ps(float x) {
    return expf(x);
}
#endif

#if HAVE_NEON
// NEON-optimized sine function
static inline float32x4_t sin_ps(float32x4_t x) {
    // Normalize x to [-π, π]
    const float32x4_t pi = vdupq_n_f32(PI);
    const float32x4_t two_pi = vdupq_n_f32(TWO_PI);
    const float32x4_t inv_two_pi = vdupq_n_f32(1.0f / TWO_PI);
    
    // x = x - floor(x / (2π)) * 2π
    float32x4_t x_scaled = vmulq_f32(x, inv_two_pi);
    x_scaled = vsubq_f32(x_scaled, vcvtq_f32_s32(vcvtq_s32_f32(x_scaled)));
    x = vmulq_f32(x_scaled, two_pi);
    
    // Clamp to [-π, π]
    x = vminq_f32(vmaxq_f32(x, vnegq_f32(pi)), pi);
    
    // Taylor series approximation for sine
    const float32x4_t c1 = vdupq_n_f32(-1.0f / 6.0f);
    const float32x4_t c2 = vdupq_n_f32(1.0f / 120.0f);
    const float32x4_t c3 = vdupq_n_f32(-1.0f / 5040.0f);
    
    float32x4_t x2 = vmulq_f32(x, x);
    float32x4_t x3 = vmulq_f32(x2, x);
    float32x4_t x5 = vmulq_f32(x3, x2);
    float32x4_t x7 = vmulq_f32(x5, x2);
    
    // sin(x) ≈ x + c1*x³ + c2*x⁵ + c3*x⁷
    float32x4_t result = x;
    result = vmlaq_f32(result, x3, c1);
    result = vmlaq_f32(result, x5, c2);
    result = vmlaq_f32(result, x7, c3);
    
    return result;
}
#else
// Fallback implementation for non-NEON
static inline float sin_ps(float x) {
    // Wrap x to [-π, π]
    x = fmodf(x + M_PI, 2.0f * M_PI);
    if (x < 0) x += 2.0f * M_PI;
    x -= M_PI;
    
    // 7th order Taylor series approximation
    float x2 = x * x;
    float x3 = x * x2;
    float x5 = x3 * x2;
    float x7 = x5 * x2;
    return x * (1.0f - x2/6.0f * (1.0f - x2/20.0f * (1.0f - x2/42.0f)));
}
#endif

#if HAVE_NEON
// NEON-optimized cosine function
static inline float32x4_t cos_ps(float32x4_t x) {
    // cos(x) = sin(x + π/2)
    const float32x4_t half_pi = vdupq_n_f32(PI / 2.0f);
    return sin_ps(vaddq_f32(x, half_pi));
}
#else
// Fallback implementation for non-NEON
static inline float cos_ps(float x) {
    // Wrap x to [-π, π]
    x = fmodf(fabsf(x), 2.0f * M_PI);
    if (x > M_PI) x = 2.0f * M_PI - x;
    
    // 6th order Taylor series approximation
    float x2 = x * x;
    float x4 = x2 * x2;
    float x6 = x4 * x2;
    return 1.0f - x2/2.0f * (1.0f - x2/12.0f * (1.0f - x2/30.0f));
}
#endif

#if HAVE_NEON
// NEON-optimized complex multiplication
static inline void complex_multiply_ps(
    float32x4_t a_real, float32x4_t a_imag,
    float32x4_t b_real, float32x4_t b_imag,
    float32x4_t* out_real, float32x4_t* out_imag
) {
    *out_real = vsubq_f32(
        vmulq_f32(a_real, b_real),
        vmulq_f32(a_imag, b_imag)
    );
    
    *out_imag = vaddq_f32(
        vmulq_f32(a_real, b_imag),
        vmulq_f32(a_imag, b_real)
    );
}
#else
// Fallback implementation for non-NEON
static inline void complex_multiply_ps(
    float a_real, float a_imag,
    float b_real, float b_imag,
    float* out_real, float* out_imag
) {
    *out_real = a_real * b_real - a_imag * b_imag;
    *out_imag = a_real * b_imag + a_imag * b_real;
}
#endif

// Vectorized sum with NEON optimization when available
static inline float vector_sum(float* data, int n) {
    float sum = 0.0f;
    
    #if HAVE_NEON
    if (n >= 4) {
        float32x4_t sum_vec = vdupq_n_f32(0.0f);
        int i = 0;
        
        // Process 4 elements at a time with NEON
        for (; i <= n - 4; i += 4) {
            float32x4_t data_vec = vld1q_f32(data + i);
            sum_vec = vaddq_f32(sum_vec, data_vec);
        }
        
        // Horizontal add
        float32x2_t sum_high = vget_high_f32(sum_vec);
        float32x2_t sum_low = vget_low_f32(sum_vec);
        sum_high = vadd_f32(sum_high, sum_low);
        sum = vget_lane_f32(vpadd_f32(sum_high, sum_high), 0);
        
        // Process remaining elements
        for (; i < n; i++) {
            sum += data[i];
        }
        
        return sum;
    }
    #endif
    
    // Fallback to scalar implementation
    for (int i = 0; i < n; i++) {
        sum += data[i];
    }
    
    return sum;
}

#if HAVE_NEON
// NEON-optimized vector multiplication and accumulate
static inline void vector_multiply_accumulate(
    const float* a, const float* b, float* out, int n
) {
    int i = 0;
    
    // Process 4 elements at a time with NEON
    for (; i <= n - 4; i += 4) {
        float32x4_t a_vec = vld1q_f32(a + i);
        float32x4_t b_vec = vld1q_f32(b + i);
        float32x4_t out_vec = vld1q_f32(out + i);
        
        out_vec = vmlaq_f32(out_vec, a_vec, b_vec);
        vst1q_f32(out + i, out_vec);
    }
    
    // Process remaining elements
    for (; i < n; i++) {
        out[i] += a[i] * b[i];
    }
}
#else
// Fallback implementation for non-NEON
static inline void vector_multiply_accumulate(
    const float* a, const float* b, float* out, int n
) {
    // Process 4 elements at a time for better performance
    int i = 0;
    for (; i <= n - 4; i += 4) {
        out[i]   += a[i]   * b[i];
        out[i+1] += a[i+1] * b[i+1];
        out[i+2] += a[i+2] * b[i+2];
        out[i+3] += a[i+3] * b[i+3];
    }
    // Process remaining elements
    for (; i < n; i++) {
        out[i] += a[i] * b[i];
    }
}
#endif

#if HAVE_NEON
// NEON-optimized matrix-vector multiplication
static void matrix_vector_multiply(
    const float* matrix, const float* vector,
    float* result, int rows, int cols
) {
    for (int i = 0; i < rows; i++) {
        const float* row = matrix + i * cols;
        float sum = 0.0f;
        
        int j = 0;
        float32x4_t sum_vec = vdupq_n_f32(0.0f);
        
        // Process 4 elements at a time with NEON
        for (; j <= cols - 4; j += 4) {
            float32x4_t row_vec = vld1q_f32(row + j);
            float32x4_t vec = vld1q_f32(vector + j);
            sum_vec = vmlaq_f32(sum_vec, row_vec, vec);
        }
        
        // Horizontal add
        float32x2_t sum_high = vget_high_f32(sum_vec);
        float32x2_t sum_low = vget_low_f32(sum_vec);
        sum_high = vadd_f32(sum_high, sum_low);
        sum = vget_lane_f32(vpadd_f32(sum_high, sum_high), 0);
        
        // Process remaining elements
        for (; j < cols; j++) {
            sum += row[j] * vector[j];
        }
        
        result[i] = sum;
    }
}
#else
// Fallback implementation for non-NEON
static void matrix_vector_multiply(
    const float* matrix, const float* vector,
    float* result, int rows, int cols
) {
    for (int i = 0; i < rows; i++) {
        const float* row = matrix + i * cols;
        float sum = 0.0f;
        
        for (int j = 0; j < cols; j++) {
            sum += row[j] * vector[j];
        }
        
        result[i] = sum;
    }
}
#endif

#if HAVE_NEON
// NEON-optimized element-wise multiplication
static void vector_multiply(
    const float* a, const float* b, float* out, int n
) {
    int i = 0;
    
    // Process 4 elements at a time with NEON
    for (; i <= n - 4; i += 4) {
        float32x4_t a_vec = vld1q_f32(a + i);
        float32x4_t b_vec = vld1q_f32(b + i);
        float32x4_t out_vec = vmulq_f32(a_vec, b_vec);
        vst1q_f32(out + i, out_vec);
    }
    
    // Process remaining elements
    for (; i < n; i++) {
        out[i] = a[i] * b[i];
    }
}
#else
// Fallback implementation for non-NEON
static void vector_multiply(
    const float* a, const float* b, float* out, int n
) {
    // Process 4 elements at a time for better performance
    int i = 0;
    for (; i <= n - 4; i += 4) {
        out[i]   = a[i]   * b[i];
        out[i+1] = a[i+1] * b[i+1];
        out[i+2] = a[i+2] * b[i+2];
        out[i+3] = a[i+3] * b[i+3];
    }
    // Process remaining elements
    for (; i < n; i++) {
        out[i] = a[i] * b[i];
    }
}
#endif

#if HAVE_NEON
// NEON-optimized element-wise addition
static void vector_add(
    const float* a, const float* b, float* out, int n
) {
    int i = 0;
    
    // Process 4 elements at a time with NEON
    for (; i <= n - 4; i += 4) {
        float32x4_t a_vec = vld1q_f32(a + i);
        float32x4_t b_vec = vld1q_f32(b + i);
        float32x4_t out_vec = vaddq_f32(a_vec, b_vec);
        vst1q_f32(out + i, out_vec);
    }
    
    // Process remaining elements
    for (; i < n; i++) {
        out[i] = a[i] + b[i];
    }
}
#else
// Fallback implementation for non-NEON
static void vector_add(
    const float* a, const float* b, float* out, int n
) {
    // Process 4 elements at a time for better performance
    int i = 0;
    for (; i <= n - 4; i += 4) {
        out[i]   = a[i]   + b[i];
        out[i+1] = a[i+1] + b[i+1];
        out[i+2] = a[i+2] + b[i+2];
        out[i+3] = a[i+3] + b[i+3];
    }
    // Process remaining elements
    for (; i < n; i++) {
        out[i] = a[i] + b[i];
    }
}
#endif

#if HAVE_NEON
// NEON-optimized element-wise subtraction
static void vector_subtract(
    const float* a, const float* b, float* out, int n
) {
    int i = 0;
    
    // Process 4 elements at a time with NEON
    for (; i <= n - 4; i += 4) {
        float32x4_t a_vec = vld1q_f32(a + i);
        float32x4_t b_vec = vld1q_f32(b + i);
        float32x4_t out_vec = vsubq_f32(a_vec, b_vec);
        vst1q_f32(out + i, out_vec);
    }
    
    // Process remaining elements
    for (; i < n; i++) {
        out[i] = a[i] - b[i];
    }
}
#else
// Fallback implementation for non-NEON
static void vector_subtract(
    const float* a, const float* b, float* out, int n
) {
    // Process 4 elements at a time for better performance
    int i = 0;
    for (; i <= n - 4; i += 4) {
        out[i]   = a[i]   - b[i];
        out[i+1] = a[i+1] - b[i+1];
        out[i+2] = a[i+2] - b[i+2];
        out[i+3] = a[i+3] - b[i+3];
    }
    // Process remaining elements
    for (; i < n; i++) {
        out[i] = a[i] - b[i];
    }
}
#endif

#if HAVE_NEON
// NEON-optimized vector scaling
static void vector_scale(
    const float* in, float scale, float* out, int n
) {
    int i = 0;
    float32x4_t scale_vec = vdupq_n_f32(scale);
    
    // Process 4 elements at a time with NEON
    for (; i <= n - 4; i += 4) {
        float32x4_t in_vec = vld1q_f32(in + i);
        float32x4_t out_vec = vmulq_f32(in_vec, scale_vec);
        vst1q_f32(out + i, out_vec);
    }
    
    // Process remaining elements
    for (; i < n; i++) {
        out[i] = in[i] * scale;
    }
}
#else
// Fallback implementation for non-NEON
static void vector_scale(
    const float* in, float scale, float* out, int n
) {
    // Process 4 elements at a time for better performance
    int i = 0;
    for (; i <= n - 4; i += 4) {
        out[i]   = in[i]   * scale;
        out[i+1] = in[i+1] * scale;
        out[i+2] = in[i+2] * scale;
        out[i+3] = in[i+3] * scale;
    }
    // Process remaining elements
    for (; i < n; i++) {
        out[i] = in[i] * scale;
    }
}
#endif
