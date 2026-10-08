cmake_minimum_required(VERSION 3.22.1)
project("piper-android")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(piper-android SHARED
    piper-jni.cpp
)

find_library(log-lib log)

target_link_libraries(piper-android
    ${log-lib}
)
