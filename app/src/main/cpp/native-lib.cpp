#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "audio_processing.h"

#define LOG_TAG "AudioFeatureExtractor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define JNI_METHOD(return_type, method_name) \
  JNIEXPORT return_type JNICALL \
  Java_com_freehands_assistant_utils_AudioFeatureExtractor_##method_name

extern "C" {

// Initialize native resources
JNI_METHOD(jlong, nativeInit)(JNIEnv* env, jobject /* this */) {
    // Log CPU features and NEON availability
    #if HAVE_NEON
    LOGD("Initializing with NEON optimizations");
    #else
    LOGD("Initializing without NEON optimizations");
    #endif
    
    // We can initialize any resources here if needed
    return 0;  // Return a handle if needed
}

// Cleanup native resources
JNI_METHOD(void, nativeRelease)(JNIEnv* env, jobject /* this */, jlong handle) {
    // Cleanup resources if needed
}

// Compute MFCC features
JNI_METHOD(jfloatArray, nativeComputeMfcc)(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray audio_data,
    jint num_samples,
    jint sample_rate,
    jint num_mfcc,
    jint num_filters
) {
    // Validate input parameters
    if (audio_data == nullptr || num_samples <= 0 || sample_rate <= 0 || num_mfcc <= 0 || num_filters <= 0) {
        LOGE("Invalid input parameters");
        return nullptr;
    }

    // Get input array
    jfloat* audio_data_ptr = env->GetFloatArrayElements(audio_data, nullptr);
    if (audio_data_ptr == nullptr) {
        LOGE("Failed to get audio data array");
        return nullptr;  // Out of memory or other error
    }

    // Allocate output array
    jfloatArray result = env->NewFloatArray(num_mfcc);
    if (result == nullptr) {
        LOGE("Failed to allocate result array");
        env->ReleaseFloatArrayElements(audio_data, audio_data_ptr, JNI_ABORT);
        return nullptr;  // Out of memory
    }

    try {
        // Compute MFCC
        std::vector<float> mfcc(num_mfcc);
        compute_mfcc(
            audio_data_ptr,  // Input audio data
            num_samples,     // Number of samples
            sample_rate,     // Sample rate
            mfcc.data(),     // Output MFCCs
            num_mfcc,        // Number of MFCC coefficients
            num_filters      // Number of Mel filters
        );

        // Set the result and release resources
        env->SetFloatArrayRegion(result, 0, num_mfcc, mfcc.data());
    } catch (const std::exception& e) {
        LOGE("Exception in MFCC computation: %s", e.what());
        env->ReleaseFloatArrayElements(audio_data, audio_data_ptr, JNI_ABORT);
        return nullptr;
    } catch (...) {
        LOGE("Unknown exception in MFCC computation");
        env->ReleaseFloatArrayElements(audio_data, audio_data_ptr, JNI_ABORT);
        return nullptr;
    }
    
    // Release input array
    env->ReleaseFloatArrayElements(audio_data, audio_data_ptr, JNI_ABORT);
    return result;
}

// Compute FFT
JNI_METHOD(jfloatArray, nativeFft)(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray input,
    jint n
) {
    // Validate input parameters
    if (input == nullptr || n <= 0 || (n & (n - 1)) != 0) {
        LOGE("Invalid input parameters for FFT: n=%d", n);
        return nullptr;  // n must be a power of 2
    }

    // Get input array
    jfloat* input_ptr = env->GetFloatArrayElements(input, nullptr);
    if (input_ptr == nullptr) {
        LOGE("Failed to get input array for FFT");
        return nullptr;  // Out of memory or other error
    }

    // Allocate output array (complex numbers, so 2*n elements)
    jfloatArray result = env->NewFloatArray(2 * n);
    if (result == nullptr) {
        LOGE("Failed to allocate result array for FFT");
        env->ReleaseFloatArrayElements(input, input_ptr, JNI_ABORT);
        return nullptr;  // Out of memory
    }

    try {
        // Allocate temporary buffer for FFT output
        std::vector<float> fft_output(2 * n);

        // Compute FFT
        fft(input_ptr, fft_output.data(), n);

        // Copy result to Java array
        env->SetFloatArrayRegion(result, 0, 2 * n, fft_output.data());
    } catch (const std::exception& e) {
        LOGE("Exception in FFT computation: %s", e.what());
        env->ReleaseFloatArrayElements(input, input_ptr, JNI_ABORT);
        return nullptr;
    } catch (...) {
        LOGE("Unknown exception in FFT computation");
        env->ReleaseFloatArrayElements(input, input_ptr, JNI_ABORT);
        return nullptr;
    }

    // Release input array
    env->ReleaseFloatArrayElements(input, input_ptr, JNI_ABORT);
    return result;
}

}  // extern "C"
