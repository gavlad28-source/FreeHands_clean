cmake_minimum_required(VERSION 3.22.1)

# Set the project name
project(audio_processing)

# Add the math library to the target
find_library(log-lib log)

# Include the NDK's android-ndk-stl directory
include_directories(${ANDROID_NDK}/sources/android/native_app_glue)

# Enable testing
enable_testing()

# Find and include GoogleTest
include(FetchContent)
FetchContent_Declare(
    googletest
    URL https://github.com/google/googletest/archive/refs/tags/v1.14.0.zip
)
FetchContent_MakeAvailable(googletest)

# Add cpufeatures as a static library
add_library(cpufeatures STATIC
    ${ANDROID_NDK}/sources/android/cpufeatures/cpu-features.c
)

# Set include directory for cpufeatures
target_include_directories(cpufeatures PRIVATE
    ${ANDROID_NDK}/sources/android/cpufeatures
)

# List all source files for the main library
file(GLOB_RECURSE AUDIO_PROCESSING_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/*.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/*.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/../jni/*.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/../jni/*.c"
)

# Define the main library
add_library(
    audio_processing
    SHARED
    ${AUDIO_PROCESSING_SOURCES}
)

# Find test files if they exist
file(GLOB_RECURSE TEST_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/../test/cpp/**/*.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/../test/jni/*.cpp"
)

# Only add test executable if test files exist
if(TEST_SOURCES)
    # Create a test executable
    add_executable(
        audio_processing_tests
        ${TEST_SOURCES}
    )
    
    # Link test executable with gtest and the main library
    target_link_libraries(audio_processing_tests
        PRIVATE
        gtest_main
        audio_processing
    )
    
    # Add the test to CTest
    include(GoogleTest)
    gtest_discover_tests(audio_processing_tests)
endif()

# Removed duplicate target_link_libraries

# Add test
add_test(NAME audio_processing_tests COMMAND audio_processing_tests)

# Link the libraries
target_link_libraries(
        audio_processing
        android
        ${log-lib}
        cpufeatures
        -latomic
)

# Enable C++17
target_compile_features(audio_processing PRIVATE cxx_std_17)

# Set common compile flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -frtti -fno-limit-debug-info")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fexceptions -fno-limit-debug-info")

# Set architecture-specific flags
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    # Enable NEON for ARMv7
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfloat-abi=softfp -mfpu=neon -march=armv7-a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mfloat-abi=softfp -mfpu=neon -march=armv7-a")
    target_compile_definitions(audio_processing PRIVATE HAVE_NEON=1)
    
elseif(ANDROID_ABI STREQUAL "arm64-v8a")
    # ARM64 always has NEON
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8-a")
    target_compile_definitions(audio_processing PRIVATE HAVE_NEON=1)
    
elseif(ANDROID_ABI STREQUAL "x86" OR ANDROID_ABI STREQUAL "x86_64")
    # x86 doesn't support NEON, use SSE/SSE2 instead
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -msse2 -mfpmath=sse")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -msse2 -mfpmath=sse")
    target_compile_definitions(audio_processing PRIVATE HAVE_NEON=0)
    
else()
    # For other ABIs, disable NEON
    target_compile_definitions(audio_processing PRIVATE HAVE_NEON=0)
    message(WARNING "Unsupported ABI: ${ANDROID_ABI}, NEON optimizations disabled")
endif()

# Add include directories for cpufeatures (used for runtime CPU detection)
include_directories(${ANDROID_NDK}/sources/cpufeatures)
include_directories(${ANDROID_NDK}/sources/android/cpufeatures)

# Enable RTTI and exceptions
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -frtti")
