# Audio Feature Extraction Module

This document provides an overview of the audio feature extraction implementation in the FreeHands application, including performance optimizations, usage guidelines, and testing procedures.

## Overview

The audio feature extraction module is responsible for converting raw audio signals into Mel-Frequency Cepstral Coefficients (MFCCs), which are commonly used in speech and audio processing tasks. The implementation includes both Kotlin and native (C++) versions for maximum performance.

## Key Features

- **High-performance MFCC extraction** using optimized native code
- **SIMD (NEON) optimizations** for ARM processors
- **Object pooling** to minimize memory allocations
- **Multi-threading support** for parallel processing
- **Comprehensive test coverage** with unit and instrumentation tests
- **Benchmarking** for performance monitoring

## Performance Optimizations

### 1. Native Implementation
- Critical algorithms implemented in C++
- NEON SIMD instructions for vectorized math operations
- Efficient memory management with minimal JNI overhead

### 2. Object Pooling
- Reusable buffers for FFT and MFCC calculations
- Thread-local storage for thread safety
- Reduced garbage collection pressure

### 3. Algorithmic Optimizations
- Pre-computed constants where possible
- Optimized FFT implementation with bit-reversal
- Efficient Mel filter bank implementation
- Vectorized math operations

### 4. Memory Management
- Zero-copy operations where possible
- Efficient buffer reuse
- Proper cleanup of native resources

## Performance Benchmarks

### Test Setup
- Device: [Device Model]
- Android Version: [Android Version]
- Input: 16kHz, 16-bit PCM audio
- MFCC Configuration: 13 coefficients, 40 filter banks

### Results

| Test Case | Avg. Processing Time | Real-time Factor | Memory Usage |
|-----------|----------------------|------------------|--------------|
| Silence   | X.XX ms/sec          | XX.XXx           | XX MB        |
| Sine Wave | X.XX ms/sec          | XX.XXx           | XX MB        |
| Speech    | X.XX ms/sec          | XX.XXx           | XX MB        |
| Noise     | X.XX ms/sec          | XX.XXx           | XX MB        |

*Note: Run the benchmark on target devices for accurate measurements.*

## Usage

### Basic Usage

```kotlin
val extractor = AudioFeatureExtractor()
val audioSamples: ShortArray = // Your audio data (16kHz, 16-bit PCM)
val mfccFeatures = extractor.extractFeatures(audioSamples)
```

### Configuration

```kotlin
val extractor = AudioFeatureExtractor().apply {
    sampleRate = 16000  // Input sample rate in Hz
    numMfcc = 13        // Number of MFCC coefficients to extract
    numFilterBanks = 40 // Number of Mel filter banks
    frameSizeMs = 25    // Frame size in milliseconds
    frameStepMs = 10    // Frame step in milliseconds
}
```

### Error Handling

```kotlin
try {
    val features = extractor.extractFeatures(audioSamples)
    // Process features
} catch (e: AudioProcessingException) {
    // Handle error
    Log.e("AudioFeatureExtractor", "Error processing audio: ${e.message}", e)
}
```

## Testing

### Unit Tests

Run the unit tests to verify the correctness of the implementation:

```bash
./gradlew testDebugUnitTest
```

### Instrumentation Tests

Run the instrumentation tests on a connected device or emulator:

```bash
./gradlew connectedAndroidTest
```

### Benchmarking

Run the benchmarks to measure performance:

```bash
./gradlew benchmark:connectedCheck
```

## Native Code

The native implementation is located in the `app/src/main/cpp` directory:

- `audio_processing.h/cpp`: Core audio processing functions
- `fft_util.h/cpp`: FFT implementation with NEON optimizations
- `mfcc_util.h/cpp`: MFCC calculation utilities
- `native-lib.cpp`: JNI bridge code

## Best Practices

1. **Reuse Instances**: Reuse `AudioFeatureExtractor` instances when possible to benefit from object pooling.
2. **Batch Processing**: Process audio in chunks that match your application's real-time requirements.
3. **Error Handling**: Always handle potential errors, especially for malformed input.
4. **Thread Safety**: The extractor is not thread-safe. Use separate instances or external synchronization.
5. **Memory Management**: Be mindful of memory usage when processing long audio streams.

## Troubleshooting

### Common Issues

1. **Native Library Not Found**
   - Ensure the native library is properly linked in your build.gradle
   - Check that the ABI filters match your device

2. **Poor Performance**
   - Verify that the native library is being loaded
   - Check for thermal throttling on the device
   - Ensure the app is running in release mode for accurate performance

3. **Incorrect MFCC Values**
   - Verify the input audio format (16kHz, 16-bit PCM)
   - Check the configuration parameters (sample rate, FFT size, etc.)

## Future Improvements

- [ ] Support for variable frame sizes
- [ ] Additional feature extraction methods (e.g., spectral features)
- [ ] More aggressive SIMD optimizations
- [ ] Quantization for faster inference
- [ ] Support for different audio formats

## License

[Your License Information]
