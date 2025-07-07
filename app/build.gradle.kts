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
    compileSdkVersion(libs.versions.compileSdk.get().toInt())

    defaultConfig {
        applicationId = "br.com.ticpass.pos"
        versionCode = 25
        versionName = "3.0.0"
        androidResources.localeFilters += listOf("pt", "en") // First language is prioritized
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "br.com.ticpass.pos.HiltInstrumentationTestRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"

        buildConfigField("String", "EXODUS_API_KEY", "\"bbe6ebae4ad45a9cbacb17d69739799b8df2c7ae\"")
        buildConfigField("String", "CHECK_DUE_PAYMENTS_INTERVAL", "\"" + getAPICheckDuePaymentsInterval() + "\"")
        buildConfigField("String", "ALERT_DUE_PAYMENTS_INTERVAL", "\"" + getAPIAlertDuePaymentsInterval() + "\"")
        buildConfigField("String", "MAX_DUE_PAYMENTS_DAYS", "\"" + getAPIMaxDuePaymentsDays() + "\"")
        buildConfigField("String", "EVENT_SYNC_INTERVAL", "\"" + getAPIEventSyncInterval() + "\"")
        buildConfigField("String", "TELEMETRY_INTERVAL", "\"" + getAPITelemetryInterval() + "\"")
        buildConfigField("String", "POS_SYNC_INTERVAL", "\"" + getAPIPOSSyncInterval() + "\"")
        buildConfigField("String", "REMOVE_OLD_RECORDS_INTERVAL", "\"" + getPOSOldRecordsRemovalInterval() + "\"")
        buildConfigField("String", "API_HOST", "\"" + getApiHost() + "\"")
        buildConfigField("String", "STONE_QRCODE_AUTH", "\"" + getQRCodeAuthCode() + "\"")
        buildConfigField("String", "STONE_QRCODE_PROVIDER_ID", "\"" + getQRCodeAuthProviderID() + "\"")
        buildConfigField("String", "PASS_REPRINTING_MAX_RETRIES", "\"" + getPassReprintingMaxRetries() + "\"")
        buildConfigField("String", "API_MAX_RETRIES", "\"" + getApiMaxRetries() + "\"")
        buildConfigField("String", "API_TIMEOUT_SECONDS", "\"" + getApiTimeoutSeconds() + "\"")

        missingDimensionStrategy("device", "vanilla")
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["dagger.hilt.disableModulesHaveInstallInCheck"] = "true"
                arguments["room.schemaLocation"] = "$projectDir/schemas".toString()
            }
        }
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
            storeFile = file(AppConfig.gertecStoreFile)
            storePassword = AppConfig.gertecStorePassword
            keyAlias = AppConfig.gertecKeyAlias
            keyPassword = AppConfig.gertecKeyPassword
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
            signingConfig = signingConfigs.getByName("release")
            if (File("signing.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        debug {
            applicationIdSuffix = ".debug"
        }

        create("positivoDebug") {
            initWith(getByName("debug"))
            signingConfig = signingConfigs.getByName("positivo")
        }

        create("gertecDebug") {
            initWith(getByName("debug"))
            signingConfig = signingConfigs.getByName("gertec")
        }
    }

    // Flavors have been removed - using unified codebase instead

    buildFeatures {
        buildConfig = true
        viewBinding = true
        aidl = true
        compose = true
        dataBinding = true
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.cardview)
    implementation(libs.play.services.measurement.api)
    implementation(libs.play.services.games.v2)
    ksp(libs.androidx.room.compiler)
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.0")
    implementation(libs.zxing.core)
    implementation(libs.zxing)


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
    implementation(libs.lottie)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.destinations.core)
    ksp(libs.destinations.ksp)
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

    //Look
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    annotationProcessor(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.material)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.retrofit2)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.datastore)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")


//    // PagSeguro
//    implementation(libs.plugpagservice.wrapper)
//    implementation(libs.reactivex.java)
//    implementation(libs.reactivex.android)
//    // stone
//    implementation(libs.stone.sdk.envconfig)
//    implementation(libs.stone.sdk)
//    implementation(libs.stone.sdk.posandroid)
//    implementation(libs.stone.sdk.sunmi)
//    implementation(libs.stone.sdk.positivo)
//    implementation(libs.stone.sdk.ingenico)
//    implementation(libs.stone.sdk.gertec)


}


fun getPackageCloudReadToken(): String? {
    return project.findProperty("PACKAGE_CLOUD_READ_TOKEN") as? String
}

fun getAPITelemetryInterval(): String? {
    return project.findProperty("API_TELEMETRY_INTERVAL") as? String
}

fun getAPIEventSyncInterval(): String? {
    return project.findProperty("API_EVENT_SYNC_INTERVAL") as? String
}

fun getAPICheckDuePaymentsInterval(): String? {
    return project.findProperty("API_CHECK_DUE_PAYMENTS_INTERVAL") as? String
}

fun getAPIAlertDuePaymentsInterval(): String? {
    return project.findProperty("API_ALERT_DUE_PAYMENTS_INTERVAL") as? String
}

fun getAPIMaxDuePaymentsDays(): String? {
    return project.findProperty("API_MAX_DUE_PAYMENTS_DAYS") as? String
}

fun getAPIPOSSyncInterval(): String? {
    return project.findProperty("API_POS_SYNC_INTERVAL") as? String
}

fun getPOSOldRecordsRemovalInterval(): String? {
    return project.findProperty("REMOVE_OLD_RECORDS_INTERVAL") as? String
}

fun getApiHost(): String {
    return project.findProperty("API_HOST") as String
}

fun getApiMaxRetries(): String {
    return project.findProperty("API_MAX_RETRIES") as String
}

fun getApiTimeoutSeconds(): String {
    return project.findProperty("API_TIMEOUT_SECONDS") as String
}

fun getQRCodeAuthCode(): String {
    return project.findProperty("STONE_QRCODE_AUTH") as String
}

fun getQRCodeAuthProviderID(): String {
    return project.findProperty("STONE_QRCODE_PROVIDER_ID") as String
}

fun getPassReprintingMaxRetries(): String {
    return project.findProperty("PASS_REPRINTING_MAX_RETRIES") as String
}