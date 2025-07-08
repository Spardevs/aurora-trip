package br.com.ticpass.pos.data.helper

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class PrefsHelper(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAuthData(authData: AuthData) {
        val json = gson.toJson(authData)
        sharedPref.edit().putString("auth_data", json).apply()
    }

    fun getAuthData(): AuthData? {
        val json = sharedPref.getString("auth_data", null)
        return gson.fromJson(json, AuthData::class.java)
    }

    fun clearAuthData() {
        sharedPref.edit().remove("auth_data").apply()
    }
}

data class AuthData(
    val status: Int,
    val message: String,
    val result: ResultData,
    val error: String?,
    val name: String?
)

data class ResultData(
    val token: String,
    val tokenRefresh: String,
    val user: UserData
)

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
    val hash: String,
    val qrcode: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val operator: String?
)