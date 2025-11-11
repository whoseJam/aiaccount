cmake_minimum_required(VERSION 3.22.1)

project("whisperandroid")

add_subdirectory(whisper.cpp)

include_directories(
    whisper.cpp/include
)

add_library(
    whisperandroid
    SHARED
    native-lib.cpp
)

target_link_libraries(
    whisperandroid
    whisper
    log
)