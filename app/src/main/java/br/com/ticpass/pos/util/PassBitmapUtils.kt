package br.com.ticpass.pos.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.pass.PassData
import br.com.ticpass.pos.view.ui.pass.PassType
import java.io.File
import java.io.FileOutputStream

fun savePassAsBitmap(context: Context, passType: PassType, passData: PassData): File {
    val inflater = LayoutInflater.from(context)

    // Inflar o layout do pass
    val view: View = when (passType) {
        is PassType.ProductCompact ->
            inflateProductLayout(inflater, R.layout.printer_pass_compact, passData)
        is PassType.ProductExpanded ->
            inflateProductLayout(inflater, R.layout.printer_pass_expanded, passData)
        is PassType.ProductGrouped ->
            inflateGroupedLayout(inflater, passData)
    }

    // Converter a view em bitmap
    val bitmap = view.toBitmap()

    // Nome do diretório baseado no tipo de pass
    val passTypeName = when (passType) {
        is PassType.ProductCompact -> "ProductCompact"
        is PassType.ProductExpanded -> "ProductExpanded"
        is PassType.ProductGrouped -> "ProductGrouped"
    }

    // Diretório: files/ProductCompact/, files/ProductExpanded/, etc
    val outputDir = File(context.filesDir, passTypeName)
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    // Nome do arquivo com timestamp
    val fileName = "pass_${System.currentTimeMillis()}.png"
    val file = File(outputDir, fileName)

    // Salvar imagem
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    return file
}

private fun inflateProductLayout(inflater: LayoutInflater, layoutRes: Int, data: PassData): View {
    val view = inflater.inflate(layoutRes, null)

    data.productData?.let { product ->
        view.findViewById<TextView>(R.id.productName)?.text = product.name
        view.findViewById<TextView>(R.id.productPrice)?.text = product.price
        view.findViewById<TextView>(R.id.eventTitle)?.text = product.eventTitle
        view.findViewById<TextView>(R.id.eventTime)?.text = product.eventTime
    }

    view.findViewById<TextView>(R.id.attendantName)?.text = data.footer.cashierName
    view.findViewById<TextView>(R.id.menuName)?.text = data.footer.menuName
    view.findViewById<TextView>(R.id.passPrinter)?.text = data.footer.printerInfo
    view.findViewById<TextView>(R.id.passDescription)?.text = data.footer.description
    view.findViewById<TextView>(R.id.printTime)?.text = data.footer.printTime

    view.findViewById<ImageView>(R.id.passCutInHere)?.visibility =
        if (data.showCutLine) View.VISIBLE else View.GONE

    return view
}

private fun inflateGroupedLayout(inflater: LayoutInflater, data: PassData): View {
    val view = inflater.inflate(R.layout.printer_pass_grouped, null)

    view.findViewById<TextView>(R.id.headerTitle)?.text = data.header.title
    view.findViewById<TextView>(R.id.headerDate)?.text = data.header.date
    view.findViewById<TextView>(R.id.barcodeImageView)?.text = data.header.barcode

    val container = view.findViewById<LinearLayout>(R.id.itemsContainer)
    container?.removeAllViews()

    data.groupedData?.items?.forEach { item ->
        val itemView = inflater.inflate(R.layout.item_grouped_product, container, false)
        itemView.findViewById<TextView>(R.id.itemQuantity)?.text = "${item.quantity}x"
        itemView.findViewById<TextView>(R.id.itemName)?.text = item.name
        itemView.findViewById<TextView>(R.id.itemPrice)?.text = item.price
        container?.addView(itemView)
    }

    data.groupedData?.let {
        view.findViewById<TextView>(R.id.totalItems)?.text =
            "${it.totalItems} itens - ${it.totalPrice}"
    }

    view.findViewById<TextView>(R.id.cashierInfo)?.text =
        "Caixa: ${data.footer.menuName}\nOperador: ${data.footer.cashierName}"
    view.findViewById<TextView>(R.id.footerText)?.text = data.footer.description
    view.findViewById<TextView>(R.id.printTime)?.text = data.footer.printTime

    return view
}

private fun View.toBitmap(): Bitmap {
    measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    layout(0, 0, measuredWidth, measuredHeight)

    val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)

    return bitmap
}
