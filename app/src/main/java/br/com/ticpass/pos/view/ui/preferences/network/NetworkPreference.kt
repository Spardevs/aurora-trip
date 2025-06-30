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

package br.com.ticpass.pos.view.ui.preferences.network

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import br.com.ticpass.extensions.runOnUiThread
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.PackageUtil
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.Preferences.PREFERENCE_MICROG_AUTH
import br.com.ticpass.pos.util.Preferences.PREFERENCE_PROXY_URL
import br.com.ticpass.pos.util.Preferences.PREFERENCE_VENDING_VERSION
import br.com.ticpass.pos.util.save
import br.com.ticpass.pos.view.ui.preferences.BasePreferenceFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkPreference : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_network, rootKey)

        findPreference<Preference>(Preferences.PREFERENCE_DISPENSER_URLS)?.apply {
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.dispenserFragment)
                true
            }
        }

        findPreference<Preference>(PREFERENCE_PROXY_URL)?.setOnPreferenceClickListener { _ ->
            findNavController().navigate(R.id.proxyURLDialog)
            false
        }

        findPreference<Preference>(PREFERENCE_VENDING_VERSION)?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                save(PREFERENCE_VENDING_VERSION, Integer.parseInt(newValue.toString()))
                runOnUiThread {
                    requireContext().toast(R.string.insecure_anonymous_apply)
                }
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_MICROG_AUTH)?.isEnabled =
            PackageUtil.hasSupportedMicroG(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.pref_network_title)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }
}
