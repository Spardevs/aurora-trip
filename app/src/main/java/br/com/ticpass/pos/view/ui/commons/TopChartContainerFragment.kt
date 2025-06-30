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

package br.com.ticpass.pos.view.ui.commons

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import br.com.ticpass.Constants
import br.com.ticpass.pos.R
import br.com.ticpass.pos.databinding.FragmentTopChartBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopChartContainerFragment : BaseFragment<FragmentTopChartBinding>() {
    companion object {
        @JvmStatic
        fun newInstance(chartType: Int): TopChartContainerFragment {
            return TopChartContainerFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.TOP_CHART_TYPE, chartType)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var chartType = 0
        val bundle = arguments
        if (bundle != null) {
            chartType = bundle.getInt(Constants.TOP_CHART_TYPE, 0)
        }

        // ViewPager
        binding.pager.adapter =
            ViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, chartType)
        binding.topTabGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds[0]) {
                R.id.tab_top_free -> binding.pager.setCurrentItem(0, true)
                R.id.tab_top_grossing -> binding.pager.setCurrentItem(1, true)
                R.id.tab_trending -> binding.pager.setCurrentItem(2, true)
                R.id.tab_top_paid -> binding.pager.setCurrentItem(3, true)
            }
        }

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> binding.topTabGroup.check(R.id.tab_top_free)
                    1 -> binding.topTabGroup.check(R.id.tab_top_grossing)
                    2 -> binding.topTabGroup.check(R.id.tab_trending)
                    3 -> binding.topTabGroup.check(R.id.tab_top_paid)
                }
            }
        })
    }

    override fun onDestroyView() {
        binding.pager.adapter = null
        super.onDestroyView()
    }

    internal class ViewPagerAdapter(
        fragment: FragmentManager,
        lifecycle: Lifecycle,
        chartType: Int
    ) :
        FragmentStateAdapter(fragment, lifecycle) {
        private val tabFragments: MutableList<TopChartFragment> = mutableListOf(
            TopChartFragment.newInstance(chartType, 0),
            TopChartFragment.newInstance(chartType, 1),
            TopChartFragment.newInstance(chartType, 2),
            TopChartFragment.newInstance(chartType, 3)
        )

        override fun createFragment(position: Int): Fragment {
            return tabFragments[position]
        }

        override fun getItemCount(): Int {
            return tabFragments.size
        }
    }
}
