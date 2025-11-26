package br.com.ticpass.pos.core.network

import br.com.ticpass.pos.data.local.database.AppDatabase
import br.com.ticpass.pos.data.user.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val database: AppDatabase
) {
    @Volatile
    private var cachedAccessToken: String? = null

    init {
        // inicializa cache em background
        CoroutineScope(Dispatchers.IO).launch {
            cachedAccessToken = database.userDao().getUserById("current_user").firstOrNull()?.accessToken
        }
    }

    suspend fun getAccessToken(): String? {
        return cachedAccessToken ?: database.userDao().getUserById("current_user").firstOrNull()?.accessToken?.also {
            cachedAccessToken = it
        }
    }

    // getter sincrono para uso em Interceptor
    fun getAccessTokenSync(): String? = cachedAccessToken

    suspend fun saveTokens(accessToken: String?, refreshToken: String?) {
        val userEntity = UserEntity(
            id = "current_user",
            accessToken = accessToken,
            refreshToken = refreshToken
        )
        database.userDao().insert(userEntity)
        cachedAccessToken = accessToken
    }

    fun saveTokensAsync(accessToken: String?, refreshToken: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            saveTokens(accessToken, refreshToken)
        }
    }

    suspend fun clearTokens() {
        database.userDao().deleteAll()
        cachedAccessToken = null
    }
}