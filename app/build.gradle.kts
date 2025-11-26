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
        buildConfigField("String", "CHECK_DUE_PAYMENTS_INTERVAL", "\"" + AppConfig.apiCheckDuePaymentsInterval + "\"")
        buildConfigField("String", "ALERT_DUE_PAYMENTS_INTERVAL", "\"" + AppConfig.apiAlertDuePaymentsInterval + "\"")
        buildConfigField("String", "MAX_DUE_PAYMENTS_DAYS", "\"" + AppConfig.apiMaxDuePaymentsDays + "\"")
        buildConfigField("String", "EVENT_SYNC_INTERVAL", "\"" + AppConfig.apiEventSyncInterval + "\"")
        buildConfigField("String", "TELEMETRY_INTERVAL", "\"" + AppConfig.apiTelemetryInterval + "\"")
        buildConfigField("String", "POS_SYNC_INTERVAL", "\"" + AppConfig.apiPosSyncInterval + "\"")
        buildConfigField("String", "REMOVE_OLD_RECORDS_INTERVAL", "\"" + AppConfig.posOldRecordsRemovalInterval + "\"")
        buildConfigField("String", "API_HOST", "\"" + AppConfig.apiHost + "\"")
        buildConfigField("String", "STONE_QRCODE_AUTH", "\"" + AppConfig.qrCodeAuthCode + "\"")
        buildConfigField("String", "STONE_QRCODE_PROVIDER_ID", "\"" + AppConfig.qrCodeAuthProviderId + "\"")
        buildConfigField("String", "PASS_REPRINTING_MAX_RETRIES", "\"" + AppConfig.passReprintingMaxRetries + "\"")
        buildConfigField("String", "API_MAX_RETRIES", "\"" + AppConfig.apiMaxRetries + "\"")
        buildConfigField("String", "API_TIMEOUT_SECONDS", "\"" + AppConfig.apiTimeoutSeconds + "\"")
        buildConfigField("String", "PACKAGE_CLOUD_READ_TOKEN", "\"" + AppConfig.packageCloudReadToken + "\"")
        buildConfigField("String", "CONVERSION_FACTOR", "\"" + AppConfig.conversionFactor + "\"")
        buildConfigField("String", "APP_NAME", "\"${AppConfig.appName}\"")
        buildConfigField("String", "NFC_KEY_TYPE_A", "\"${AppConfig.nfcKeyTypeA}\"")
        buildConfigField("String", "NFC_KEY_TYPE_B", "\"${AppConfig.nfcKeyTypeB}\"")
        buildConfigField("String", "MOCK_DEVICE_MODEL", "\"${AppConfig.mockDeviceModel}\"")
        buildConfigField("String", "MOCK_DEVICE_SERIAL", "\"${AppConfig.mockDeviceSerial}\"")

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
        create("proprietaryGertec") {
            dimension = "acquirers"
            applicationIdSuffix = ".proprietary.gertec"
            versionNameSuffix = "-proprietary-gertec"
            minSdk = libs.versions.pagseguroMinSdk.get().toInt()
            targetSdk = libs.versions.pagseguroTargetSdk.get().toInt()
        }

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
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
            if (File("signing.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        create("sunmi") {
            initWith(getByName("release"))
            versionNameSuffix = "-sunmi"
        }

        create("sunmiSeriesP") {
            initWith(getByName("release"))
            versionNameSuffix = "-sunmiSeriesP"
        }

        create("ingenico") {
            initWith(getByName("release"))
            versionNameSuffix = "-ingenico"
        }

        create("tectoySeriesT") {
            initWith(getByName("release"))
            versionNameSuffix = "-tectoySeriesT"
        }

        create("sk210") {
            initWith(getByName("release"))
            versionNameSuffix = "-sk210"
            signingConfig = signingConfigs.getByName("gertec")
        }

        create("gertecGpos700") {
            initWith(getByName("release"))
            versionNameSuffix = "-gertecGpos700"
            signingConfig = signingConfigs.getByName("gertec")
        }

        create("gertecGpos760") {
            initWith(getByName("release"))
            versionNameSuffix = "-gertecGpos760"
            signingConfig = signingConfigs.getByName("gertec")
        }

        create("positivoSeriesL") {
            initWith(getByName("release"))
            versionNameSuffix = "-positivoSeriesL"
            signingConfig = signingConfigs.getByName("positivo")
        }
    }

    configurations {
        register("stoneDebugImplementation")
    }

    androidComponents {
        beforeVariants { variantBuilder ->
            val flavorName = variantBuilder.productFlavors.first().second
            val buildType = variantBuilder.buildType
            val isProprietaryGertec = flavorName == "proprietaryGertec"
            val isPagSeguro = flavorName == "pagseguro"
            val isStone = flavorName == "stone"
            val isDebug = buildType == "debug"
            val isRelease = buildType == "release"
            val isSk210 = buildType == "sk210"

            // proprietaryGertec only supports debug and sk210 build types
            if (isProprietaryGertec && !(isDebug || isSk210)) {
                variantBuilder.enable = false
            }

            // pagseguro only supports release and debug build types (no device-specific builds)
            if (isPagSeguro && !(isDebug || isRelease)) {
                variantBuilder.enable = false
            }

            // stone supports all build types except release and sk210
            if (isStone && (isRelease || isSk210)) {
                variantBuilder.enable = false
            }
        }
    }

    // Custom tasks to build stone variants
    afterEvaluate {
        val stoneVariantTaskNames = mutableListOf<String>()
        val stoneVariants = mutableListOf<String>()

        // Collect all stone variant task names (except debug)
        android.applicationVariants.configureEach {
            if (flavorName == "stone" && buildType.name != "debug") {
                val taskName = "assemble${name.capitalize()}"
                stoneVariantTaskNames.add(taskName)
                stoneVariants.add(taskName)
            }
        }

        tasks.register("buildStoneRelease") {
            group = "custom"
            description = "Builds all Stone variants"
            dependsOn(stoneVariants)
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
    implementation(libs.androidx.gridlayout)
    ksp(libs.androidx.room.compiler)
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.0")
    implementation(libs.zxing.core)
    implementation(libs.zxing)
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Google's Goodies
    implementation(libs.google.android.material)
    implementation(libs.google.protobuf.javalite)

    // AndroidX
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

    // Coil
    implementation(libs.coil.kt)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // Shimmer
    implementation(libs.facebook.shimmer)

    // Epoxy
    implementation(libs.airbnb.epoxy.android)
    ksp(libs.airbnb.epoxy.processor)

    // HTTP Clients
    implementation(libs.squareup.okhttp)

    // Lib-SU
    implementation(libs.github.topjohnwu.libsu)

    // GPlayApi
    implementation(libs.auroraoss.gplayapi)

    // Shizuku
    compileOnly(libs.rikka.hidden.stub)
    implementation(libs.rikka.tools.refine.runtime)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.lsposed.hiddenapibypass)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.espresso.core)

    // Hilt
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.androidx.compiler)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.hilt.android.core)
    implementation(libs.hilt.androidx.work)

    kspAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.hilt.android.testing)

    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    implementation(libs.process.phoenix)

    // LeakCanary
    debugImplementation(libs.squareup.leakcanary.android)

    // Look
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
    implementation(libs.emv.qrcode) {
        exclude(group = "org.aspectj")
    }

    implementation("com.auth0.android:jwtdecode:2.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.moshi:moshi:1.14.0")

    "proprietaryGertecImplementation"(files("libs/EasyLayer-SK210-v2.1.7-release.aar"))

    // PagSeguro
    "pagseguroImplementation"(libs.plugpagservice.wrapper)
    "pagseguroImplementation"(libs.reactivex.java)
    "pagseguroImplementation"(libs.reactivex.android)
    "pagseguroImplementation"(libs.android.support.design)

    // Stone
    "stoneImplementation"(libs.stone.sdk)
    "stoneImplementation"(libs.stone.sdk.posandroid)

    // Stone device-specific implementations (production)
    "sunmiImplementation"(libs.stone.sdk.sunmi)
    "positivoSeriesLImplementation"(libs.stone.sdk.positivo)
    "ingenicoImplementation"(libs.stone.sdk.ingenico)
    "tectoySeriesTImplementation"(libs.stone.sdk.tectoy)
    "gertecGpos700Implementation"(libs.stone.sdk.gertec)
    "gertecGpos760Implementation"(libs.stone.sdk.gertec)

    // Stone device-specific implementations (debug - all variants for testing)
    "stoneDebugImplementation"(libs.stone.sdk.envconfig)
    "stoneDebugImplementation"(libs.stone.sdk.sunmi)
    "stoneDebugImplementation"(libs.stone.sdk.positivo)
    "stoneDebugImplementation"(libs.stone.sdk.ingenico)
    "stoneDebugImplementation"(libs.stone.sdk.gertec)
    // Note: tectoy SDK causes build failures in debug, only enabled for production tectoySeriesT flavor
    // "stoneDebugImplementation"(libs.stone.sdk.tectoy)
}