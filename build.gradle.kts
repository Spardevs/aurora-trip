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

buildscript {
    repositories {
        maven(url = "https://github.com/pagseguro/PlugPagServiceWrapper/raw/master")
        maven(url = "https://packagecloud.io/priv" +
                "/${AppConfig.packageCloudReadToken}/stone/pos-android/maven2")
    }

    dependencies {
        classpath(libs.plugpagservice.wrapper)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.parcelize) apply false
    alias(libs.plugins.jetbrains.kotlin.serialization) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.androidx.navigation) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.rikka.tools.refine.plugin) apply false
    alias(libs.plugins.hilt.android.plugin) apply false
}
