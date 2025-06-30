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

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import br.com.ticpass.extensions.isTAndAbove
import br.com.ticpass.pos.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class UIPreference : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_ui, rootKey)

        findPreference<Preference>("PREFERENCE_APP_LANGUAGE")?.apply {
            if (isTAndAbove) {
                summary = Locale.getDefault().displayName
                setOnPreferenceClickListener {
                    startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                        data = ("package:" + requireContext().packageName).toUri()
                    })
                    true
                }
            } else {
                isVisible = false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.pref_ui_title)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }
}
