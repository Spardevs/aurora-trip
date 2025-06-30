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

package br.com.ticpass.pos

import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.contracts.TopChartsContract

typealias MR = com.google.android.material.R.attr

typealias TopChartStash = MutableMap<TopChartsContract.Type, MutableMap<TopChartsContract.Chart, StreamCluster>>
typealias HomeStash = MutableMap<StreamContract.Category, StreamBundle>
typealias CategoryStash = MutableMap<Category.Type, List<Category>>
typealias AppStreamStash = MutableMap<String, StreamBundle>
