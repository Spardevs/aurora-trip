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

package br.com.ticpass.pos.view.epoxy.controller

import com.airbnb.epoxy.TypedEpoxyController
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.epoxy.groups.CarouselModelGroup
import br.com.ticpass.pos.view.epoxy.groups.CarouselShimmerGroup
import br.com.ticpass.pos.view.epoxy.views.app.AppListViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.NoAppViewModel_

open class GenericCarouselController(private val callbacks: Callbacks) :

    TypedEpoxyController<StreamBundle?>() {

    interface Callbacks {
        fun onHeaderClicked(streamCluster: StreamCluster)
        fun onClusterScrolled(streamCluster: StreamCluster)
        fun onAppClick(app: App)
        fun onAppLongClick(app: App)
    }

    open fun applyFilter(streamBundle: StreamCluster): Boolean {
        return streamBundle.clusterTitle.isNotBlank()  //Filter noisy cluster
                && streamBundle.clusterAppList.isNotEmpty() //Filter empty clusters
                && streamBundle.clusterAppList.count() > 1 //Filter clusters with single apps (mostly promotions)
    }

    override fun buildModels(streamBundle: StreamBundle?) {
        setFilterDuplicates(true)
        if (streamBundle == null) {
            for (i in 1..4) {
                add(
                    CarouselShimmerGroup()
                        .id(i)
                )
            }
        } else {
            if (streamBundle.streamClusters.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_app")
                        .icon(R.drawable.ic_apps)
                        .message(R.string.no_apps_available)
                )
            } else {
                if (streamBundle.streamClusters.size == 1) {
                    streamBundle
                        .streamClusters
                        .values
                        .filter { applyFilter(it) }
                        .forEach { streamCluster ->
                            streamCluster.clusterAppList.forEach {
                                add(
                                    AppListViewModel_()
                                        .id(it.id)
                                        .app(it)
                                        .click { _ -> callbacks.onAppClick(it) }
                                )
                            }
                        }

                } else {
                    streamBundle
                        .streamClusters
                        .values
                        .filter { applyFilter(it) }
                        .forEach { streamCluster ->
                            add(CarouselModelGroup(streamCluster, callbacks))
                        }

                }
                if (streamBundle.hasNext())
                    add(
                        CarouselShimmerGroup()
                            .id("progress")
                    )
            }
        }
    }
}
