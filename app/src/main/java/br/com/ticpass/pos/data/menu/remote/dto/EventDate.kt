package br.com.ticpass.pos.data.menu.remote.dto

import com.google.gson.annotations.SerializedName

data class EventDate(
    @SerializedName("start")
    val start: String,
    @SerializedName("end")
    val end: String)