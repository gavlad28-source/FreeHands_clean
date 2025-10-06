#include <gtest/gtest.h>
#include <vector>
#include <cmath>
#include "audio_processing.h"

class MFCCTest : public ::testing::Test {
protected:
    void SetUp() override {
        // Initialize test data
        const int frameSize = 512;
        sampleRate = 16000; // 16kHz sample rate
        numFilters = 26;
        
        // Create a simple frame with a 1kHz sine wave
        frame.resize(frameSize);
        for (int i = 0; i < frameSize; i++) {
            frame[i] = 0.5f * sin(2.0f * M_PI * 1000.0f * i / sampleRate);
        }
    }
    
    std::vector<float> frame;
    int sampleRate;
    int numFilters;
};

TEST_F(MFCCTest, TestMFCCBasic) {
    const int numMfcc = 13; // Standard number of MFCC coefficients
    std::vector<float> mfcc(numMfcc);
    
    // Call the MFCC computation function
    compute_mfcc(frame.data(), frame.size(), sampleRate, mfcc.data(), numMfcc, numFilters);
    
    // Basic test: check if we got the expected number of coefficients
    ASSERT_EQ(mfcc.size(), numMfcc) << "Incorrect number of MFCC coefficients";
    
    // Test that not all coefficients are zero
    bool allZero = true;
    for (float coeff : mfcc) {
        if (std::abs(coeff) > 1e-6) {
            allZero = false;
            break;
        }
    }
    EXPECT_FALSE(allZero) << "MFCC coefficients should not be all zero";
    
    // Test that the first coefficient (energy) is positive
    EXPECT_GT(mfcc[0], 0.0f) << "First MFCC coefficient (energy) should be positive";
}

TEST_F(MFCCTest, TestMFCCSilence) {
    const int numMfcc = 13;
    std::vector<float> silenceFrame(frame.size(), 0.0f);
    std::vector<float> mfcc(numMfcc);
    
    // Call the MFCC computation function with silence
    compute_mfcc(silenceFrame.data(), silenceFrame.size(), sampleRate, 
                mfcc.data(), numMfcc, numFilters);
    
    // With silence, the energy (first coefficient) should be very small but not NaN or Inf
    EXPECT_FALSE(std::isnan(mfcc[0])) << "MFCC energy should not be NaN";
    EXPECT_FALSE(std::isinf(mfcc[0])) << "MFCC energy should not be infinite";
    
    // The log of a very small number will be negative, but not -inf
    EXPECT_LT(mfcc[0], 0.0f) << "MFCC energy should be negative for silence";
}
