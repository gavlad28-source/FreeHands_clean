#include <gtest/gtest.h>
#include <cmath>
#include <vector>
#include "audio_processing.h"

class FFTTest : public ::testing::Test {
protected:
    void SetUp() override {
        // Initialize test data
        const int N = 8;
        inputReal.resize(N * 2);
        inputImag.resize(N * 2, 0.0f);
        outputReal.resize(N * 2);
        outputImag.resize(N * 2);
        
        // Simple sine wave input
        for (int i = 0; i < N; i++) {
            inputReal[2*i] = sin(2.0f * M_PI * i / N);
            inputReal[2*i + 1] = 0.0f; // Imaginary part
        }
    }
    
    std::vector<float> inputReal;
    std::vector<float> inputImag;
    std::vector<float> outputReal;
    std::vector<float> outputImag;
};

TEST_F(FFTTest, TestFFTBasic) {
    const int N = 8;
    
    // Call the FFT function
    fft(inputReal.data(), outputReal.data(), N);
    
    // Simple test: check if the output has non-zero values
    bool hasNonZero = false;
    for (int i = 0; i < N; i++) {
        if (std::abs(outputReal[2*i]) > 1e-6) {
            hasNonZero = true;
            break;
        }
    }
    EXPECT_TRUE(hasNonZero) << "FFT output should have non-zero values";
    
    // Test Parseval's theorem: sum of squares should be preserved
    float inputEnergy = 0.0f;
    float outputEnergy = 0.0f;
    
    for (int i = 0; i < N; i++) {
        inputEnergy += inputReal[2*i] * inputReal[2*i];
    }
    
    for (int i = 0; i < N; i++) {
        outputEnergy += outputReal[2*i] * outputReal[2*i] + 
                       (2*i+1 < 2*N ? outputReal[2*i+1] * outputReal[2*i+1] : 0);
    }
    
    // Normalize output energy (FFT doesn't preserve the scale)
    outputEnergy /= N * N;
    
    EXPECT_NEAR(inputEnergy, outputEnergy, 1e-4) << "FFT should preserve energy (Parseval's theorem)";
}
