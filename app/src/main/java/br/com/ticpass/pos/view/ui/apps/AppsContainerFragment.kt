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

package br.com.ticpass.pos.view.ui.apps

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import br.com.ticpass.pos.MobileNavigationDirections
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.FragmentAppsGamesBinding
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import br.com.ticpass.pos.view.ui.commons.CategoryFragment
import br.com.ticpass.pos.view.ui.commons.ForYouFragment
import br.com.ticpass.pos.view.ui.commons.TopChartContainerFragment
import br.com.ticpass.pos.viewmodel.apps.AppsContainerViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppsContainerFragment : BaseFragment<FragmentAppsGamesBinding>() {

    private val viewModel: AppsContainerViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust FAB margins for edgeToEdge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.searchFab) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.searchFab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + resources.getDimensionPixelSize(R.dimen.margin_large)
            }
            WindowInsetsCompat.CONSUMED
        }

        // Toolbar
        binding.toolbar.apply {
            title = getString(R.string.title_apps)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_download_manager -> {
                        findNavController().navigate(R.id.downloadFragment)
                    }

                    R.id.menu_more -> {
                        findNavController().navigate(
                            MobileNavigationDirections.actionGlobalMoreDialogFragment()
                        )
                    }
                }
                true
            }
        }

        // ViewPager
        val isForYouEnabled = Preferences.getBoolean(
            requireContext(),
            Preferences.PREFERENCE_FOR_YOU
        )

        binding.pager.adapter = ViewPagerAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
            !viewModel.authProvider.isAnonymous,
            isForYouEnabled
        )

        binding.pager.isUserInputEnabled =
            false //Disable viewpager scroll to avoid scroll conflicts

        val tabTitles: MutableList<String> = mutableListOf<String>().apply {
            if (isForYouEnabled) {
                add(getString(R.string.tab_for_you))
            }

            add(getString(R.string.tab_top_charts))
            add(getString(R.string.tab_categories))
        }

        TabLayoutMediator(
            binding.tabLayout,
            binding.pager,
            true
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = tabTitles[position]
        }.attach()

        binding.searchFab.setOnClickListener {
            findNavController().navigate(R.id.searchSuggestionFragment)
        }
    }

    override fun onDestroyView() {
        binding.pager.adapter = null
        super.onDestroyView()
    }

    internal class ViewPagerAdapter(
        fragment: FragmentManager,
        lifecycle: Lifecycle,
        private val isGoogleAccount: Boolean,
        private val isForYouEnabled: Boolean
    ) :
        FragmentStateAdapter(fragment, lifecycle) {

        private val tabFragments: MutableList<Fragment> = mutableListOf<Fragment>().apply {
            if (isForYouEnabled) {
                add(ForYouFragment.newInstance(0))
            }
            add(TopChartContainerFragment.newInstance(0))
            add(CategoryFragment.newInstance(0))
        }

        override fun createFragment(position: Int): Fragment {
            return tabFragments[position]
        }

        override fun getItemCount(): Int {
            return tabFragments.size
        }
    }
}
