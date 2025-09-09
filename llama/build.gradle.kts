plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "android.llama.cpp"
    compileSdk = 36

    defaultConfig {
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            // Add NDK properties if wanted, e.g.
             //noinspection ChromeOsAbiSupport
             abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DCMAKE_VERBOSE_MAKEFILE=ON"
                arguments += "-DLLAMA_CURL=OFF"
                arguments += "-DGGML_OPENMP=OFF"
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DGGML_LLAMAFILE=OFF"
                arguments += "-DCMAKE_BUILD_TYPE=Release"

                // ✅ Enable GPU (OpenCL / Adreno)
//                arguments += "-DGGML_OPENCL=ON"
//                arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"
//                arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"

                // (Optional) try Vulkan if you want to experiment
//                arguments += "-DGGML_VULKAN=ON"
//                arguments += "-DGGML_VULKAN_CHECK_RESULTS=OFF"  // Skip costly checks
//                arguments += "-DGGML_VULKAN_VALIDATE=OFF"  // Disable validation layers
//                arguments += "-DGGML_VULKAN_DEBUG=OFF"  // Reduce logging overhead
                // Add this to include the Vulkan headers (including vulkan.hpp)
//                arguments += "-DCMAKE_CXX_FLAGS=-I/Users/gokulakrishnanv/VulkanSDK/1.4.321.0/macOS/include"

                cppFlags += listOf()
                arguments += listOf()


                cppFlags("")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
