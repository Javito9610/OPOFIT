plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.opofit.miapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.opofit.miapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ============ KAPT (necesario para Hilt) ============
    kapt("com.google.dagger:hilt-compiler:2.48")

    // ============ FIREBASE ============
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // ============ HILT (Inyección de dependencias) ============
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ============ COROUTINES (Para operaciones async) ============
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ============ COIL (Para cargar imágenes) ============
    implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha06")

    // ============ NAVIGATION (Para navegar entre pantallas) ============
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ============ DATASTORE (Para guardar preferencias) ============
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ============ RETROFIT (Para APIs HTTP - si lo necesitas) ============
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.0")
}