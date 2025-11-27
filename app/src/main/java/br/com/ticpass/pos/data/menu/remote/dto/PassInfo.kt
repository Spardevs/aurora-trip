package br.com.ticpass.pos.data.menu.remote.dto

import com.google.gson.annotations.SerializedName

data class PassInfo(
    @SerializedName("vouchering")
    val vouchering: Boolean,
    @SerializedName("pricing")
    val pricing: Boolean,
    @SerializedName("mode")
    val mode: String,
    @SerializedName("description")
    val description: String
)