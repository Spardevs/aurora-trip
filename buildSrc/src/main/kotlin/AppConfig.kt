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

import PropertyDelegates.required

/**
 * Application configuration properties from Gradle
 */
object AppConfig {
    val packageCloudReadToken by required("PACKAGE_CLOUD_READ_TOKEN")
    val apiTelemetryInterval by required("API_TELEMETRY_INTERVAL")
    val apiEventSyncInterval by required("API_EVENT_SYNC_INTERVAL") 
    val apiCheckDuePaymentsInterval by required("API_CHECK_DUE_PAYMENTS_INTERVAL")
    val apiAlertDuePaymentsInterval by required("API_ALERT_DUE_PAYMENTS_INTERVAL")
    val apiMaxDuePaymentsDays by required("API_MAX_DUE_PAYMENTS_DAYS")
    val apiPosSyncInterval by required("API_POS_SYNC_INTERVAL")
    val posOldRecordsRemovalInterval by required("REMOVE_OLD_RECORDS_INTERVAL")
    val apiHost by required("API_HOST")
    val apiMaxRetries by required("API_MAX_RETRIES")
    val apiTimeoutSeconds by required("API_TIMEOUT_SECONDS")
    val qrCodeAuthCode by required("STONE_QRCODE_AUTH") 
    val qrCodeAuthProviderId by required("STONE_QRCODE_PROVIDER_ID")
    val passReprintingMaxRetries by required("PASS_REPRINTING_MAX_RETRIES")

    val releaseStoreFile by required("RELEASE_STORE_FILE")
    val releaseStorePassword by required("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias by required("RELEASE_KEY_ALIAS")
    val releaseKeyPassword by required("RELEASE_KEY_PASSWORD")

    val positivoStoreFile by required("POSITIVO_STORE_FILE")
    val positivoStorePassword by required("POSITIVO_STORE_PASSWORD")
    val positivoKeyAlias by required("POSITIVO_KEY_ALIAS")
    val positivoKeyPassword by required("POSITIVO_KEY_PASSWORD")

    val gertecStoreFile by required("GERTEC_STORE_FILE")
    val gertecStorePassword by required("GERTEC_STORE_PASSWORD")
    val gertecKeyAlias by required("GERTEC_KEY_ALIAS")
    val gertecKeyPassword by required("GERTEC_KEY_PASSWORD")

    val conversionFactor by required("CONVERSION_FACTOR")
    val appName by required("APP_NAME")
}
