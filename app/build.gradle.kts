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
    val geminiApiKey = project.findProperty("AIzaSyDbtB40Iam_6HRIostrEb52YdW0lExgBs0") as String? ?: ""
    buildTypes {
        debug {
            buildConfigField("String", "AIzaSyDbtB40Iam_6HRIostrEb52YdW0lExgBs0", "\"$geminiApiKey\"")
        }
        release {
            buildConfigField("String", "AIzaSyDbtB40Iam_6HRIostrEb52YdW0lExgBs0", "\"$geminiApiKey\"")
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
    implementation(libs.glide)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.okhttp)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2")) // Add the Firebase BOM
    implementation("com.google.firebase:firebase-storage")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.storage)
    implementation("com.google.firebase:firebase-analytics-ktx") // Example, add what you need
    implementation("com.google.firebase:firebase-appcheck-playintegrity")


}