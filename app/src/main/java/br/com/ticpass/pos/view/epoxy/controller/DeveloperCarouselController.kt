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

import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import br.com.ticpass.pos.view.epoxy.groups.CarouselShimmerGroup
import br.com.ticpass.pos.view.epoxy.groups.DeveloperModelGroup

open class DeveloperCarouselController(private val callbacks: Callbacks) :
    GenericCarouselController(callbacks) {

    override fun applyFilter(streamBundle: StreamCluster): Boolean {
        return streamBundle.clusterTitle.isNotBlank()  //Filter noisy cluster
                && streamBundle.clusterAppList.isNotEmpty() //Filter empty clusters
    }

    override fun buildModels(streamBundle: StreamBundle?) {
        setFilterDuplicates(true)
        if (streamBundle == null) {
            for (i in 1..2) {
                add(
                    CarouselShimmerGroup()
                        .id(i)
                )
            }
        } else {
            streamBundle
                .streamClusters
                .values
                .filter { applyFilter(it) }
                .forEach { streamCluster ->
                    add(DeveloperModelGroup(streamCluster, callbacks))
                }
        }
    }
}