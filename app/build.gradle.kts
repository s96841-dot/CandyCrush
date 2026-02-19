plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.CandyCrush"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.CandyCrush"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    val geminiApiKey = project.findProperty("GEMINI_API_KEY") as String? ?: ""
    buildTypes {
        debug {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        }
        release {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
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
}

dependencies {
    // 1. Import the BOM (Bill of Materials) FIRST
    implementation(platform(libs.firebase.bom))

    // 2. Firebase Libraries (No versions needed here, BOM handles it)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck)

    // 3. UI and AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // 4. Networking and Media
    implementation(libs.glide)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    // 5. Misc
    implementation(libs.guava)
    implementation(libs.reactive.streams)

    // 6. Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}