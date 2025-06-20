// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false // Add "apply false" to avoid applying at this level
}

// Configure the buildscript block if required
buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter() // Optional, for backward compatibility
        maven {
            url = uri("https://jitpack.io") // Correct syntax for JitPack
        }    }
    dependencies {
        // Additional dependencies for the build script (if required)
    }
}

// Add repositories for all sub-projects/modules
allprojects {
    repositories {
    }
}
