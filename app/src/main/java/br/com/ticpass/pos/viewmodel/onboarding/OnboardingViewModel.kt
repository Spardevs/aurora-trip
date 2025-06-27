/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package br.com.ticpass.pos.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import br.com.ticpass.pos.data.helper.UpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(val updateHelper: UpdateHelper) : ViewModel()
