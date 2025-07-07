package br.com.ticpass.pos.util

import br.com.ticpass.pos.BuildConfig
import br.com.ticpass.pos.MainActivity

const val DATABASE_NAME = "ticpass-pos"
const val SEED_DATA_FILENAME = "plants.json"
const val API_HOST = BuildConfig.API_HOST
val POS_SYNC_INTERVAL = BuildConfig.POS_SYNC_INTERVAL.toLong()
val REMOVE_OLD_RECORDS_INTERVAL = BuildConfig.REMOVE_OLD_RECORDS_INTERVAL.toLong()
val EVENT_SYNC_INTERVAL = BuildConfig.EVENT_SYNC_INTERVAL.toLong()
val TELEMETRY_INTERVAL = BuildConfig.TELEMETRY_INTERVAL.toLong()
val APP_NAME = getAppName()
val STONE_QRCODE_AUTH = BuildConfig.STONE_QRCODE_AUTH
val STONE_QRCODE_PROVIDER_ID = BuildConfig.STONE_QRCODE_PROVIDER_ID
val PASS_REPRINTING_MAX_RETRIES = BuildConfig.PASS_REPRINTING_MAX_RETRIES.toLong().toInt()
val API_MAX_RETRIES = BuildConfig.API_MAX_RETRIES.toLong()
val API_TIMEOUT_SECONDS = BuildConfig.API_TIMEOUT_SECONDS.toLong()
val CHECK_DUE_PAYMENTS_INTERVAL = BuildConfig.CHECK_DUE_PAYMENTS_INTERVAL.toLong()
val ALERT_DUE_PAYMENTS_INTERVAL = BuildConfig.ALERT_DUE_PAYMENTS_INTERVAL.toLong()
val MAX_DUE_PAYMENTS_DAYS = BuildConfig.MAX_DUE_PAYMENTS_DAYS.toLong()

private fun getAppName(): String {
    val context = MainActivity.appContext
    val applicationInfo = context.applicationInfo
    val stringId = applicationInfo.labelRes
    return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(
        stringId
    )
}

