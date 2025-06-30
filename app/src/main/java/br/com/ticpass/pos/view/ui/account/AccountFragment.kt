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

package br.com.ticpass.pos.view.ui.account

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import br.com.ticpass.Constants.URL_DISCLAIMER
import br.com.ticpass.Constants.URL_LICENSE
import br.com.ticpass.Constants.URL_TOS
import br.com.ticpass.extensions.browse
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.FragmentAccountBinding
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.viewmodel.account.AccountViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    private val viewModel: AccountViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Chips
        view.context.apply {
            binding.chipDisclaimer.setOnClickListener { browse(URL_DISCLAIMER) }
            binding.chipLicense.setOnClickListener { browse(URL_LICENSE) }
            binding.chipTos.setOnClickListener { browse(URL_TOS) }
        }

        viewModel.authProvider.authData?.userProfile?.let {
            val avatar =
                if (viewModel.authProvider.isAnonymous) R.mipmap.ic_launcher else it.artwork.url
            binding.imgAvatar.load(avatar) {
                placeholder(R.drawable.bg_placeholder)
                transformations(RoundedCornersTransformation(32F))
            }
            binding.txtName.text = if (viewModel.authProvider.isAnonymous) "Anonymous" else it.name
            binding.txtEmail.text =
                if (viewModel.authProvider.isAnonymous) "anonymous@gmail.com" else it.email
        }

        binding.btnLogout.addOnClickListener {
            findNavController().navigate(R.id.logoutDialog)
        }
    }
}
