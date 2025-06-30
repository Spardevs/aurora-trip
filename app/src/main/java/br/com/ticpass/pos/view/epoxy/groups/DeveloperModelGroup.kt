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

package br.com.ticpass.pos.view.epoxy.groups

import android.util.Log
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelGroup
import com.aurora.gplayapi.data.models.StreamCluster
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.epoxy.controller.GenericCarouselController
import br.com.ticpass.pos.view.epoxy.views.HeaderViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.AppListViewModel_
import br.com.ticpass.pos.view.epoxy.views.app.AppViewModel_
import br.com.ticpass.pos.view.epoxy.views.details.ScreenshotViewModel_

class DeveloperModelGroup(
    streamCluster: StreamCluster,
    callbacks: GenericCarouselController.Callbacks
) :
    EpoxyModelGroup(
        R.layout.model_developer_carousel_group, buildModels(
            streamCluster,
            callbacks
        )
    ) {
    companion object {
        private const val TAG = "DeveloperModelGroup"

        private fun buildModels(
            streamCluster: StreamCluster,
            callbacks: GenericCarouselController.Callbacks
        ): List<EpoxyModel<*>> {
            val models = ArrayList<EpoxyModel<*>>()
            val clusterViewModels = mutableListOf<EpoxyModel<*>>()
            val screenshotsViewModels = mutableListOf<EpoxyModel<*>>()

            val idPrefix = streamCluster.id

            models.add(
                HeaderViewModel_()
                    .id("${idPrefix}_header")
                    .title(streamCluster.clusterTitle)
                    .browseUrl(streamCluster.clusterBrowseUrl)
                    .click { _ ->
                        callbacks.onHeaderClicked(streamCluster)
                    }
            )

            if (streamCluster.clusterAppList.size == 1) {
                val app = streamCluster.clusterAppList[0]

                for (artwork in app.screenshots) {
                    screenshotsViewModels.add(
                        ScreenshotViewModel_()
                            .id(artwork.url)
                            .artwork(artwork)
                    )
                }

                clusterViewModels.add(
                    AppListViewModel_()
                        .id(app.id)
                        .app(app)
                        .click { _ ->
                            callbacks.onAppClick(app)
                        }
                )
            } else {
                for (app in streamCluster.clusterAppList) {
                    clusterViewModels.add(
                        AppViewModel_()
                            .id(app.id)
                            .app(app)
                            .click { _ ->
                                callbacks.onAppClick(app)
                            }
                            .longClick { _ ->
                                callbacks.onAppLongClick(app)
                                false
                            }
                            .onBind { _, _, position ->
                                val itemCount = clusterViewModels.count()
                                if (itemCount >= 2) {
                                    if (position == clusterViewModels.count() - 2) {
                                        callbacks.onClusterScrolled(streamCluster)
                                        Log.i(TAG, "Cluster ${streamCluster.clusterTitle} Scrolled")
                                    }
                                }
                            }
                    )
                }
            }

            if (screenshotsViewModels.isNotEmpty()) {
                models.add(
                    CarouselHorizontalModel_()
                        .id("${idPrefix}_screenshots")
                        .models(screenshotsViewModels)
                )
            }

            models.add(
                CarouselHorizontalModel_()
                    .id("${idPrefix}_cluster")
                    .models(clusterViewModels)
            )

            return models
        }
    }
}
