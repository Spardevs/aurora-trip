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

package br.com.ticpass.pos.view.ui.onboarding

import androidx.fragment.app.Fragment
import br.com.ticpass.Constants
import br.com.ticpass.pos.util.Preferences.PREFERENCE_AUTO_DELETE
import br.com.ticpass.pos.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import br.com.ticpass.pos.util.Preferences.PREFERENCE_DISPENSER_URLS
import br.com.ticpass.pos.util.Preferences.PREFERENCE_FILTER_AURORA_ONLY
import br.com.ticpass.pos.util.Preferences.PREFERENCE_FILTER_FDROID
import br.com.ticpass.pos.util.Preferences.PREFERENCE_FOR_YOU
import br.com.ticpass.pos.util.Preferences.PREFERENCE_INSTALLER_ID
import br.com.ticpass.pos.util.Preferences.PREFERENCE_SIMILAR
import br.com.ticpass.pos.util.Preferences.PREFERENCE_THEME_STYLE
import br.com.ticpass.pos.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import br.com.ticpass.pos.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import br.com.ticpass.pos.util.Preferences.PREFERENCE_VENDING_VERSION
import br.com.ticpass.pos.util.save
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingFragment : BaseFlavouredOnboardingFragment() {

    override fun loadDefaultPreferences() {
        /*Filters*/
        save(PREFERENCE_FILTER_AURORA_ONLY, false)
        save(PREFERENCE_FILTER_FDROID, true)

        /*Network*/
        save(PREFERENCE_DISPENSER_URLS, setOf(Constants.URL_DISPENSER))
        save(PREFERENCE_VENDING_VERSION, 0)

        /*Customization*/
        save(PREFERENCE_THEME_STYLE, 0)
        save(PREFERENCE_DEFAULT_SELECTED_TAB, 0)
        save(PREFERENCE_FOR_YOU, true)
        save(PREFERENCE_SIMILAR, false)

        /*Installer*/
        save(PREFERENCE_AUTO_DELETE, true)
        save(PREFERENCE_INSTALLER_ID, 0)

        /*Updates*/
        save(PREFERENCE_UPDATES_EXTENDED, false)
        save(PREFERENCE_UPDATES_CHECK_INTERVAL, 3)
    }

    override fun onboardingPages(): List<Fragment> {
        return listOf(
            WelcomeFragment(),
            PermissionsFragment.newInstance()
        )
    }
}
