/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package br.com.ticpass.store.view.ui.all

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import br.com.ticpass.Constants
import br.com.ticpass.extensions.toast
import com.aurora.gplayapi.data.models.App
import br.com.ticpass.store.AuroraApp
import br.com.ticpass.store.R
import br.com.ticpass.store.data.event.InstallerEvent
import br.com.ticpass.store.data.model.MinimalApp
import br.com.ticpass.store.databinding.FragmentGenericWithSearchBinding
import br.com.ticpass.store.view.epoxy.views.HeaderViewModel_
import br.com.ticpass.store.view.epoxy.views.app.AppListViewModel_
import br.com.ticpass.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import br.com.ticpass.store.view.ui.commons.BaseFragment
import br.com.ticpass.store.viewmodel.all.InstalledViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AppsGamesFragment : BaseFragment<FragmentGenericWithSearchBinding>() {

    private val viewModel: InstalledViewModel by viewModels()

    private val startForDocumentExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument(Constants.JSON_MIME_TYPE)) {
            if (it != null) exportInstalledApps(it) else toast(R.string.toast_fav_export_failed)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.apps.collect {
                updateController(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            AuroraApp.events.installerEvent.collect {
                when (it) {
                    is InstallerEvent.Installed,
                    is InstallerEvent.Uninstalled -> {
                        viewModel.fetchApps()
                    }

                    else -> {}
                }
            }
        }

        // Toolbar
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_import_export)

            // TODO: Add support for batch install
            menu.findItem(R.id.action_import).isEnabled = false

            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_export -> {
                        startForDocumentExport.launch(
                            "aurora_store_apps_${Calendar.getInstance().time.time}.json"
                        )
                        true
                    }

                    else -> false
                }
            }
        }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    updateController(viewModel.apps.value)
                } else {
                    val filteredPackages = viewModel.apps.value?.filter {
                        it.displayName.contains(s, true) || it.packageName.contains(s, true)
                    }
                    updateController(filteredPackages)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }
        })
    }

    private fun updateController(packages: List<App>?) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            if (packages == null) {
                for (i in 1..10) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                add(
                    HeaderViewModel_()
                        .id("header")
                        .title(getString(R.string.installed_apps_size, packages.size))
                )
                packages.forEach { app ->
                    add(
                        AppListViewModel_()
                            .id(app.packageName.hashCode())
                            .app(app)
                            .click { _ ->
                                openDetailsFragment(
                                    app.packageName,
                                    app
                                )
                            }
                            .longClick { _ ->
                                openAppMenuSheet(
                                    MinimalApp.fromApp(
                                        app
                                    )
                                )
                                false
                            }
                    )
                }
            }
        }
    }

    private fun exportInstalledApps(uri: Uri) {
        viewModel.exportApps(requireContext(), uri)
        toast(R.string.toast_fav_export_success)
    }
}
