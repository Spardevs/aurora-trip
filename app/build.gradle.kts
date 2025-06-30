/*
 * Copyright (c) 2025 Ticpass. All rights reserved.
 *
 * PROPRIETARY AND CONFIDENTIAL
 *
 * This software is the confidential and proprietary information of Ticpass
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Ticpass.
 *
 * Unauthorized copying, distribution, or use of this software, via any medium,
 * is strictly prohibited without the express written permission of Ticpass.
 */

import java.util.Properties

PropertyDelegates.init(project)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.navigation)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.rikka.tools.refine.plugin)
    alias(libs.plugins.hilt.android.plugin)
}

val lastCommitHash = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.map { it.trim() }

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "br.com.ticpass.pos"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "br.com.ticpass.pos"
        versionCode = 25
        versionName = "2.23.0"
        resConfigs("pt", "en") // First language is prioritized


        testInstrumentationRunner = "br.com.ticpass.pos.HiltInstrumentationTestRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"

        buildConfigField("String", "EXODUS_API_KEY", "\"bbe6ebae4ad45a9cbacb17d69739799b8df2c7ae\"")

        missingDimensionStrategy("device", "vanilla")
    }

    signingConfigs {
        create("release") {
            storeFile = file(AppConfig.releaseStoreFile)
            storePassword = AppConfig.releaseStorePassword
            keyAlias = AppConfig.releaseKeyAlias
            keyPassword = AppConfig.releaseKeyPassword
        }

        create("positivo") {
            storeFile = file(AppConfig.positivoStoreFile)
            storePassword = AppConfig.positivoStorePassword
            keyAlias = AppConfig.positivoKeyAlias
            keyPassword = AppConfig.positivoKeyPassword
        }

        create("gertec") {
            storeFile = file(AppConfig.positivoStoreFile)
            storePassword = AppConfig.positivoStorePassword
            keyAlias = AppConfig.positivoKeyAlias
            keyPassword = AppConfig.positivoKeyPassword
        }
    }

    flavorDimensions += "acquirers"
    productFlavors {
        create("pagseguro") {
            dimension = "acquirers"
            applicationIdSuffix = ".amalerinha"
            versionNameSuffix = "-amalerinha"
            minSdk = libs.versions.pagseguroMinSdk.get().toInt()
            targetSdk = libs.versions.pagseguroTargetSdk.get().toInt()
        }

        create("stone") {
            dimension = "acquirers"
            applicationIdSuffix = ".pedra"
            versionNameSuffix = "-pedra"
            minSdk = libs.versions.stoneMinSdk.get().toInt()
            targetSdk = libs.versions.stoneTargetSdk.get().toInt()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (File("signing.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        debug {
            applicationIdSuffix = ".debug"
        }
    }

    // Flavors have been removed - using unified codebase instead

    buildFeatures {
        buildConfig = true
        viewBinding = true
        aidl = true
        compose = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        lintConfig = file("lint.xml")
    }

    androidResources {
        generateLocaleConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

// androidComponents block removed as flavors are now unified

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    //Google's Goodies
    implementation(libs.google.android.material)
    implementation(libs.google.protobuf.javalite)

    //AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Coil
    implementation(libs.coil.kt)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    //Shimmer
    implementation(libs.facebook.shimmer)

    //Epoxy
    implementation(libs.airbnb.epoxy.android)
    ksp(libs.airbnb.epoxy.processor)

    //HTTP Clients
    implementation(libs.squareup.okhttp)

    //Lib-SU
    implementation(libs.github.topjohnwu.libsu)

    //GPlayApi
    implementation(libs.auroraoss.gplayapi)

    //Shizuku
    compileOnly(libs.rikka.hidden.stub)
    implementation(libs.rikka.tools.refine.runtime)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.lsposed.hiddenapibypass)

    //Test
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.espresso.core)

    //Hilt
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.androidx.compiler)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.hilt.android.core)
    implementation(libs.hilt.androidx.work)

    kspAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.hilt.android.testing)

    //Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    implementation(libs.process.phoenix)

    // LeakCanary
    debugImplementation(libs.squareup.leakcanary.android)
}
