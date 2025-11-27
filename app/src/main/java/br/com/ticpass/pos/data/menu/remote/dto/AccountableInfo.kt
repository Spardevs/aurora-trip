package br.com.ticpass.pos.data.menu.remote.dto

import com.google.gson.annotations.SerializedName

data class AccountableInfo(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String
)