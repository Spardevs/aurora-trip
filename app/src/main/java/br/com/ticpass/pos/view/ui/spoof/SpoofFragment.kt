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

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import br.com.ticpass.extensions.toast
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.providers.NativeDeviceInfoProvider
import br.com.ticpass.pos.databinding.FragmentSpoofBinding
import br.com.ticpass.pos.util.PathUtil
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpoofFragment : BaseFragment<FragmentSpoofBinding>() {
    private val TAG = SpoofFragment::class.java.simpleName

    // Android is weird, even if export device config with proper mime type, it will refuse to open
    // it again with same mime type
    private val importMimeType = "application/octet-stream"
    private val exportMimeType = "text/x-java-properties"

    private val startForDocumentImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) importDeviceConfig(it) else toast(R.string.toast_import_failed)
        }
    private val startForDocumentExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument(exportMimeType)) {
            if (it != null) exportDeviceConfig(it) else toast(R.string.toast_export_failed)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.apply {
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_import -> {
                        startForDocumentImport.launch(arrayOf(importMimeType))
                    }

                    R.id.action_export -> {
                        startForDocumentExport
                            .launch("aurora_store_${Build.BRAND}_${Build.DEVICE}.properties")
                    }
                }
                true
            }
        }

        // ViewPager
        binding.pager.adapter = ViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        TabLayoutMediator(
            binding.tabLayout,
            binding.pager,
            true
        ) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                0 -> tab.text = getString(R.string.title_device)
                1 -> tab.text = getString(R.string.title_language)
                else -> {
                }
            }
        }.attach()
    }

    override fun onDestroyView() {
        binding.pager.adapter = null
        super.onDestroyView()
    }

    private fun importDeviceConfig(uri: Uri) {
        try {
            requireContext().contentResolver?.openInputStream(uri)?.use { input ->
                PathUtil.getNewEmptySpoofConfig(requireContext()).outputStream().use {
                    input.copyTo(it)
                }
            }
            toast(R.string.toast_import_success)
            activity?.recreate()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to import device config", exception)
            toast(R.string.toast_import_failed)
        }
    }

    private fun exportDeviceConfig(uri: Uri) {
        try {
            NativeDeviceInfoProvider.getNativeDeviceProperties(requireContext(), true)
                .store(requireContext().contentResolver?.openOutputStream(uri), "DEVICE_CONFIG")
            toast(R.string.toast_export_success)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to export device config", exception)
            toast(R.string.toast_export_failed)
        }
    }

    internal class ViewPagerAdapter(fragment: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragment, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DeviceSpoofFragment.newInstance()
                1 -> LocaleSpoofFragment.newInstance()
                else -> Fragment()
            }
        }

        override fun getItemCount(): Int {
            return 2
        }
    }
}
