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
val localProperties = java.util.Properties().apply {
    val propertiesFile = File(settings.rootDir, "local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

val packageCloudReadToken = localProperties.getProperty("PACKAGE_CLOUD_READ_TOKEN")
    ?: error("Required property PACKAGE_CLOUD_READ_TOKEN not found")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        // libsu is only available via jitpack
        maven("https://jitpack.io/") {
            content {
                includeModule("com.github.topjohnwu.libsu", "core")
            }
        }

        maven(url = "https://github.com/pagseguro/PlugPagServiceWrapper/raw/master")
        maven(url = "https://packagecloud.io/priv" +
                "/${packageCloudReadToken}/stone/pos-android/maven2")
    }
}
include(":app")
rootProject.name = "Ticpass"
