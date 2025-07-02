package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pass_settings")
data class PassSettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mode: String,
    val pricePrinting: Boolean
)