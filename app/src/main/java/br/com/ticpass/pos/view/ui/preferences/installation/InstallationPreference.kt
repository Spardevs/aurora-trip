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

package br.com.ticpass.pos.view.ui.preferences.installation

import android.app.admin.DevicePolicyManager
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.getSystemService
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import br.com.ticpass.extensions.showDialog
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.Preferences.PREFERENCE_INSTALLATION_DEVICE_OWNER
import br.com.ticpass.pos.util.Preferences.PREFERENCE_INSTALLER_ID
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstallationPreference : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_installation, rootKey)

        findPreference<Preference>(PREFERENCE_INSTALLER_ID)?.apply {
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.installerFragment)
                true
            }
        }

        findPreference<Preference>(PREFERENCE_INSTALLATION_DEVICE_OWNER)?.apply {
            val packageName = context.packageName
            val devicePolicyManager = context.getSystemService<DevicePolicyManager>()

            isVisible = devicePolicyManager?.isDeviceOwnerApp(packageName) ?: false
            setOnPreferenceClickListener {
                context.showDialog(
                    context.getString(R.string.pref_clear_device_owner_title),
                    context.getString(R.string.pref_clear_device_owner_desc),
                    { _: DialogInterface, _: Int ->
                        @Suppress("DEPRECATION")
                        devicePolicyManager!!.clearDeviceOwnerApp(packageName)
                        activity?.recreate()
                    },
                    { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                )
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_installation)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }
}
