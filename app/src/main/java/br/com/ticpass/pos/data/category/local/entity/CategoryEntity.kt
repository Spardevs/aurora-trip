package br.com.ticpass.pos.data.category.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.ColumnInfo

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "menuId") val menuId: String,
    @ColumnInfo(name = "name") val name: String
)