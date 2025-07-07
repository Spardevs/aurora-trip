package br.com.ticpass.pos.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pos" )
data class PosEntity(
    @PrimaryKey val id: String,
    var name: String,
    var cashier: String,
    var isClosed: Boolean,
    var isSelected: Boolean,

    @ColumnInfo(defaultValue = "0")
    var commission: Long = 0L,
) {

    override fun toString() = name
}
