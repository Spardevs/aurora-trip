package br.com.ticpass.store.data.model

import br.com.ticpass.store.BuildConfig

/**
 * Class representing build types for Aurora Store
 */
enum class BuildType(val packageName: String) {
    RELEASE("br.com.ticpass.store"),
    NIGHTLY("br.com.ticpass.store.nightly"),
    DEBUG("br.com.ticpass.store.debug");

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
