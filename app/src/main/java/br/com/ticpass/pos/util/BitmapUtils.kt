package br.com.ticpass.pos.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.view.drawToBitmap
import br.com.ticpass.pos.view.ui.pass.PassData
import br.com.ticpass.pos.view.ui.pass.PassType
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import br.com.ticpass.pos.R
import br.com.ticpass.pos.viewmodel.report.ReportData


fun calculateEAN13Checksum(code: String): String {
    val cleanCode = code.replace("[^0-9]".toRegex(), "")
    require(cleanCode.length == 12) { "EAN-13 requires 12 digits to calculate checksum" }

    val digits = cleanCode.map { it.toString().toInt() }
    var sum = 0
    for (i in digits.indices) {
        sum += if (i % 2 == 0) digits[i] * 1 else digits[i] * 3
    }
    val checksum = (10 - (sum % 10)) % 10
    return cleanCode + checksum
}

fun isValidEAN13(code: String): Boolean {
    val cleanCode = code.replace("[^0-9]".toRegex(), "")
    if (cleanCode.length != 13) return false

    val digits = cleanCode.map { it.toString().toInt() }
    val checksum = digits.last()

    var sum = 0
    for (i in 0..11) {
        val digit = digits[i]
        sum += if (i % 2 == 0) digit * 1 else digit * 3
    }
    val calculatedChecksum = (10 - (sum % 10)) % 10

    return checksum == calculatedChecksum
}

fun generateEAN13BarcodeBitmap(code: String, width: Int = 350): Bitmap {
    if (code.isBlank()) throw IllegalArgumentException("Barcode cannot be empty")

    val cleanCode = code.replace("[^0-9]".toRegex(), "")
    val finalCode = when {
        cleanCode.length == 12 -> calculateEAN13Checksum(cleanCode)
        cleanCode.length == 13 -> if (!isValidEAN13(cleanCode)) calculateEAN13Checksum(cleanCode.substring(0, 12)) else cleanCode
        else -> throw IllegalArgumentException("EAN-13 requires 12 or 13 digits")
    }

    val writer = MultiFormatWriter()
    val height: Int = (width / 4.0).toInt()

    val bitMatrix = writer.encode(finalCode, BarcodeFormat.EAN_13, width + 30, height)

    val barcodeBitmap = createBitmap(bitMatrix.width, height, Bitmap.Config.RGB_565)
    for (x in 0 until bitMatrix.width) {
        val column = IntArray(height) { if (bitMatrix[x, 0]) Color.BLACK else Color.WHITE }
        barcodeBitmap.setPixels(column, 0, 1, x, 0, 1, height)
    }

    return barcodeBitmap
}

fun savePassAsBitmap(context: Context, passType: PassType, passData: PassData): File? {
    return try {
        val inflater = LayoutInflater.from(context)
        val view: View = when (passType) {
            is PassType.ProductCompact ->
                inflateProductLayout(inflater, R.layout.printer_pass_compact, passData)
            is PassType.ProductExpanded ->
                inflateProductLayout(inflater, R.layout.printer_pass_expanded, passData)
            is PassType.ProductGrouped ->
                inflateGroupedLayout(inflater, passData)
        }

        // Ensure proper layout before capturing
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = view.drawToBitmap()

        val passTypeName = when (passType) {
            is PassType.ProductCompact -> "ProductCompact"
            is PassType.ProductExpanded -> "ProductExpanded"
            is PassType.ProductGrouped -> "ProductGrouped"
        }

        // Create directory with pass type name and save the image inside
        val outputDir = File(context.filesDir, passTypeName).apply { mkdirs() }
        Log.d("SavePassAsBitmap", "Diretório criado: ${outputDir.absolutePath}")

        File(outputDir, "pass_${System.currentTimeMillis()}.png").apply {
            FileOutputStream(this).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("SavePassAsBitmap", "Imagem salva em: ${this.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("SavePassAsBitmap", "Erro ao salvar imagem: ${e.message}", e)
        e.printStackTrace()
        null
    }
}
private fun inflateProductLayout(inflater: LayoutInflater, layoutRes: Int, data: PassData): View {
    val view = inflater.inflate(layoutRes, null)

    val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
    val barcodeBitmap = try {
        generateEAN13BarcodeBitmap(data.header.barcode)
    } catch (e: Exception) {
        null
    }
    barcodeImage?.setImageBitmap(barcodeBitmap)

    // Preencher dados do produto
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

    // Header
    view.findViewById<TextView>(R.id.headerTitle)?.text = data.header.title
    view.findViewById<TextView>(R.id.headerDate)?.text = data.header.date

    // Barcode
    val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
    val barcodeBitmap = try {
        generateEAN13BarcodeBitmap(data.header.barcode)
    } catch (e: Exception) {
        null
    }
    barcodeImage?.setImageBitmap(barcodeBitmap)

    // Lista de itens agrupados
    val container = view.findViewById<LinearLayout>(R.id.itemsContainer)
    container?.removeAllViews()

    data.groupedData?.items?.forEach { item ->
        val itemView = inflater.inflate(R.layout.item_grouped_product, container, false)
        itemView.findViewById<TextView>(R.id.itemQuantity)?.text = "${item.quantity}x"
        itemView.findViewById<TextView>(R.id.itemName)?.text = item.name
        itemView.findViewById<TextView>(R.id.itemPrice)?.text = item.price
        container?.addView(itemView)
    }

    // Total de itens
    data.groupedData?.let {
        view.findViewById<TextView>(R.id.totalItems)?.text =
            "${it.totalItems} itens - ${it.totalPrice}"
    }

    // Footer
    view.findViewById<TextView>(R.id.cashierInfo)?.text =
        "Caixa: ${data.footer.menuName}\nOperador: ${data.footer.cashierName}"
    view.findViewById<TextView>(R.id.footerText)?.text = data.footer.description
    view.findViewById<TextView>(R.id.printTime)?.text = data.footer.printTime

    return view
}

fun splitString(input: String, chunkSize: Int): String {
    return input.chunked(chunkSize).joinToString(" ")
}

fun saveVoucherAsBitmap(context: Context): File? {
    return try {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.printer_voucher, null)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = view.drawToBitmap()

        val outputDir = File(context.filesDir, "Vouchers").apply { mkdirs() }
        File(outputDir, "voucher_${System.currentTimeMillis()}.png").apply {
            FileOutputStream(this).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveRefundAsBitmap(context: Context): File? {
    return try {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.printer_refund, null)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = view.drawToBitmap()

        val outputDir = File(context.filesDir, "Refunds").apply { mkdirs() }
        File(outputDir, "refund_${System.currentTimeMillis()}.png").apply {
            FileOutputStream(this).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun rememberBitmapImage(bitmap: Bitmap?): ImageBitmap? {
    return remember(bitmap) {
        bitmap?.asImageBitmap()
    }
}

@SuppressLint("InflateParams")
fun saveReportAsBitmap(context: Context, reportData: ReportData): File? {
    return try {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.printer_report, null)

        // Preencher cabeçalho
        view.findViewById<TextView>(R.id.eventTitle).text = reportData.eventTitle
        view.findViewById<TextView>(R.id.eventDate).text = reportData.eventDate

        // Preencher valor total
        view.findViewById<TextView>(R.id.totalAmount).text = reportData.totalAmount

        // Preencher entradas
        view.findViewById<TextView>(R.id.cashAmount).text = reportData.cashAmount
        view.findViewById<TextView>(R.id.bitcoinAmount).text = reportData.bitcoinAmount
        view.findViewById<TextView>(R.id.debitAmount).text = reportData.debitAmount
        view.findViewById<TextView>(R.id.creditAmount).text = reportData.creditAmount
        view.findViewById<TextView>(R.id.pixAmount).text = reportData.pixAmount
        view.findViewById<TextView>(R.id.mealVoucherAmount).text = reportData.mealVoucherAmount
        view.findViewById<TextView>(R.id.totalInAmount).text = reportData.totalInAmount

        // Preencher saídas
        view.findViewById<TextView>(R.id.refundAmount).text = reportData.refundAmount
        view.findViewById<TextView>(R.id.withdrawalAmount).text = reportData.withdrawalAmount
        view.findViewById<TextView>(R.id.totalOutAmount).text = reportData.totalOutAmount

        // Preencher produtos
        view.findViewById<TextView>(R.id.productDescription).text = reportData.productDescription
        view.findViewById<TextView>(R.id.productUnitPrice).text = reportData.productUnitPrice
        view.findViewById<TextView>(R.id.productTotal).text = reportData.productTotal

        // Preencher miscelânea
        view.findViewById<TextView>(R.id.serialNumber).text = reportData.serialNumber
        view.findViewById<TextView>(R.id.cashierName).text = reportData.cashierName
        view.findViewById<TextView>(R.id.operatorName).text = reportData.operatorName
        view.findViewById<TextView>(R.id.commissionAmount).text = reportData.commissionAmount
        view.findViewById<TextView>(R.id.openingTime).text = reportData.openingTime
        view.findViewById<TextView>(R.id.reprintedTickets).text = reportData.reprintedTickets
        view.findViewById<TextView>(R.id.reprintedAmount).text = reportData.reprintedAmount

        // Medir e layout da view
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Criar bitmap
        val bitmap = createBitmap(view.measuredWidth, view.measuredHeight).apply {
            val canvas = Canvas(this)
            canvas.drawColor(Color.WHITE)
            view.draw(canvas)
        }

        // Salvar o arquivo
        val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Reports").apply {
            mkdirs()
        }
        File(outputDir, "report_${System.currentTimeMillis()}.png").apply {
            FileOutputStream(this).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            return this
        }
    } catch (e: Exception) {
        Log.e("ReportGenerator", "Erro ao gerar relatório", e)
        null
    }
}
