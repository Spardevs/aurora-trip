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
package br.com.ticpass.pos.view.ui.sheets

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import br.com.ticpass.extensions.hide
import br.com.ticpass.extensions.show
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.PermissionGroupInfo
import br.com.ticpass.pos.databinding.SheetPermissionsBinding
import br.com.ticpass.pos.view.custom.layouts.PermissionGroup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionBottomSheet : BaseDialogSheet<SheetPermissionsBinding>() {

    private lateinit var packageManager: PackageManager
    private lateinit var currentPerms: List<String>

    private val args: PermissionBottomSheetArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        packageManager = requireContext().packageManager
        currentPerms = try {
            packageManager.getPackageInfo(args.app.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions!!.toList()
        } catch (_: Exception) {
            emptyList()
        }

        val permissionGroupWidgets: MutableMap<String, PermissionGroup?> = mutableMapOf()
        for (permissionName in args.app.permissions) {

            val permissionInfo = getPermissionInfo(permissionName) ?: continue
            val permissionGroupInfo = getPermissionGroupInfo(permissionInfo)
            var permissionGroup: PermissionGroup?

            if (permissionGroupWidgets.containsKey(permissionGroupInfo.name)) {
                permissionGroup = permissionGroupWidgets[permissionGroupInfo.name]
            } else {
                permissionGroup = PermissionGroup(requireContext(), permissionGroupInfo)
                permissionGroupWidgets[permissionGroupInfo.name] = permissionGroup
            }

            permissionGroup?.addPermission(permissionInfo, currentPerms)
        }

        binding.permissionsContainer.removeAllViews()

        val permissionGroupLabels: List<String> = ArrayList(permissionGroupWidgets.keys)
        permissionGroupLabels.sortedBy { it }.forEach {
            binding.permissionsContainer.addView(permissionGroupWidgets[it])
        }

        if (permissionGroupLabels.isEmpty()) {
            binding.permissionsNone.show()
        } else {
            binding.permissionsNone.hide()
        }
    }

    private fun getPermissionInfo(permissionName: String): PermissionInfo? {
        return try {
            packageManager.getPermissionInfo(permissionName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun getPermissionGroupInfo(permissionInfo: PermissionInfo): PermissionGroupInfo {
        val permissionGroup = permissionInfo.group
        val permissionGroupInfo: PermissionGroupInfo =
            if (permissionGroup == null) {
                getFakePermissionGroupInfo(permissionInfo.packageName)
            } else {
                try {
                    val platformGroup = packageManager.getPermissionGroupInfo(permissionGroup, 0)

                    PermissionGroupInfo(
                        platformGroup.name,
                        platformGroup.icon,
                        platformGroup.loadLabel(packageManager).toString()
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    getFakePermissionGroupInfo(permissionInfo.packageName)
                }
            }

        if (permissionGroupInfo.icon == 0) {
            permissionGroupInfo.icon = R.drawable.ic_permission_android
        }

        return permissionGroupInfo
    }

    private fun getFakePermissionGroupInfo(packageName: String): PermissionGroupInfo {
        return when (packageName) {
            "android" -> PermissionGroupInfo("android", R.drawable.ic_permission_android)
            "com.google.android.gsf",
            "com.android.vending" -> PermissionGroupInfo("google", R.drawable.ic_permission_google)

            else -> PermissionGroupInfo()
        }
    }
}
