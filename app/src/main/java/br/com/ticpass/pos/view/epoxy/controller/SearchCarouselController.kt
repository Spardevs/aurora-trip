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
import com.aurora.gplayapi.data.models.StreamBundle
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.epoxy.controller.GenericCarouselController.Callbacks
import br.com.ticpass.pos.view.epoxy.groups.CarouselModelGroup
import br.com.ticpass.pos.view.epoxy.views.app.AppListViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.NoAppViewModel_
import br.com.ticpass.pos.view.epoxy.views.shimmer.AppListViewShimmerModel_

open class SearchCarouselController(private val callbacks: Callbacks) :

    TypedEpoxyController<StreamBundle?>() {

    override fun buildModels(streamBundle: StreamBundle?) {
        setFilterDuplicates(true)
        if (streamBundle == null) {
            for (i in 1..6) {
                add(
                    AppListViewShimmerModel_()
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
                streamBundle.streamClusters.values
                    .filter { it.clusterAppList.isNotEmpty() } // Filter out empty clusters, mostly related keywords
                    .forEach {
                        if (it.clusterTitle.isEmpty() or (it.clusterTitle == streamBundle.streamTitle)) {
                            if (it.clusterAppList.isNotEmpty()) {
                                it.clusterAppList.forEach { app ->
                                    add(
                                        AppListViewModel_()
                                            .id(app.id)
                                            .app(app)
                                            .click { _ -> callbacks.onAppClick(app) }
                                    )
                                }
                            }
                        } else {
                            add(CarouselModelGroup(it, callbacks))
                        }
                    }

                if (streamBundle.hasNext())
                    add(
                        AppListViewShimmerModel_()
                            .id("progress")
                    )
            }
        }
    }
}
