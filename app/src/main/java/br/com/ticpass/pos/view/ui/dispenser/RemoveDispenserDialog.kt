package br.com.ticpass.pos.view.ui.dispenser

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.Preferences.PREFERENCE_DISPENSER_URLS
import br.com.ticpass.pos.util.save
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemoveDispenserDialog : DialogFragment() {

    private val args: RemoveDispenserDialogArgs by navArgs()

    private val dispensers: Set<String>
        get() = Preferences.getStringSet(requireContext(), PREFERENCE_DISPENSER_URLS)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.remove_dispenser_title)
            .setMessage(getString(R.string.remove_dispenser_summary, args.url))
            .setPositiveButton(getString(R.string.remove)) { _, _ -> removeDispenserUrl() }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ -> dialog?.dismiss() }
            .create()
    }

    private fun removeDispenserUrl() {
        val newSet = dispensers.toMutableSet().apply {
            remove(args.url)
        }
        save(PREFERENCE_DISPENSER_URLS, newSet)
    }
}
