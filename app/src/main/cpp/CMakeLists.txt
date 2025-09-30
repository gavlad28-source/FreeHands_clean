cmake_minimum_required(VERSION 3.22.1)

# Set the project name
project(audio_processing)

# Add the math library to the target
find_library(log-lib log)

# Include the NDK's android-ndk-stl directory which contains the NEON headers
include_directories(${ANDROID_NDK}/sources/android/cpufeatures)
include_directories(${ANDROID_NDK}/sources/android/native_app_glue)

# Define the library
add_library(
        audio_processing
        SHARED
        audio_processing.cpp
        fft_util.cpp
        mfcc_util.cpp
        native-lib.cpp
)

# Link the libraries
target_link_libraries(
        audio_processing
        android
        ${log-lib}
        cpufeatures
)

# Enable C++17 and NEON optimizations
target_compile_features(audio_processing PRIVATE cxx_std_17)

if(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(audio_processing PRIVATE -mfloat-abi=softfp -mfpu=neon)
    target_compile_definitions(audio_processing PRIVATE -DHAVE_NEON=1)
elseif(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(audio_processing PRIVATE -DHAVE_NEON=1)
endif()

# Enable RTTI and exceptions
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -frtti")
