package br.com.ticpass.pos.data.user.repository

import br.com.ticpass.pos.data.user.local.entity.UserEntity

interface UserRepository {
    /**
     * Retorna o usuário "logado" atual (ou null se não houver nenhum).
     * Implementação deve executar a leitura do DB (Room) — método suspend.
     */
    suspend fun getLoggedUser(): UserEntity?
}