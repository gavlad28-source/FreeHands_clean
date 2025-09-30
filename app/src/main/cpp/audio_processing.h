#ifndef AUDIO_PROCESSING_H
#define AUDIO_PROCESSING_H

#include <jni.h>
#include <vector>

#ifdef __cplusplus
extern "C" {
#endif

// FFT functions
void fft(float* input, float* output, int n);
void ifft(float* input, float* output, int n);

// MFCC functions
void compute_mfcc(const float* audio_data, int num_samples, int sample_rate,
                 float* mfcc, int num_mfcc, int num_filters);

// Utility functions
void apply_hamming_window(float* data, int n);
void power_spectrum(const float* fft_real, const float* fft_imag, float* power, int n);
void apply_mel_filter_bank(const float* power_spectrum, int fft_size, float sample_rate,
                          float* mel_energies, int num_filters);
void dct(const float* input, float* output, int n, int m);

#ifdef __cplusplus
}
#endif

#endif // AUDIO_PROCESSING_H
