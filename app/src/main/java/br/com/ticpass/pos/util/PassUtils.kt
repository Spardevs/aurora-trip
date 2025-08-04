package br.com.ticpass.pos.util

import android.content.Context
import br.com.ticpass.pos.view.ui.pass.PassType
import java.io.File

fun getSavedPasses(context: Context, passType: PassType): List<File> {
    val passTypeName = when (passType) {
        is PassType.ProductCompact -> "ProductCompact"
        is PassType.ProductExpanded -> "ProductExpanded"
        is PassType.ProductGrouped -> "ProductGrouped"
    }

    val directory = File(context.filesDir, passTypeName)
    return if (directory.exists()) {
        directory.listFiles()?.toList() ?: emptyList()
    } else {
        emptyList()
    }
}

fun clearSavedPasses(context: Context, passType: PassType) {
    val passTypeName = when (passType) {
        is PassType.ProductCompact -> "ProductCompact"
        is PassType.ProductExpanded -> "ProductExpanded"
        is PassType.ProductGrouped -> "ProductGrouped"
    }

    val directory = File(context.filesDir, passTypeName)
    if (directory.exists()) {
        directory.listFiles()?.forEach { it.delete() }
    }
}