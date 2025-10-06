#include <jni.h>
#include <string>
#include "audio_processing.h"

extern "C" JNIEXPORT void JNICALL
Java_com_freehands_assistant_audio_FFTNativeTest_nativeFFT(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray input,
        jfloatArray output,
        jint n) {
    
    jfloat* inputPtr = env->GetFloatArrayElements(input, nullptr);
    jfloat* outputPtr = env->GetFloatArrayElements(output, nullptr);
    
    // Call the FFT function
    fft(inputPtr, outputPtr, n);
    
    // Release the arrays
    env->ReleaseFloatArrayElements(input, inputPtr, JNI_ABORT);
    env->ReleaseFloatArrayElements(output, outputPtr, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_freehands_assistant_audio_MFCCNativeTest_nativeExtractMFCC(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray audioData,
        jint sampleRate,
        jint numCoefficients,
        jfloatArray result) {
    
    jfloat* audioDataPtr = env->GetFloatArrayElements(audioData, nullptr);
    jfloat* resultPtr = env->GetFloatArrayElements(result, nullptr);
    jsize frameSize = env->GetArrayLength(audioData);
    
    // Call the MFCC function
    compute_mfcc(audioDataPtr, frameSize, sampleRate, resultPtr, numCoefficients, 26);
    
    // Release the arrays
    env->ReleaseFloatArrayElements(audioData, audioDataPtr, JNI_ABORT);
    env->ReleaseFloatArrayElements(result, resultPtr, 0);
}
