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



@Composable
private fun ProductPass(
    layoutRes: Int,
    passData: PassData,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = LayoutInflater.from(context).inflate(layoutRes, null, false)

            val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
            val barcodeBitmap = try {
                generateEAN13BarcodeBitmap(passData.header.barcode)
            } catch (e: Exception) {
                null
            }
            barcodeImage?.setImageBitmap(barcodeBitmap)

            passData.productData?.let { product ->
                view.findViewById<TextView>(R.id.productName)?.text = product.name
                view.findViewById<TextView>(R.id.productPrice)?.text = product.price
                view.findViewById<TextView>(R.id.eventTitle)?.text = product.eventTitle
                view.findViewById<TextView>(R.id.eventTime)?.text = product.eventTime
            }

            view.findViewById<TextView>(R.id.attendantName)?.text = passData.footer.cashierName
            view.findViewById<TextView>(R.id.menuName)?.text = passData.footer.menuName
            view.findViewById<TextView>(R.id.passPrinter)?.text = passData.footer.printerInfo
            view.findViewById<TextView>(R.id.passDescription)?.text = passData.footer.description
            view.findViewById<TextView>(R.id.printTime)?.text = passData.footer.printTime

            view.findViewById<ImageView>(R.id.passCutInHere)?.visibility =
                if (passData.showCutLine) View.VISIBLE else View.GONE

            view
        },
        update = { view ->
            // Atualiza a view ao recompor (ex: mudar passData)
            val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
            val barcodeBitmap = try {
                generateEAN13BarcodeBitmap(passData.header.barcode)
            } catch (e: Exception) {
                null
            }
            barcodeImage?.setImageBitmap(barcodeBitmap)

            passData.productData?.let { product ->
                view.findViewById<TextView>(R.id.productName)?.text = product.name
                view.findViewById<TextView>(R.id.productPrice)?.text = product.price
                view.findViewById<TextView>(R.id.eventTitle)?.text = product.eventTitle
                view.findViewById<TextView>(R.id.eventTime)?.text = product.eventTime
            }

            view.findViewById<TextView>(R.id.attendantName)?.text = passData.footer.cashierName
            view.findViewById<TextView>(R.id.menuName)?.text = passData.footer.menuName
            view.findViewById<TextView>(R.id.passPrinter)?.text = passData.footer.printerInfo
            view.findViewById<TextView>(R.id.passDescription)?.text = passData.footer.description
            view.findViewById<TextView>(R.id.printTime)?.text = passData.footer.printTime

            view.findViewById<ImageView>(R.id.passCutInHere)?.visibility =
                if (passData.showCutLine) View.VISIBLE else View.GONE
        }
    )
}

@Composable
private fun GroupedPass(
    passData: PassData,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = LayoutInflater.from(context).inflate(R.layout.printer_pass_grouped, null, false)

            view.findViewById<TextView>(R.id.headerTitle)?.text = passData.header.title
            view.findViewById<TextView>(R.id.headerDate)?.text = passData.header.date

            val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
            val barcodeBitmap = try {
                generateEAN13BarcodeBitmap(passData.header.barcode)
            } catch (e: Exception) {
                null
            }
            barcodeImage?.setImageBitmap(barcodeBitmap)

            val itemsContainer = view.findViewById<LinearLayout>(R.id.itemsContainer)
            itemsContainer?.removeAllViews()

            passData.groupedData?.items?.forEach { item ->
                val itemView = LayoutInflater.from(context).inflate(R.layout.item_grouped_product, itemsContainer, false)
                itemView.findViewById<TextView>(R.id.itemQuantity)?.text = "${item.quantity}x"
                itemView.findViewById<TextView>(R.id.itemName)?.text = item.name
                itemView.findViewById<TextView>(R.id.itemPrice)?.text = item.price
                itemsContainer?.addView(itemView)
            }

            passData.groupedData?.let {
                view.findViewById<TextView>(R.id.totalItems)?.text =
                    "${it.totalItems} itens - ${it.totalPrice}"
            }

            view.findViewById<TextView>(R.id.cashierInfo)?.text =
                "Caixa: ${passData.footer.menuName}\nOperador: ${passData.footer.cashierName}"
            view.findViewById<TextView>(R.id.footerText)?.text = passData.footer.description
            view.findViewById<TextView>(R.id.printTime)?.text = passData.footer.printTime

            view
        },
        update = { view ->
            // Atualiza view para refletir novos dados caso recomponha
            view.findViewById<TextView>(R.id.headerTitle)?.text = passData.header.title
            view.findViewById<TextView>(R.id.headerDate)?.text = passData.header.date

            val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
            val barcodeBitmap = try {
                generateEAN13BarcodeBitmap(passData.header.barcode)
            } catch (e: Exception) {
                null
            }
            barcodeImage?.setImageBitmap(barcodeBitmap)

            val itemsContainer = view.findViewById<LinearLayout>(R.id.itemsContainer)
            itemsContainer?.removeAllViews()

            passData.groupedData?.items?.forEach { item ->
                val itemView = LayoutInflater.from(view.context).inflate(R.layout.item_grouped_product, itemsContainer, false)
                itemView.findViewById<TextView>(R.id.itemQuantity)?.text = "${item.quantity}x"
                itemView.findViewById<TextView>(R.id.itemName)?.text = item.name
                itemView.findViewById<TextView>(R.id.itemPrice)?.text = item.price
                itemsContainer?.addView(itemView)
            }

            passData.groupedData?.let {
                view.findViewById<TextView>(R.id.totalItems)?.text =
                    "${it.totalItems} itens - ${it.totalPrice}"
            }

            view.findViewById<TextView>(R.id.cashierInfo)?.text =
                "Caixa: ${passData.footer.menuName}\nOperador: ${passData.footer.cashierName}"
            view.findViewById<TextView>(R.id.footerText)?.text = passData.footer.description
            view.findViewById<TextView>(R.id.printTime)?.text = passData.footer.printTime
        }
    )
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
        val eventTime: String = ""
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
            val price: String
        )
    }
}
