package br.com.ticpass.pos.view.ui.pass

import android.content.Context
import android.content.SharedPreferences
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.view.ui.pass.adapter.PassAdapter
import kotlinx.serialization.Serializable

@Composable
fun PassScreen(
    passType: PassType,
    passList: List<PassData>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Resolve PassType a partir do SharedPreferences "ConfigPrefs"
    val effectiveType = resolvePassTypeFromPrefs(context, passType)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            factory = { ctx ->
                RecyclerView(ctx).apply {
                    layoutManager = LinearLayoutManager(ctx)
                    adapter = PassAdapter(effectiveType, passList).also {
                        it.initTypeFromPrefs(ctx)
                    }
                    setHasFixedSize(true)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            },
            update = { recyclerView ->
                val adapter = recyclerView.adapter as? PassAdapter ?: return@AndroidView
                // Atualiza dados e tipo a partir das prefs
                adapter.updateDataFromPrefs(context, passList)
            }
        )
    }
}

private fun resolvePassTypeFromPrefs(context: Context, fallback: PassType): PassType {
    val prefs = context.getSharedPreferences("ConfigPrefs", Context.MODE_PRIVATE)
    val format = prefs.getString("print_format", "DEFAULT")
    return PassAdapter.mapFormatToPassType(format) ?: fallback
}

sealed class PassType {
    object ProductCompact : PassType()
    object ProductExpanded : PassType()
    object ProductGrouped : PassType()
}

@Serializable
data class PassData(
    val header: HeaderData = HeaderData(),
    val footer: FooterData = FooterData(),
    val showCutLine: Boolean = false,
    val productData: ProductData? = null,
    val groupedData: GroupedData? = null
) {
    @Serializable
    data class HeaderData(
        val title: String = "ticpass",
        val date: String = "",
        val barcode: String = ""
    )

    @Serializable
    data class FooterData(
        val description: String = "",
        val printTime: String = "",
        val cashierName: String = "",
        val menuName: String = "",
        val printerInfo: String = ""
    )

    @Serializable
    data class ProductData(
        val name: String = "",
        val price: String = "",
        val eventTitle: String = "",
        val eventTime: String = "",
        val observation: String? = null
    )

    @Serializable
    data class GroupedData(
        val items: List<GroupedItem> = emptyList(),
        val totalItems: Int = 0,
        val totalPrice: String = ""
    ) {
        @Serializable
        data class GroupedItem(
            val quantity: Int,
            val name: String,
            val price: String,
            val observation: String? = null
        )
    }
}