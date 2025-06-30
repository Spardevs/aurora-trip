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
package br.com.ticpass.pos.view.custom.layouts

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import br.com.ticpass.extensions.showDialog
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.model.PermissionGroupInfo
import java.util.Locale

class PermissionGroup @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var permissionGroupInfo: PermissionGroupInfo
    private lateinit var packageManager: PackageManager

    private val permissionMap: MutableMap<String, String> = HashMap()

    constructor(context: Context?, permissionGroupInfo: PermissionGroupInfo) : this(context) {
        if (context != null) {
            inflate(context, R.layout.layout_permission, this)

            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )

            this.packageManager = context.packageManager
            this.permissionGroupInfo = permissionGroupInfo

            val imageView = findViewById<ImageView>(R.id.img)
            imageView.setImageDrawable(getPermissionGroupIcon(permissionGroupInfo))
        }
    }

    fun addPermission(permissionInfo: PermissionInfo, currentPerms: List<String> = emptyList()) {
        val title = permissionInfo.loadLabel(packageManager)
        val description = permissionInfo.loadDescription(packageManager)

        permissionMap[getReadableLabel(title.toString(), permissionInfo.packageName)] =
            if (description.isNullOrEmpty())
                "No description"
            else
                description.toString()

        val permissionLabels: List<String> = ArrayList(permissionMap.keys)
        val permissionLabelsView = findViewById<LinearLayout>(R.id.permission_labels)
        permissionLabelsView.removeAllViews()

        permissionLabels
            .filter { it.isNotEmpty() }
            .sortedBy { it }
            .forEach {
                addPermissionLabel(
                    permissionLabelsView,
                    it,
                    permissionMap[it],
                    if (currentPerms.isNotEmpty()) permissionInfo.name !in currentPerms else false
                )
            }
    }

    private fun addPermissionLabel(
        permissionLabelsView: LinearLayout,
        label: String,
        description: String?,
        isNewPerm: Boolean = false
    ) {
        val textView = TextView(context)
        textView.text = label
        if (isNewPerm) textView.setTextColor(ContextCompat.getColor(context, R.color.colorGreen))
        textView.setOnClickListener {
            var title: String = permissionGroupInfo.label

            if (title.contains("UNDEFINED")) {
                title = "Android"
            }

            title = title.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(Locale.getDefault())
                } else {
                    it.toString()
                }
            }

            context.showDialog(title, description)
        }

        permissionLabelsView.addView(textView)
    }

    private fun getPermissionGroupIcon(permissionGroupInfo: PermissionGroupInfo): Drawable? {
        return ContextCompat.getDrawable(context, permissionGroupInfo.icon)
    }

    private fun getReadableLabel(label: String, packageName: String): String {
        val prefixes: MutableList<String> = mutableListOf(
            "android",
            packageName
        )

        if (label.contains("UNDEFINED")) {
            return "Android"
        }

        prefixes
            .map { "$it.permission." }
            .forEach {
                if (label.startsWith(it)) {
                    return it.replace(it, "")
                        .replace("_", " ")
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar {
                            if (it.isLowerCase()) {
                                it.titlecase(Locale.getDefault())
                            } else {
                                it.toString()
                            }
                        }
                }
            }

        return label.replaceFirstChar {
            it.titlecase(Locale.getDefault())
        }
    }
}
