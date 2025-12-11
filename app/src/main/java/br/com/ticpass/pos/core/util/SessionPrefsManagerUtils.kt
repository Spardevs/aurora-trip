package br.com.ticpass.pos.core.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SessionPrefsManagerUtils {
    private const val PREFS_NAME = "SessionPrefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun clearAll() {
        prefs.edit { clear() }
    }

    fun saveSelectedMenuId(menuId: String) {
        prefs.edit { putString("selected_menu_id", menuId) }
    }

    fun getSelectedMenuId(): String? {
        return prefs.getString("selected_menu_id", null)
    }

    fun saveMenuStartDate(startDate: String) {
        prefs.edit { putString("menu_start_date", startDate) }
    }

    fun getMenuStartDate(): String? {
        return prefs.getString("menu_start_date", null)
    }

    fun saveMenuEndDate(endDate: String) {
        prefs.edit { putString("menu_end_date", endDate) }
    }

    fun getMenuEndDate(): String? {
        return prefs.getString("menu_end_date", null)
    }

    fun saveMenuName(name: String) {
        prefs.edit { putString("menu_name", name) }
    }

    fun getMenuName(): String? {
        return prefs.getString("menu_name", null)
    }

    fun savePosId(posId: String) {
        prefs.edit { putString("pos_id", posId) }
    }

    fun getPosId(): String? {
        return prefs.getString("pos_id", null)
    }

    fun savePosName(posName: String) {
        prefs.edit { putString("pos_name", posName) }
    }

    fun getPosName(): String? {
        return prefs.getString("pos_name", null)
    }

    fun savePosCommission(commission: Long) {
        prefs.edit { putLong("pos_commission", commission) }
    }

    fun getPosCommission(): Long? {
        return prefs.getLong("pos_commission", 0)
    }

    fun savePosAccessToken(token: String) {
        prefs.edit { putString("pos_access_token", token) }
    }

    fun getPosAccessToken(): String? {
        return prefs.getString("pos_access_token", null)
    }

    fun saveProxyCredentials(credentials: String) {
        prefs.edit { putString("proxy_credentials", credentials) }
    }

    fun getProxyCredentials(): String? {
        return prefs.getString("proxy_credentials", null)
    }

    fun saveCashierName(name: String) {
        prefs.edit { putString("cashier_name", name) }
    }

    fun getCashierName(): String? {
        return prefs.getString("cashier_name", null)
    }

    fun saveDeviceId(id: String) {
        prefs.edit { putString("device_id", id) }
    }

    fun getDeviceId(): String? {
        return prefs.getString("device_id", null)
    }
}