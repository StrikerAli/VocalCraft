plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Apply the plugin here
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.aliashraf.vocalcraft"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aliashraf.vocalcraft"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("com.google.firebase:firebase-bom:33.4.0") // Use the BOM for Firebase
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation ("com.google.firebase:firebase-auth:23.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("com.github.yukuku:ambilwarna:2.0.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.foundation.android)
    implementation(libs.firebase.storage)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation (libs.androidsvg)
    implementation ("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
    implementation ("com.github.yukuku:ambilwarna:2.0.1")

    implementation (libs.dropbox.core.sdk)

    implementation (libs.glide)

    // Other dependencies you might need


}
apply(plugin = "com.google.gms.google-services")
