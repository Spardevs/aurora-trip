package br.com.ticpass.pos.data.model

import com.aurora.gplayapi.data.models.App

data class PaginatedAppList(
    val appList: MutableList<App> = mutableListOf(),
    var hasMore: Boolean
)