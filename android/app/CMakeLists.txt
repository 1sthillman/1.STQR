cmake_minimum_required(VERSION 3.22.1)

project("llama_android")

# C++17 gerekli
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Llama.cpp kaynak dosyaları (submodule veya direkt olarak)
# NOT: Gerçek entegrasyon için llama.cpp reposunu clone etmek gerekir
# Şimdilik sadece wrapper'ı derliyoruz

# JNI Wrapper
add_library(llama_android SHARED
        src/main/cpp/llama_android.cpp)

# Android log library bağla
find_library(log-lib log)
target_link_libraries(llama_android ${log-lib})

# Llama.cpp library burada link edilecek (gerçek entegrasyonda)
# target_link_libraries(llama_android llama)






























