/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package br.com.ticpass.store.viewmodel.sheets

import androidx.lifecycle.ViewModel
import br.com.ticpass.store.data.providers.FilterProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(val filterProvider: FilterProvider) : ViewModel()
