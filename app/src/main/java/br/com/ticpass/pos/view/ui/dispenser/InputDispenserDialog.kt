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

package br.com.ticpass.pos.view.ui.dispenser

import android.app.Dialog
import android.os.Bundle
import android.util.Patterns
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import br.com.ticpass.extensions.showKeyboard
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.Preferences.PREFERENCE_DISPENSER_URLS
import br.com.ticpass.pos.util.save
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputDispenserDialog: DialogFragment() {

    private val textInputLayout: TextInputLayout?
        get() = dialog?.findViewById(R.id.textInputLayout)

    private val dispensers: Set<String>
        get() = Preferences.getStringSet(requireContext(), PREFERENCE_DISPENSER_URLS)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_text_input_edit_text, null)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_dispenser_title)
            .setMessage(R.string.add_dispenser_summary)
            .setView(view)
            .setPositiveButton(getString(R.string.add)) { _, _ -> saveDispenserUrl() }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> dialog?.dismiss()}
            .create()
    }

    override fun onResume() {
        super.onResume()
        textInputLayout?.editText?.apply {
            hint = requireContext().getString(R.string.add_dispenser_hint)
            showKeyboard()
        }
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun saveDispenserUrl() {
        val url = textInputLayout?.editText?.text?.toString()
        if (!url.isNullOrEmpty() && Patterns.WEB_URL.matcher(url).matches()) {
            val newSet = dispensers.toMutableSet().apply {
                add(url)
            }
            save(PREFERENCE_DISPENSER_URLS, newSet)
        } else {
            toast(R.string.add_dispenser_error)
        }
    }
}
