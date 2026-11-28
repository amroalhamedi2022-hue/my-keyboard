cmake_minimum_required(VERSION 3.22.1)
project(zero_voice LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
include(FetchContent)
FetchContent_Declare(
    whispercpp
    URL https://codeload.github.com/ggml-org/whisper.cpp/tar.gz/a8d002cfd879315632a579e73f0148d06959de36
    URL_HASH SHA256=7b17da903114ed45d82f48c030e3be6a5a8a279f884b2f733706dbb4e832fb6b
    TLS_VERIFY ON
)
FetchContent_MakeAvailable(whispercpp)
add_library(zero_whisper SHARED bridge.cpp)
target_link_libraries(zero_whisper PRIVATE whisper)
