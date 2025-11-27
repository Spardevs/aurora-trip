package br.com.ticpass.pos.data.menu.remote.dto

import com.google.gson.annotations.SerializedName

data class PageInfo(
    @SerializedName("total")
    val total: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("pageCount")
    val pageCount: Int,
    @SerializedName("hasNextPage")
    val hasNextPage: Boolean,
    @SerializedName("nextPage")
    val nextPage: Int?,
    @SerializedName("hasPrevPage")
    val hasPrevPage: Boolean,
    @SerializedName("prevPage")
    val prevPage: Int?,
    @SerializedName("cursor")
    val cursor: Int
)