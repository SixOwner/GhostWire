cmake_minimum_required(VERSION 3.22.1)
project("ghostwire_core")

# Specifies that CMake should locate or compile the native shared library
set(CMAKE_CXX_STANDARD 17)

# Prebuilt or cargo-ndk output target
add_library(ghostwire_core SHARED IMPORTED)
set_target_properties(ghostwire_core PROPERTIES
    IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/../app/src/main/jniLibs/${ANDROID_ABI}/libghostwire_core.so"
)
