package br.com.ticpass.pos.domain.login.usecase


import br.com.ticpass.pos.data.auth.remote.dto.LoginResponse
import br.com.ticpass.pos.data.auth.repository.AuthRepositoryImpl
import javax.inject.Inject

class SignInWithQrUseCase @Inject constructor(
    private val authRepositoryImpl: AuthRepositoryImpl
) {
    suspend operator fun invoke(shortLivedToken: String, pin: String): Result<Pair<LoginResponse, Pair<String?, String?>>> {
        return try {
            val response = authRepositoryImpl.signInWithQrCode(shortLivedToken, pin)
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(Exception("Resposta vazia do servidor"))
                // Extrai cookies Set-Cookie: access=...; ..., refresh=...; ...
                val cookies = response.headers().values("Set-Cookie")
                val accessCookie = cookies.firstOrNull { it.startsWith("access=") }
                val refreshCookie = cookies.firstOrNull { it.startsWith("refresh=") }

                val accessToken = accessCookie
                    ?.substringAfter("access=")
                    ?.substringBefore(";")
                    ?.takeIf { it.isNotBlank() }
                    ?: body.jwt.access

                val refreshToken = refreshCookie
                    ?.substringAfter("refresh=")
                    ?.substringBefore(";")
                    ?.takeIf { it.isNotBlank() }
                    ?: body.jwt.refresh

                Result.success(Pair(body, Pair(accessToken, refreshToken)))
            } else {
                val msg = when (response.code()) {
                    400 -> "Requisição inválida"
                    401 -> "Não autorizado: token ou PIN inválido"
                    in 500..599 -> "Erro no servidor"
                    else -> "Erro desconhecido (status ${response.code()})"
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}