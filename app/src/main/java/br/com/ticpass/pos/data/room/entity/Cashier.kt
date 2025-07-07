package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cashiers")
data class CashierEntity(
    @PrimaryKey val id: String,
    val name: String,
) {

    override fun toString() = name
}
