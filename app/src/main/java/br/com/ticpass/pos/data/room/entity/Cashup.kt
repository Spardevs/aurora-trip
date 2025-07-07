package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.compose.utils.generateRandomEAN
import br.com.ticpass.pos.compose.utils.generateObjectId

@Entity(tableName = "cashups" )
data class CashupEntity(
    @PrimaryKey()
    val id: String = generateRandomEAN(),
    var accountable: String,
    var createdAt: String,
    var initial: Long,
    var taken: Long,
    var remaining: Long,
    var synced: Boolean = false,
) {

    override fun toString() = id.toString()
}