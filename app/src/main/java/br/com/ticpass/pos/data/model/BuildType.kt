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

package br.com.ticpass.pos.data.model

import br.com.ticpass.pos.BuildConfig

/**
 * Class representing build types for Aurora Store
 */
enum class BuildType(val packageName: String) {
    RELEASE("br.com.ticpass.pos"),
    NIGHTLY("br.com.ticpass.pos.nightly"),
    DEBUG("br.com.ticpass.pos.debug");

    companion object {

        /**
         * Returns current build type
         */
        @Suppress("KotlinConstantConditions")
        val CURRENT: BuildType
            get() = when (BuildConfig.BUILD_TYPE) {
                "release" -> RELEASE
                "nightly" -> NIGHTLY
                else -> DEBUG
            }

        /**
         * Returns package names for all possible build types
         */
        val PACKAGE_NAMES: List<String>
            get() = BuildType.entries.map { it.packageName }
    }
}
