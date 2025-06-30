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

package br.com.ticpass.pos.view.ui.preferences

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import br.com.ticpass.pos.data.providers.PermissionProvider
import br.com.ticpass.pos.view.custom.preference.M3EditTextPreference
import br.com.ticpass.pos.view.custom.preference.M3ListPreference

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    lateinit var permissionProvider: PermissionProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionProvider = PermissionProvider(this)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is EditTextPreference -> {
                val dialogFragment = M3EditTextPreference.newInstance(preference.getKey())
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(
                    parentFragmentManager,
                    M3EditTextPreference.PREFERENCE_DIALOG_FRAGMENT_TAG
                )
            }
            is ListPreference -> {
                val dialogFragment = M3ListPreference.newInstance(preference.getKey())
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(
                    parentFragmentManager,
                    M3ListPreference.PREFERENCE_DIALOG_FRAGMENT_TAG
                )
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onDestroy() {
        permissionProvider.unregister()
        super.onDestroy()
    }
}
