package br.com.ticpass.pos.core.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SessionPrefsManagerUtils {
    private const val PREFS_NAME = "SessionPrefs"
    private const val SELECTED_MENU_ID_KEY = "selected_menu_id"
    private const val MENU_START_DATE_KEY = "menu_start_date"
    private const val MENU_END_DATE_KEY = "menu_end_date"
    private const val MENU_NAME_KEY = "menu_name"
    private const val POS_ID_KEY = "pos_id"
    private const val POS_NAME_KEY = "pos_name"
    private const val POS_COMMISSION_KEY = "pos_commission"
    private const val POS_ACCESS_TOKEN_KEY = "pos_access_token"
    private const val PROXY_CREDENTIALS_KEY = "proxy_credentials"
    private const val OPERATOR_NAME_KEY = "operator_name"
    private const val DEVICE_SERIAL_KEY = "device_serial"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSelectedMenuId(menuId: String) {
        prefs.edit { putString(SELECTED_MENU_ID_KEY, menuId) }
    }

    fun getSelectedMenuId(): String? {
        return prefs.getString(SELECTED_MENU_ID_KEY, null)
    }

    fun saveMenuStartDate(startDate: String) {
        prefs.edit { putString(MENU_START_DATE_KEY, startDate) }
    }

    fun getMenuStartDate(): String? {
        return prefs.getString(MENU_START_DATE_KEY, null)
    }

    fun saveMenuEndDate(endDate: String) {
        prefs.edit { putString(MENU_END_DATE_KEY, endDate) }
    }

    fun getMenuEndDate(): String? {
        return prefs.getString(MENU_END_DATE_KEY, null)
    }

    fun saveMenuName(name: String) {
        prefs.edit { putString(MENU_NAME_KEY, name) }
    }

    fun getMenuName(): String? {
        return prefs.getString(MENU_NAME_KEY, null)
    }
    fun clearSelectedMenuId() {
        prefs.edit {
            remove(SELECTED_MENU_ID_KEY)
            remove(MENU_START_DATE_KEY)
            remove(MENU_END_DATE_KEY)
            remove(MENU_NAME_KEY)
        }
    }

    fun savePosId(posId: String) {
        prefs.edit { putString(POS_ID_KEY, posId) }
    }

    fun getPosId(): String? {
        return prefs.getString(POS_ID_KEY, null)
    }

    fun savePosName(posName: String) {
        prefs.edit { putString(POS_NAME_KEY, posName) }
    }

    fun getPosName(): String? {
        return prefs.getString(POS_NAME_KEY, null)
    }

    fun savePosCommission(commission: Long) {
        prefs.edit { putLong(POS_COMMISSION_KEY, commission) }
    }

    fun getPosCommission(): Long? {
        return prefs.getLong(POS_COMMISSION_KEY, 0)
    }

    fun savePosAccessToken(token: String) {
        prefs.edit { putString(POS_ACCESS_TOKEN_KEY, token) }
    }

    fun getPosAccessToken(): String? {
        return prefs.getString(POS_ACCESS_TOKEN_KEY, null)
    }

    fun saveProxyCredentials(credentials: String) {
        prefs.edit { putString(PROXY_CREDENTIALS_KEY, credentials) }
    }

    fun getProxyCredentials(): String? {
        return prefs.getString(PROXY_CREDENTIALS_KEY, null)
    }

    fun saveOperatorName(name: String) {
        prefs.edit { putString(OPERATOR_NAME_KEY, name) }
    }

    fun getOperatorName(): String? {
        return prefs.getString(OPERATOR_NAME_KEY, null)
    }
    fun saveDeviceSerial(serial: String) {
        prefs.edit { putString(DEVICE_SERIAL_KEY, serial) }
    }

    fun getDeviceSerial(): String? {
        return prefs.getString(DEVICE_SERIAL_KEY, null)
    }
}