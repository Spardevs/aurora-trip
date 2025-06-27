package br.com.ticpass.store.data.room.favourite

import br.com.ticpass.store.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
data class ImportExport(
    val favourites: List<Favourite>,
    val auroraStoreVersion: Int = BuildConfig.VERSION_CODE,
)
