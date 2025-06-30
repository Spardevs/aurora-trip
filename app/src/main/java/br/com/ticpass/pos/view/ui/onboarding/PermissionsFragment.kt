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
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import br.com.ticpass.extensions.isMAndAbove
import br.com.ticpass.extensions.isOAndAbove
import br.com.ticpass.extensions.isRAndAbove
import br.com.ticpass.extensions.isSAndAbove
import br.com.ticpass.extensions.isTAndAbove
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.Permission
import br.com.ticpass.pos.data.model.PermissionType
import br.com.ticpass.pos.databinding.FragmentOnboardingPermissionsBinding
import br.com.ticpass.pos.view.epoxy.views.TextDividerViewModel_
import br.com.ticpass.pos.view.epoxy.views.preference.PermissionViewModel_
import br.com.ticpass.pos.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionsFragment : BaseFragment<FragmentOnboardingPermissionsBinding>() {

    private val args: PermissionsFragmentArgs by navArgs()

    companion object {
        fun newInstance(isOnboarding: Boolean = true): PermissionsFragment {
            return PermissionsFragment().apply {
                arguments = bundleOf("isOnboarding" to isOnboarding)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Headers are only visible if we are onboarding
        binding.title.isVisible = args.isOnboarding
        binding.toolbar.apply {
            isVisible = !args.isOnboarding
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        updateController()
    }

    private fun permissionList(): List<Permission> {
        val permissions = mutableListOf(
            Permission(
                PermissionType.INSTALL_UNKNOWN_APPS,
                getString(R.string.onboarding_permission_installer),
                if (isOAndAbove) {
                    getString(R.string.onboarding_permission_installer_desc)
                } else {
                    getString(R.string.onboarding_permission_installer_legacy_desc)
                }
            )
        )

        if (isRAndAbove) {
            permissions.add(
                Permission(
                    PermissionType.STORAGE_MANAGER,
                    getString(R.string.onboarding_permission_esm),
                    getString(R.string.onboarding_permission_esa_desc),
                    false
                )
            )
        } else {
            permissions.add(
                Permission(
                    PermissionType.EXTERNAL_STORAGE,
                    getString(R.string.onboarding_permission_esa),
                    getString(R.string.onboarding_permission_esa_desc),
                    false
                )
            )
        }

        if (isMAndAbove) {
            permissions.add(
                Permission(
                    PermissionType.DOZE_WHITELIST,
                    getString(R.string.onboarding_permission_doze),
                    getString(R.string.onboarding_permission_doze_desc),
                    true
                )
            )
        }

        if (isTAndAbove) {
            permissions.add(
                Permission(
                    PermissionType.POST_NOTIFICATIONS,
                    getString(R.string.onboarding_permission_notifications),
                    getString(R.string.onboarding_permission_notifications_desc),
                    true
                )
            )
        }

        if (isSAndAbove) {
            permissions.add(
                Permission(
                    PermissionType.APP_LINKS,
                    getString(R.string.app_links_title),
                    getString(R.string.app_links_desc),
                    optional = true
                ),
            )
        }

        return permissions
    }

    private fun updateController() {
        binding.epoxyRecycler.withModels {
            setFilterDuplicates(true)

            add(
                TextDividerViewModel_()
                    .id("required_divider")
                    .title(getString(R.string.item_required))
            )

            permissionList()
                .filterNot { it.optional }
                .forEach { add(renderPermissionView(it)) }

            val optionalPermissions = permissionList().filter { it.optional }
            if (optionalPermissions.isNotEmpty()) {
                add(
                    TextDividerViewModel_()
                        .id("optional_divider")
                        .title(getString(R.string.item_optional))
                )

                optionalPermissions.forEach { add(renderPermissionView(it)) }
            }
        }
    }

    private fun renderPermissionView(permission: Permission): PermissionViewModel_ {
        return PermissionViewModel_()
            .id(permission.type.name)
            .permission(permission)
            .isGranted(permissionProvider.isGranted(permission.type))
            .click { _ ->
                permissionProvider.request(permission.type) {
                    if (it) updateController()
                }
            }
    }
}
