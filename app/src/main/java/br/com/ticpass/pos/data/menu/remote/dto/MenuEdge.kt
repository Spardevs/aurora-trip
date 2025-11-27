package br.com.ticpass.pos.data.menu.remote.dto

import com.google.gson.annotations.SerializedName

data class MenuEdge(
    @SerializedName("id")
    val id: String,
    @SerializedName("label")
    val label: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("mode")
    val mode: String,
    @SerializedName("logo")
    val logo: String?,
    @SerializedName("goal")
    val goal: Long,
    @SerializedName("date")
    val date: EventDate,
    @SerializedName("pass")
    val pass: PassInfo,
    @SerializedName("payment")
    val payment: PaymentInfo,
    @SerializedName("team")
    val team: List<String>,
    @SerializedName("accountable")
    val accountable: AccountableInfo,
    @SerializedName("createdBy")
    val createdBy: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)