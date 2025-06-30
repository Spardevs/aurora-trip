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

package br.com.ticpass.pos.view.ui.preferences.updates

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.PermissionType
import br.com.ticpass.pos.data.model.UpdateMode
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS
import br.com.ticpass.pos.util.Preferences.PREFERENCE_FILTER_AURORA_ONLY
import br.com.ticpass.pos.util.Preferences.PREFERENCE_FILTER_FDROID
import br.com.ticpass.pos.util.Preferences.PREFERENCE_UPDATES_AUTO
import br.com.ticpass.pos.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import br.com.ticpass.pos.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import br.com.ticpass.pos.util.save
import br.com.ticpass.pos.view.ui.preferences.BasePreferenceFragment
import br.com.ticpass.pos.viewmodel.all.UpdatesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdatesPreference : BasePreferenceFragment() {

    private val viewModel: UpdatesViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_updates, rootKey)

        val updatesEnabled = Preferences.getInteger(requireContext(), PREFERENCE_UPDATES_AUTO) != 0

        findPreference<ListPreference>(PREFERENCE_UPDATES_AUTO)?.setOnPreferenceChangeListener { _, newValue ->
            when (UpdateMode.entries[newValue.toString().toInt()]) {
                UpdateMode.DISABLED -> {
                    handleAutoUpdateDependentPrefs(false)
                    viewModel.updateHelper.cancelAutomatedCheck()
                    requireContext().save(PREFERENCE_UPDATES_AUTO, 0)
                    true
                }

                UpdateMode.CHECK_AND_NOTIFY -> {
                    if (permissionProvider.isGranted(PermissionType.POST_NOTIFICATIONS)) {
                        handleAutoUpdateDependentPrefs(true)
                        viewModel.updateHelper.scheduleAutomatedCheck()
                        true
                    } else {
                        permissionProvider.request(PermissionType.POST_NOTIFICATIONS) {
                            if (it) {
                                handleAutoUpdateDependentPrefs(true)
                                requireContext().save(PREFERENCE_UPDATES_AUTO, 1)
                                viewModel.updateHelper.scheduleAutomatedCheck()
                                activity?.recreate()
                            }
                        }
                        false
                    }
                }

                UpdateMode.CHECK_AND_INSTALL -> {
                    if (permissionProvider.isGranted(PermissionType.DOZE_WHITELIST)) {
                        handleAutoUpdateDependentPrefs(true)
                        viewModel.updateHelper.scheduleAutomatedCheck()
                        true
                    } else {
                        permissionProvider.request(PermissionType.DOZE_WHITELIST) {
                            if (it) {
                                handleAutoUpdateDependentPrefs(true)
                                requireContext().save(PREFERENCE_UPDATES_AUTO, 2)
                                viewModel.updateHelper.scheduleAutomatedCheck()
                                activity?.recreate()
                            }
                        }
                        false
                    }
                }

                else -> false
            }
        }

        handleAutoUpdateDependentPrefs(updatesEnabled)

        findPreference<SwitchPreferenceCompat>(PREFERENCE_FILTER_AURORA_ONLY)
            ?.setOnPreferenceChangeListener { _, newValue ->
                findPreference<SwitchPreferenceCompat>(PREFERENCE_FILTER_FDROID)?.isEnabled =
                    !newValue.toString().toBoolean()
                viewModel.updateHelper.checkUpdatesNow()
                true
            }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_FILTER_FDROID)?.apply {
            isEnabled = !Preferences.getBoolean(requireContext(), PREFERENCE_FILTER_AURORA_ONLY)
            setOnPreferenceChangeListener { _, _ ->
                viewModel.updateHelper.checkUpdatesNow()
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_UPDATES_EXTENDED)
            ?.setOnPreferenceChangeListener { _, _ ->
                viewModel.updateHelper.checkUpdatesNow()
                true
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_updates)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }

    private fun handleAutoUpdateDependentPrefs(enabled: Boolean) {
        findPreference<Preference>(PREFERENCES_UPDATES_RESTRICTIONS)?.apply {
            isEnabled = enabled
            isVisible = enabled
            setOnPreferenceClickListener {
                findNavController().navigate(R.id.updatesRestrictionsDialog)
                true
            }
        }

        findPreference<SeekBarPreference>(PREFERENCE_UPDATES_CHECK_INTERVAL)?.apply {
            isEnabled = enabled
            isVisible = enabled
            setOnPreferenceChangeListener { _, _ ->
                viewModel.updateHelper.updateAutomatedCheck()
                true
            }
        }
    }
}
