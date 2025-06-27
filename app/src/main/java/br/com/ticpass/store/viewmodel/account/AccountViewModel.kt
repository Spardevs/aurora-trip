/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package br.com.ticpass.store.viewmodel.account

import androidx.lifecycle.ViewModel
import br.com.ticpass.store.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(val authProvider: AuthProvider) : ViewModel()
