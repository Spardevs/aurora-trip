package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories" )
data class CategoryEntity(
    @PrimaryKey val id: String,
    var name: String,
) {

    override fun toString() = name
}