package br.com.ticpass.pos.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "payment_settings")

data class PaymentSettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val methods: List<String>,
    val acquirerEnabled: Boolean,
    val multiPaymentEnabled: Boolean
)