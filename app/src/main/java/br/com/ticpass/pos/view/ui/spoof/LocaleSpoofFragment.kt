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

package br.com.ticpass.pos.view.ui.spoof

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.providers.AccountProvider
import br.com.ticpass.pos.databinding.FragmentGenericRecyclerBinding
import br.com.ticpass.pos.view.epoxy.views.TextDividerViewModel_
import br.com.ticpass.pos.view.epoxy.views.preference.LocaleViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.spoof.SpoofViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class LocaleSpoofFragment : BaseFragment<FragmentGenericRecyclerBinding>() {

    private val viewModel: SpoofViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(): LocaleSpoofFragment {
            return LocaleSpoofFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.availableLocales.collect {
                updateController(it)
            }
        }
    }

    private fun updateController(locales: List<Locale>) {
        binding.recycler.withModels {
            setFilterDuplicates(true)

            add(
                TextDividerViewModel_()
                    .id("default_divider")
                    .title(getString(R.string.default_spoof))
            )

            add(
                LocaleViewModel_()
                    .id(viewModel.defaultLocale.language)
                    .markChecked(viewModel.isLocaleSelected(viewModel.defaultLocale))
                    .checked { _, checked ->
                        if (checked) {
                            viewModel.onLocaleSelected(viewModel.defaultLocale)
                            requestModelBuild()
                            AccountProvider.logout(requireContext())
                            findNavController().navigate(R.id.forceRestartDialog)
                        }
                    }
                    .locale(viewModel.defaultLocale)
            )

            add(
                TextDividerViewModel_()
                    .id("available_divider")
                    .title(getString(R.string.available_spoof))
            )

            locales.forEach {
                add(
                    LocaleViewModel_()
                        .id(it.language)
                        .markChecked(viewModel.spoofProvider.locale == it)
                        .checked { _, checked ->
                            if (checked) {
                                viewModel.onLocaleSelected(it)
                                requestModelBuild()
                                AccountProvider.logout(requireContext())
                                findNavController().navigate(R.id.forceRestartDialog)
                            }
                        }
                        .locale(it)
                )
            }
        }
    }
}
