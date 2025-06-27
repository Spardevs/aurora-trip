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
