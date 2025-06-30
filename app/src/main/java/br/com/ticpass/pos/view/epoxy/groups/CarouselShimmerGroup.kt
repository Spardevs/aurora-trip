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

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyModelGroup
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.epoxy.views.shimmer.AppViewShimmerModel_
import br.com.ticpass.pos.view.epoxy.views.shimmer.HeaderViewShimmerModel_
import java.util.UUID

class CarouselShimmerGroup :
    EpoxyModelGroup(
        R.layout.model_carousel_group, buildModels()
    ) {
    companion object {
        private fun buildModels(): List<EpoxyModel<*>> {
            val models = ArrayList<EpoxyModel<*>>()
            val clusterViewModels = mutableListOf<EpoxyModel<*>>()
            val idPrefix = UUID.randomUUID()

            for (i in 1..8) {
                clusterViewModels.add(
                    AppViewShimmerModel_()
                        .id(i)
                )
            }

            models.add(
                HeaderViewShimmerModel_()
                    .id("shimmer_header")
            )

            models.add(
                CarouselHorizontalModel_()
                    .id("cluster_${idPrefix}")
                    .models(clusterViewModels)
            )
            return models
        }
    }
}
