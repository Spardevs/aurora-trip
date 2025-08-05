package br.com.ticpass.pos.view.ui.pass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.util.generateEAN13BarcodeBitmap
import br.com.ticpass.pos.view.ui.pass.adapter.PassAdapter
import kotlinx.serialization.Serializable

@Composable
fun PassScreen(
    passType: PassType,
    passList: List<PassData>,
    modifier: Modifier = Modifier
) {
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
            factory = { context ->
                RecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = PassAdapter(passType, passList)
                    setHasFixedSize(true)

                    // Configuração para centralizar os itens
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            },
            update = { recyclerView ->
                (recyclerView.adapter as? PassAdapter)?.updateData(passType, passList)
            }
        )
    }
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
