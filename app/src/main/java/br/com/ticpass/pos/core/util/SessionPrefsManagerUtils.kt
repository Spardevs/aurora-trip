package br.com.ticpass.pos.core.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SessionPrefsManager {
    private const val PREFS_NAME = "session_prefs"
    private const val SELECTED_MENU_ID_KEY = "selected_menu_id"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSelectedMenuId(menuId: String) {
        prefs.edit().putString(SELECTED_MENU_ID_KEY, menuId).apply()
    }

    fun getSelectedMenuId(): String? {
        return prefs.getString(SELECTED_MENU_ID_KEY, null)
    }

    fun clearSelectedMenuId() {
        prefs.edit { remove(SELECTED_MENU_ID_KEY) }
    }
}