plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.livewallpaper"
    compileSdk = 35 // Keeping 35 as specified

    defaultConfig {
        applicationId = "com.example.livewallpaper"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Ensure this matches the kotlinCompilerExtensionVersion compatible with your compose-bom
        // For '2024.05.00' BOM, you might need a newer Kotlin CE version.
        // As of May 2024, Compose BOM 2024.05.00 generally requires Kotlin CE 1.5.11 or 1.6.x for latest features.
        // However, if 1.5.1 works without errors, keep it. If you get compilation issues, try updating.
        kotlinCompilerExtensionVersion = "1.5.1" // Or higher if needed for your Compose BOM
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX libraries
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1") // Keep if mixing Views
    implementation("com.google.android.material:material:1.12.0") // Keep if mixing Views
    // You likely don't need constraintlayout if you are purely Compose UI
    // implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Remove if not mixing Views
    implementation("androidx.media3:media3-exoplayer:1.3.1") // Or the latest stable version
    implementation("androidx.media3:media3-ui:1.3.1") // For PlayerView in Compose
    // **IMPORTANT: Use the Compose BOM for consistent versions of Compose libraries**
    // This should be the first Compose dependency you list.
    // Check https://developer.android.com/jetpack/compose/bom for the latest stable version.
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    // Jetpack Compose dependencies (versions are managed by the BOM)
    implementation("androidx.activity:activity-compose") // For Compose-enabled Activity
    implementation("androidx.compose.ui:ui") // Core UI components
    implementation("androidx.compose.ui:ui-graphics") // Graphics utilities for Compose
    implementation("androidx.compose.ui:ui-tooling-preview") // For @Preview annotation
    implementation("androidx.compose.material3:material3") // Material Design 3 components

    // **FIX FOR NAVIGATION DUPLICATE CLASS ERROR:**
    // Use ONLY this one for Compose Navigation. It pulls in what's needed.
    //implementation("androidx.navigation:navigation-compose") // Version managed by BOM if using BOM
    // If you are NOT using the BOM, you would specify the version directly here:
    implementation("androidx.navigation:navigation-compose:2.7.7") // Example version

    // **Streamlined ExoPlayer dependencies:**
    // The media3-exoplayer and media3-ui artifacts are the recommended way to use ExoPlayer with Media3.
    // They bring in the necessary core components. Avoid mixing with older `com.google.android.exoplayer` artifacts
    // unless you have a specific reason (e.g., specific features not yet in Media3).
    implementation("androidx.media3:media3-exoplayer:1.3.1") // Or latest stable Media3 version
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation(libs.espresso.core)       // Or latest stable Media3 version

    // Remove these older ExoPlayer lines as they are likely redundant or conflict with media3
    // implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    // implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    // implementation("com.google.android.exoplayer:exoplayer:2.18.0")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Keep if needed for traditional UI tests

    // Debugging tools for Compose previews
    debugImplementation("androidx.compose.ui:ui-tooling") // Tooling for previews
    debugImplementation("androidx.compose.ui:ui-test-manifest") // Required for Compose UI tests
}