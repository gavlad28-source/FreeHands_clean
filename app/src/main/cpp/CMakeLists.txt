# Set the minimum required version of CMake.
cmake_minimum_required(VERSION 3.22.1)

# Project definition
project(
    audio_processing
    LANGUAGES C CXX
)

#======================================================================
# Options
#======================================================================

# Set C++ standard to C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

#======================================================================
# Find Dependencies
#======================================================================

# Find the Android logging library
find_library(
    log-lib    # Sets the name of the variable
    log        # Specifies the name of the library to find
)

#======================================================================
# NDK Libraries
#======================================================================

# Add cpufeatures as a library to detect CPU capabilities at runtime.
add_library(cpufeatures STATIC
    ${ANDROID_NDK}/sources/android/cpufeatures/cpu-features.c)
target_include_directories(cpufeatures PUBLIC
    ${ANDROID_NDK}/sources/android/cpufeatures)

#======================================================================
# Main Native Library
#======================================================================

set(AUDIO_PROCESSING_SOURCES
    fft_util.cpp
    mfcc_util.cpp
    native-lib.cpp
    test_interface.cpp
    audio_processing.cpp
)

add_library(
    audio_processing
    SHARED
    ${AUDIO_PROCESSING_SOURCES}
)

# Link dependencies to the main library
target_link_libraries(
    audio_processing
    PRIVATE
    android
    cpufeatures
    ${log-lib}
    -latomic
)

# Set compiler flags for the main library
target_compile_options(audio_processing PRIVATE
    -fexceptions
    -frtti
    -Wall
)

# Set architecture-specific compile definitions
if(ANDROID_ABI STREQUAL "armeabi-v7a" OR ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(audio_processing PRIVATE HAVE_NEON=1)
else()
    target_compile_definitions(audio_processing PRIVATE HAVE_NEON=0)
endif()


#======================================================================
# C++ Unit Tests (using GoogleTest) - DISABLED
#======================================================================

# To re-enable, find your C++ test files and add them to the
# add_executable() command below.

# enable_testing()

# include(FetchContent)
# FetchContent_Declare(
#     googletest
#     URL https://github.com/google/googletest/archive/refs/tags/v1.14.0.zip
# )
# FetchContent_MakeAvailable(googletest)

# add_executable(
#     audio_processing_tests
#     # "test/cpp/some_test.cpp"
# )

# target_link_libraries(
#     audio_processing_tests
#     PRIVATE
#     audio_processing
#     gtest_main
# )

# include(GoogleTest)
# gtest_discover_tests(audio_processing_tests)
