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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_BATTERY
import br.com.ticpass.pos.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_IDLE
import br.com.ticpass.pos.util.Preferences.PREFERENCES_UPDATES_RESTRICTIONS_METERED
import br.com.ticpass.pos.viewmodel.preferences.UpdatesRestrictionsViewModel
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdatesRestrictionsDialog : DialogFragment() {

    private val viewModel: UpdatesRestrictionsViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_auto_updates_restrictions, null)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_updates_restrictions_title)
            .setMessage(R.string.pref_updates_restrictions_desc)
            .setView(view)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> dialog?.dismiss()}
            .create()
    }

    override fun onResume() {
        super.onResume()
        context?.let { setupRestrictions(it) }
    }

    override fun onDestroy() {
        viewModel.updateHelper.updateAutomatedCheck()
        super.onDestroy()
    }

    private fun setupRestrictions(context: Context) {
        dialog?.findViewById<MaterialCheckBox>(R.id.checkboxMetered)?.apply {
            isChecked =
                Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_METERED, true)
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.putBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_METERED, isChecked)
            }
        }

        dialog?.findViewById<MaterialCheckBox>(R.id.checkboxIdle)?.apply {
            isChecked = Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_IDLE, true)
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.putBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_IDLE, isChecked)
            }
        }

        dialog?.findViewById<MaterialCheckBox>(R.id.checkboxBattery)?.apply {
            isChecked =
                Preferences.getBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_BATTERY, true)
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.putBoolean(context, PREFERENCES_UPDATES_RESTRICTIONS_BATTERY, isChecked)
            }
        }
    }
}
