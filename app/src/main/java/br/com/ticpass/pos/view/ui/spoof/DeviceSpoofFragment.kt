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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.providers.AccountProvider
import br.com.ticpass.pos.databinding.FragmentGenericRecyclerBinding
import br.com.ticpass.pos.view.epoxy.views.TextDividerViewModel_
import br.com.ticpass.pos.view.epoxy.views.preference.DeviceViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.spoof.SpoofViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Properties

@AndroidEntryPoint
class DeviceSpoofFragment : BaseFragment<FragmentGenericRecyclerBinding>() {

    private val viewModel: SpoofViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(): DeviceSpoofFragment {
            return DeviceSpoofFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.availableDevices.collect { updateController(it) }
            }
        }
    }

    private fun updateController(devices: List<Properties>) {
        binding.recycler.withModels {
            setFilterDuplicates(true)

            add(
                TextDividerViewModel_()
                    .id("default_divider")
                    .title(getString(R.string.default_spoof))
            )

            add(
                DeviceViewModel_()
                    .id(viewModel.defaultProperties.hashCode())
                    .markChecked(viewModel.isDeviceSelected(viewModel.defaultProperties))
                    .checked { _, checked ->
                        if (checked) {
                            viewModel.onDeviceSelected(viewModel.defaultProperties)
                            requestModelBuild()
                            AccountProvider.logout(requireContext())
                            findNavController().navigate(R.id.forceRestartDialog)
                        }
                    }
                    .properties(viewModel.defaultProperties)
            )

            add(
                TextDividerViewModel_()
                    .id("available_divider")
                    .title(getString(R.string.available_spoof))
            )

            devices.forEach {
                add(
                    DeviceViewModel_()
                        .id(it.hashCode())
                        .markChecked(viewModel.isDeviceSelected(it))
                        .checked { _, checked ->
                            if (checked) {
                                viewModel.onDeviceSelected(it)
                                requestModelBuild()
                                AccountProvider.logout(requireContext())
                                findNavController().navigate(R.id.forceRestartDialog)
                            }
                        }
                        .properties(it)
                )
            }
        }
    }
}
