package br.com.ticpass.pos.data.model

import androidx.annotation.DrawableRes
import br.com.ticpass.pos.R

data class PermissionGroupInfo(
    val name: String = "unknown",
    @DrawableRes var icon: Int = R.drawable.ic_permission_unknown,
    val label: String = "UNDEFINED"
)
