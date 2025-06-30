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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.airbnb.epoxy.EpoxyRecyclerView
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.data.models.StreamCluster
import br.com.ticpass.pos.MobileNavigationDirections
import br.com.ticpass.pos.data.model.MinimalApp
import br.com.ticpass.pos.data.providers.PermissionProvider
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<ViewBindingType : ViewBinding> : Fragment() {

    private val TAG = BaseFragment::class.java.simpleName

    lateinit var permissionProvider: PermissionProvider

    protected open var _binding: ViewBindingType? = null
    protected val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionProvider = PermissionProvider(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val type =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<ViewBindingType>
        val method = type.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        _binding = method.invoke(null, inflater, container, false) as ViewBindingType

        return binding.root
    }

    override fun onDestroy() {
        permissionProvider.unregister()
        super.onDestroy()
    }

    override fun onDestroyView() {
        cleanupRecyclerViews(findAllRecyclerViews(requireView()))
        _binding = null
        super.onDestroyView()
    }

    fun openDetailsFragment(packageName: String, app: App? = null) {
        findNavController().navigate(
            MobileNavigationDirections.actionGlobalAppDetailsFragment(packageName, app)
        )
    }

    fun openCategoryBrowseFragment(category: Category) {
        findNavController().navigate(
            MobileNavigationDirections.actionGlobalCategoryBrowseFragment(
                category.title,
                category.browseUrl
            )
        )
    }

    fun openStreamBrowseFragment(browseUrl: String, title: String = "") {
        if (browseUrl.lowercase().contains("expanded")) {
            findNavController().navigate(
                MobileNavigationDirections.actionGlobalExpandedStreamBrowseFragment(
                    title,
                    browseUrl
                )
            )
        } else if (browseUrl.lowercase().contains("developer")) {
            findNavController().navigate(
                MobileNavigationDirections.actionGlobalDevProfileFragment(
                    browseUrl.substringAfter("developer-"),
                    title
                )
            )
        }
    }

    fun openStreamBrowseFragment(streamCluster: StreamCluster) {
        findNavController().navigate(
            MobileNavigationDirections.actionGlobalStreamBrowseFragment(streamCluster)
        )
    }

    fun openScreenshotFragment(app: App, position: Int) {
        findNavController().navigate(
            MobileNavigationDirections.actionGlobalScreenshotFragment(
                position,
                app.screenshots.toTypedArray()
            )
        )
    }

    fun openAppMenuSheet(app: MinimalApp) {
        findNavController().navigate(MobileNavigationDirections.actionGlobalAppMenuSheet(app))
    }

    private fun cleanupRecyclerViews(recyclerViews: List<EpoxyRecyclerView>) {
        recyclerViews.forEach { recyclerView ->
            runCatching {
                recyclerView.adapter?.let {
                    recyclerView.swapAdapter(it, true)
                }
            }.onFailure {
                Log.e(TAG, "Failed to cleanup RecyclerView", it)
            }
        }
    }

    private fun findAllRecyclerViews(view: View): List<EpoxyRecyclerView> {
        val recyclerViews = mutableListOf<EpoxyRecyclerView>()

        if (view is EpoxyRecyclerView) {
            recyclerViews.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                recyclerViews.addAll(findAllRecyclerViews(view.getChildAt(i)))
            }
        }

        return recyclerViews
    }
}
