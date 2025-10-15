package br.com.ticpass.pos.util

import android.content.Context
import br.com.ticpass.pos.view.ui.pass.PassType
import java.io.File

private fun getPrintFormat(context: Context): String {
    val prefs = context.getSharedPreferences("ConfigPrefs", Context.MODE_PRIVATE)
    val raw = (prefs.getString("print_format", "DEFAULT") ?: "DEFAULT").uppercase()
    return if (raw == "DEFAULT") "EXPANDED" else raw
}

private fun passTypeFolderName(passType: PassType): String = when (passType) {
    is PassType.ProductCompact -> "ProductCompact"
    is PassType.ProductExpanded -> "ProductExpanded"
    is PassType.ProductGrouped -> "ProductGrouped"
}

/**
 * Lista passes salvos.
 * - Por padrão (filterByType = false), lista TUDO dentro de filesDir/<print_format>/.
 * - Se filterByType = true, lista somente em filesDir/<print_format>/<PassTypeName>/.
 */
fun getSavedPasses(context: Context, passType: PassType, filterByType: Boolean = false): List<File> {
    val printFormat = getPrintFormat(context)
    val baseDir = File(context.filesDir, printFormat)

    val directory = if (filterByType) {
        File(baseDir, passTypeFolderName(passType))
    } else {
        baseDir
    }

    if (!directory.exists()) return emptyList()

    return if (directory.isDirectory) {
        directory.walkTopDown()
            .maxDepth(if (filterByType) 1 else 2)
            .filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            .toList()
    } else {
        emptyList()
    }
}

/**
 * Limpa passes salvos.
 * - Por padrão (filterByType = false), limpa TUDO dentro de filesDir/<print_format>/.
 * - Se filterByType = true, limpa somente filesDir/<print_format>/<PassTypeName>/.
 */
fun clearSavedPasses(context: Context, passType: PassType, filterByType: Boolean = false) {
    val printFormat = getPrintFormat(context)
    val baseDir = File(context.filesDir, printFormat)

    val directory = if (filterByType) {
        File(baseDir, passTypeFolderName(passType))
    } else {
        baseDir
    }

    if (!directory.exists()) return

    directory.walkTopDown()
        .filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
        .forEach { it.delete() }
}