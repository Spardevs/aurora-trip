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

package br.com.ticpass.pos.view.ui.about

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import br.com.ticpass.extensions.browse
import br.com.ticpass.extensions.copyToClipBoard
import br.com.ticpass.pos.BuildConfig
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.Link
import br.com.ticpass.pos.databinding.FragmentAboutBinding
import br.com.ticpass.pos.view.epoxy.views.preference.LinkViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutFragment : BaseFragment<FragmentAboutBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // About Details
        binding.imgIcon.load(R.mipmap.ic_launcher)
        binding.line2.text = view.context.getString(
            R.string.version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        binding.line3.text = getString(R.string.made_with_love, String(Character.toChars(0x2764)))

        binding.epoxyRecycler.layoutManager =
            LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        updateController()
    }

    private fun updateController() {
        val linkURLS = resources.getStringArray(R.array.link_urls)
        val linkTitles = resources.getStringArray(R.array.link_titles)
        val linkSummary = resources.getStringArray(R.array.link_subtitle)

        val linkIcons = intArrayOf(
            R.drawable.ic_menu_about,
            R.drawable.ic_help,
            R.drawable.ic_xda,
            R.drawable.ic_telegram,
            R.drawable.ic_gitlab,
            R.drawable.ic_fdroid,
            R.drawable.ic_bitcoin_btc,
            R.drawable.ic_bitcoin_bch,
            R.drawable.ic_ethereum_eth,
            R.drawable.ic_bhim,
            R.drawable.ic_paypal,
            R.drawable.ic_libera_pay,
        )

        binding.epoxyRecycler.withModels {
            for (i in linkURLS.indices) {
                val link = Link(
                    id = i,
                    title = linkTitles[i],
                    subtitle = linkSummary[i],
                    url = linkURLS[i],
                    icon = linkIcons[i]
                )
                add(
                    LinkViewModel_()
                        .id(i)
                        .link(link)
                        .click { _ ->
                            if (link.id == 0) {
                                findNavController().navigate(R.id.aboutDialog)
                            } else {
                                processUrl(link.url)
                            }
                        }
                )
            }
        }
    }

    private fun processUrl(url: String) {
        when {
            url.startsWith("http") -> context?.browse(url)
            url.startsWith("upi") -> context?.browse(url)
            else -> context?.copyToClipBoard(url)
        }
    }
}
