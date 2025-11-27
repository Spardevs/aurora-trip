package br.com.ticpass.pos.data.menu.remote.dto

import androidx.compose.foundation.pager.PageInfo
import com.google.gson.annotations.SerializedName

data class MenuResponse(
    @SerializedName("edges")
    val edges: List<MenuEdge>,
    @SerializedName("info")
    val info: PageInfo
)