#include <jni.h>
#include <string>
#include <vector>
#include "audio_processing.h"

#define JNI_METHOD(return_type, method_name) \
  JNIEXPORT return_type JNICALL \
  Java_com_freehands_assistant_utils_AudioFeatureExtractor_##method_name

extern "C" {

// Initialize native resources
JNI_METHOD(jlong, nativeInit)(JNIEnv* env, jobject /* this */) {
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
    // Get input array
    jfloat* audio_data_ptr = env->GetFloatArrayElements(audio_data, nullptr);
    if (audio_data_ptr == nullptr) {
        return nullptr;  // Out of memory or other error
    }

    // Allocate output array
    jfloatArray result = env->NewFloatArray(num_mfcc);
    if (result == nullptr) {
        env->ReleaseFloatArrayElements(audio_data, audio_data_ptr, JNI_ABORT);
        return nullptr;  // Out of memory
    }

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
    // Get input array
    jfloat* input_ptr = env->GetFloatArrayElements(input, nullptr);
    if (input_ptr == nullptr) {
        return nullptr;  // Out of memory or other error
    }

    // Allocate output array (complex numbers, so 2*n elements)
    jfloatArray result = env->NewFloatArray(2 * n);
    if (result == nullptr) {
        env->ReleaseFloatArrayElements(input, input_ptr, JNI_ABORT);
        return nullptr;  // Out of memory
    }

    // Compute FFT
    std::vector<float> output(2 * n);
    fft(
        input_ptr,      // Input data (real)
        output.data(),  // Output data (interleaved real/imaginary)
        n               // FFT size
    );

    // Set the result and release resources
    env->SetFloatArrayRegion(result, 0, 2 * n, output.data());
    env->ReleaseFloatArrayElements(input, input_ptr, JNI_ABORT);
    
    return result;
}

}  // extern "C"
