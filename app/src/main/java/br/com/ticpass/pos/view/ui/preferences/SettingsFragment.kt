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
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import br.com.ticpass.pos.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey)

        findPreference<Preference>("pref_perms")?.setOnPreferenceClickListener {
            findNavController().navigate(
                SettingsFragmentDirections.actionSettingsFragmentToPermissionsFragment(false)
            )
            true
        }
        findPreference<Preference>("pref_install")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.installationPreference)
            true
        }
        findPreference<Preference>("pref_ui")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.UIPreference)
            true
        }
        findPreference<Preference>("pref_network")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.networkPreference)
            true
        }
        findPreference<Preference>("pref_updates")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.updatesPreference)
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_settings)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }
}
