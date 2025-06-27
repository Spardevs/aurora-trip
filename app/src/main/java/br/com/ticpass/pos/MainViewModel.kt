/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package br.com.ticpass.pos

import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.data.helper.UpdateHelper
import br.com.ticpass.pos.data.providers.NetworkProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val networkProvider: NetworkProvider,
    val updateHelper: UpdateHelper
) : ViewModel()
