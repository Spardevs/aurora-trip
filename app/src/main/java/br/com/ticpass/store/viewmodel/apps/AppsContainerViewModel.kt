/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package br.com.ticpass.store.viewmodel.apps

import androidx.lifecycle.ViewModel
import br.com.ticpass.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppsContainerViewModel @Inject constructor(val authProvider: AuthProvider) : ViewModel()
