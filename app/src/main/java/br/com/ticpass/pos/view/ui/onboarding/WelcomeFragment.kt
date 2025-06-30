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

package br.com.ticpass.pos.view.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import br.com.ticpass.extensions.browse
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.Dash
import br.com.ticpass.pos.databinding.FragmentOnboardingWelcomeBinding
import br.com.ticpass.pos.view.epoxy.views.preference.DashViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeFragment : BaseFragment<FragmentOnboardingWelcomeBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)
            loadDashFromAssets().forEach {
                add(
                    DashViewModel_()
                        .id(it.id)
                        .dash(it)
                        .click { _ ->
                            if (it.id == 0) {
                                findNavController().navigate(R.id.aboutDialog)
                            } else {
                                requireContext().browse(it.url)
                            }
                        }
                )
            }
        }
    }

    private fun loadDashFromAssets(): List<Dash> {
        return listOf(
            Dash(
                id = 0,
                title = requireContext().getString(R.string.title_about),
                subtitle = requireContext().getString(R.string.about_aurora_store_subtitle),
                icon = R.drawable.ic_menu_about,
                url = "https://auroraoss.com/"
            ),
            Dash(
                id = 1,
                title = requireContext().getString(R.string.faqs_title),
                subtitle = requireContext().getString(R.string.faqs_subtitle),
                icon = R.drawable.ic_faq,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/-/wikis/Frequently%20Asked%20Questions"
            ),
            Dash(
                id = 2,
                title = requireContext().getString(R.string.source_code_title),
                subtitle = requireContext().getString(R.string.source_code_subtitle),
                icon = R.drawable.ic_code,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/"
            ),
            Dash(
                id = 3,
                title = requireContext().getString(R.string.menu_license),
                subtitle = requireContext().getString(R.string.license_subtitle),
                icon = R.drawable.ic_license,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/LICENSE"
            ),
            Dash(
                id = 4,
                title = requireContext().getString(R.string.privacy_policy_title),
                subtitle = requireContext().getString(R.string.privacy_policy_subtitle),
                icon = R.drawable.ic_privacy,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/POLICY.md"
            ),
            Dash(
                id = 5,
                title = requireContext().getString(R.string.menu_disclaimer),
                subtitle = requireContext().getString(R.string.disclaimer_subtitle),
                icon = R.drawable.ic_disclaimer,
                url = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/DISCLAIMER.md"
            )
        )
    }
}
