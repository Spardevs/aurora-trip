package br.com.ticpass.pos.data.menu.remote.dto

import com.google.gson.annotations.SerializedName

data class PaymentInfo(
    @SerializedName("methods")
    val methods: List<String>,
    @SerializedName("multi")
    val multi: Boolean,
    @SerializedName("acquirer")
    val acquirer: Boolean
)