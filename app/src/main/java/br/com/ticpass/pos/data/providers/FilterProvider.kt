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
import br.com.ticpass.pos.data.model.Filter
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.remove
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext private val context: Context
) {

    companion object {
        const val PREFERENCE_FILTER = "PREFERENCE_FILTER"
    }

    init {
        // Clean any last saved filter
        context.remove(PREFERENCE_FILTER)
    }

    fun getSavedFilter(): Filter {
        val rawFilter = Preferences.getString(context, PREFERENCE_FILTER)
        return json.decodeFromString<Filter>(rawFilter.ifEmpty { "{}" })
    }

    fun saveFilter(filter: Filter) {
        Preferences.putString(context, PREFERENCE_FILTER, json.encodeToString(filter))
    }
}
