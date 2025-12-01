package br.com.ticpass.pos.core.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SessionPrefsManagerUtils {
    private const val PREFS_NAME = "SessionPrefs"
    private const val SELECTED_MENU_ID_KEY = "selected_menu_id"
    private const val POS_ID_KEY = "pos_id"
    private const val POS_NAME_KEY = "pos_name"
    private const val POS_COMMISSION_KEY = "pos_commission"
    private const val POS_ACCESS_TOKEN_KEY = "pos_access_token"
    private const val PROXY_CREDENTIALS_KEY = "proxy_credentials"

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

    fun clearSelectedMenuId() {
        prefs.edit { remove(SELECTED_MENU_ID_KEY) }
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
}