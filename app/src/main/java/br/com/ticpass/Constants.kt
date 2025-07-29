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

package br.com.ticpass

import br.com.ticpass.pos.BuildConfig
import br.com.ticpass.pos.MainActivity

object Constants {
    const val POS_VERSION_CODE = BuildConfig.VERSION_CODE
    const val MAIN_DATABASE_NAME = "aurora-trip-pos"
    val POS_SYNC_INTERVAL = BuildConfig.POS_SYNC_INTERVAL.toLong()
    const val API_HOST = BuildConfig.API_HOST
    val REMOVE_OLD_RECORDS_INTERVAL = BuildConfig.REMOVE_OLD_RECORDS_INTERVAL.toLong()
    val EVENT_SYNC_INTERVAL = BuildConfig.EVENT_SYNC_INTERVAL.toLong()
    val TELEMETRY_INTERVAL = BuildConfig.TELEMETRY_INTERVAL.toLong()
    const val STONE_QRCODE_AUTH = BuildConfig.STONE_QRCODE_AUTH
    const val STONE_QRCODE_PROVIDER_ID = BuildConfig.STONE_QRCODE_PROVIDER_ID
    val PASS_REPRINTING_MAX_RETRIES = BuildConfig.PASS_REPRINTING_MAX_RETRIES.toLong().toInt()
    val API_MAX_RETRIES = BuildConfig.API_MAX_RETRIES.toLong()
    val API_TIMEOUT_SECONDS = BuildConfig.API_TIMEOUT_SECONDS.toLong()
    val CHECK_DUE_PAYMENTS_INTERVAL = BuildConfig.CHECK_DUE_PAYMENTS_INTERVAL.toLong()
    val ALERT_DUE_PAYMENTS_INTERVAL = BuildConfig.ALERT_DUE_PAYMENTS_INTERVAL.toLong()
    val MAX_DUE_PAYMENTS_DAYS = BuildConfig.MAX_DUE_PAYMENTS_DAYS.toLong()
    val CONVERSION_FACTOR = BuildConfig.CONVERSION_FACTOR.toLong()
    const val APP_NAME = BuildConfig.APP_NAME
    const val NFC_KEY_TYPE_A = BuildConfig.NFC_KEY_TYPE_A
    const val NFC_KEY_TYPE_B = BuildConfig.NFC_KEY_TYPE_B

    const val REQUEST_CART_UPDATE = 1001


    const val PARCEL_DOWNLOAD = "PARCEL_DOWNLOAD"

    const val URL_TOS = "https://play.google.com/about/play-terms/"
    const val URL_LICENSE = "https://gitlab.com/AuroraOSS/AuroraStore/blob/master/LICENSE"
    const val URL_DISCLAIMER = "https://gitlab.com/AuroraOSS/AuroraStore/blob/master/DISCLAIMER.md"
    const val URL_POLICY = "https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/POLICY.md"

    const val EXODUS_SUBMIT_PAGE = "https://reports.exodus-privacy.eu.org/analysis/submit/#"
    const val EXODUS_REPORT_URL = "https://reports.exodus-privacy.eu.org/reports/"
    const val EXODUS_SEARCH_URL = "https://reports.exodus-privacy.eu.org/api/search/"

    const val PLEXUS_API_URL = "https://plexus.techlore.tech/api/v1/apps"
    const val PLEXUS_SEARCH_URL = "https://plexus.techlore.tech/?q="

    const val SHARE_URL = "https://play.google.com/store/apps/details?id="

    const val UPDATE_URL_STABLE = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/updates.json"
    const val UPDATE_URL_NIGHTLY =
        "https://auroraoss.com/downloads/AuroraStore/Feeds/nightly_feed.json"

    const val NOTIFICATION_CHANNEL_EXPORT = "NOTIFICATION_CHANNEL_EXPORT"
    const val NOTIFICATION_CHANNEL_INSTALL = "NOTIFICATION_CHANNEL_INSTALL"
    const val NOTIFICATION_CHANNEL_DOWNLOADS = "NOTIFICATION_CHANNEL_DOWNLOADS"
    const val NOTIFICATION_CHANNEL_UPDATES = "NOTIFICATION_CHANNEL_UPDATES"
    const val NOTIFICATION_CHANNEL_ACCOUNT = "NOTIFICATION_CHANNEL_ACCOUNT"

    const val GITLAB_URL = "https://gitlab.com/AuroraOSS/AuroraStore"
    const val URL_DISPENSER = "https://auroraoss.com/api/auth"

    //ACCOUNTS
    const val ACCOUNT_SIGNED_IN = "ACCOUNT_SIGNED_IN"
    const val ACCOUNT_TYPE = "ACCOUNT_TYPE"
    const val ACCOUNT_EMAIL_PLAIN = "ACCOUNT_EMAIL_PLAIN"
    const val ACCOUNT_AAS_PLAIN = "ACCOUNT_AAS_PLAIN"
    const val ACCOUNT_AUTH_PLAIN = "ACCOUNT_AUTH_PLAIN"

    const val PAGE_TYPE = "PAGE_TYPE"
    const val TOP_CHART_TYPE = "TOP_CHART_TYPE"
    const val TOP_CHART_CATEGORY = "TOP_CHART_CATEGORY"

    const val JSON_MIME_TYPE = "application/json"
}
