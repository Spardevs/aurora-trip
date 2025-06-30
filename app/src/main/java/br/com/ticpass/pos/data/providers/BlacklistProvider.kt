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

package br.com.ticpass.pos.data.providers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import br.com.ticpass.extensions.isNAndAbove
import br.com.ticpass.pos.util.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlacklistProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext val context: Context,
) {

    private val PREFERENCE_BLACKLIST = "PREFERENCE_BLACKLIST"

    var blacklist: MutableSet<String>
        set(value) = Preferences.putString(context, PREFERENCE_BLACKLIST, json.encodeToString(value))
        get() {
            return try {
                val rawBlacklist = if (isNAndAbove) {
                    val refMethod = Context::class.java.getDeclaredMethod(
                        "getSharedPreferences",
                        File::class.java,
                        Int::class.java
                    )
                    val refSharedPreferences = refMethod.invoke(
                        context,
                        File("/product/etc/br.com.ticpass.pos/blacklist.xml"),
                        Context.MODE_PRIVATE
                    ) as SharedPreferences

                    PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(
                            PREFERENCE_BLACKLIST,
                            refSharedPreferences.getString(PREFERENCE_BLACKLIST, "")
                        )
                } else {
                    Preferences.getString(context, PREFERENCE_BLACKLIST)
                }
                if (rawBlacklist!!.isEmpty())
                    mutableSetOf()
                else
                    json.decodeFromString<MutableSet<String>>(rawBlacklist)
            } catch (e: Exception) {
                mutableSetOf()
            }
        }

    fun isBlacklisted(packageName: String): Boolean {
        return blacklist.contains(packageName)
    }


    fun blacklist(packageName: String) {
        blacklist = blacklist.apply {
            add(packageName)
        }
    }

    fun whitelist(packageName: String) {
        blacklist = blacklist.apply {
            remove(packageName)
        }
    }
}
