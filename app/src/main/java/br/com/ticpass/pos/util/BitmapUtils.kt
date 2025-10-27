package br.com.ticpass.pos.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.view.drawToBitmap
import br.com.ticpass.pos.view.ui.pass.PassData
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import br.com.ticpass.pos.R
import br.com.ticpass.pos.viewmodel.report.ReportData
import androidx.core.graphics.set
import com.journeyapps.barcodescanner.BarcodeResult
import timber.log.Timber

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

/**
 * Gera código de barras automaticamente:
 * - Força CODE_128 quando o payload contém '|' ou qualquer caractere não numérico
 * - Para payloads numéricos adequados (12/13) usa EAN-13
 * - Fallback para CODE_128
 */
fun generateBarcodeBitmapAuto(payload: String, width: Int = 350, height: Int = (width / 2.5).toInt()): Bitmap {
    if (payload.isBlank()) throw IllegalArgumentException("Barcode cannot be empty")

    val numeric = payload.replace("[^0-9]".toRegex(), "")

    // Se o payload contiver '|' (separador) ou qualquer caractere não numérico, forçar CODE_128
    if (payload.contains("|") || payload.any { !it.isDigit() }) {
        val writer = MultiFormatWriter()
        val matrix = writer.encode(payload, BarcodeFormat.CODE_128, width, height)
        val bmp = createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                bmp[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return bmp
    }

    // Caso totalmente numérico: decide entre EAN13 e CODE128 conforme tamanho
    if (numeric.length == 12 || (numeric.length == 13 && payload == numeric)) {
        return generateEAN13BarcodeBitmap(payload, width)
    }

    // Fallback: CODE_128
    val writer = MultiFormatWriter()
    val matrix = writer.encode(payload, BarcodeFormat.CODE_128, width, height)
    val bmp = createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bmp[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return bmp
}

/**
 * Monta o payload do barcode combinando o barcode original com ATK e Transaction ID quando disponíveis.
 * Exemplo de saída: "1234567890123|ATK:abcd|TX:tx123"
 */
private fun buildBarcodePayload(originalBarcode: String?, atk: String?, transactionId: String?): String {
    val parts = mutableListOf<String>()
    if (!originalBarcode.isNullOrBlank()) parts.add(originalBarcode)
    if (!atk.isNullOrBlank()) parts.add("ATK:$atk")
    if (!transactionId.isNullOrBlank()) parts.add("TX:$transactionId")
    return parts.joinToString("|")
}

fun savePassAsBitmap(context: Context, passData: PassData, atk: String? = null, transactionId: String? = null): File? {
    val configPrefs = context.getSharedPreferences("ConfigPrefs", Context.MODE_PRIVATE)
    val rawFormat = (configPrefs.getString("print_format", "DEFAULT") ?: "DEFAULT").uppercase()
    val printFormat = if (rawFormat == "DEFAULT") "EXPANDED" else rawFormat

    // Log do barcode usado para gerar o passe — útil para debug
    try {
        val payload = buildBarcodePayload(passData.header.barcode, atk, transactionId)
        Timber.tag("SavePassAsBitmap").d("Gerando passe com barcode payload: $payload")
    } catch (_: Exception) {}

    return try {
        val inflater = LayoutInflater.from(context)

        val layoutRes = when (printFormat) {
            "COMPACT" -> R.layout.printer_pass_compact
            "EXPANDED" -> R.layout.printer_pass_expanded
            "GROUPED" -> R.layout.printer_pass_grouped
            else -> R.layout.printer_pass_expanded
        }

        val view: View =
            if (layoutRes == R.layout.printer_pass_grouped) {
                inflateGroupedLayout(inflater, passData, atk, transactionId)
            } else {
                inflateProductLayout(inflater, layoutRes, passData, atk, transactionId)
            }

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = view.drawToBitmap()

        val outputDir = File(context.filesDir, printFormat).apply { mkdirs() }
        File(outputDir, "pass_${System.currentTimeMillis()}.png").apply {
            FileOutputStream(this).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    } catch (e: Exception) {
        Timber.tag("SavePassAsBitmap").e(e, "Erro ao salvar imagem: ${e.message}")
        null
    }
}

private fun inflateProductLayout(
    inflater: LayoutInflater,
    layoutRes: Int,
    data: PassData,
    atk: String?,
    transactionId: String?
): View {
    val view = inflater.inflate(layoutRes, null)

    val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
    val payload = buildBarcodePayload(data.header.barcode, atk, transactionId)
    val barcodeBitmap = try {
        Timber.tag("inflateProductLayout").d("Payload para barcode: $payload")
        generateBarcodeBitmapAuto(payload)
    } catch (e: Exception) {
        Timber.tag("inflateProductLayout").e(e, "Erro ao gerar bitmap do barcode: ${e.message}")
        null
    }
    barcodeImage?.setImageBitmap(barcodeBitmap)

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

@SuppressLint("MissingInflatedId")
private fun inflateGroupedLayout(inflater: LayoutInflater, data: PassData, atk: String?, transactionId: String?): View {
    val view = inflater.inflate(R.layout.printer_pass_grouped, null)

    view.findViewById<TextView>(R.id.headerTitle)?.text = data.header.title
    view.findViewById<TextView>(R.id.headerDate)?.text = data.header.date

    // Gera e seta o bitmap no ImageView do layout agrupado (corrigido)
    val payload = buildBarcodePayload(data.header.barcode, atk, transactionId)
    val barcodeBitmap = try {
        Timber.tag("inflateGroupedLayout").d("Payload para barcode (grouped): $payload")
        generateBarcodeBitmapAuto(payload)
    } catch (e: Exception) {
        Timber.tag("inflateGroupedLayout").e(e, "Erro ao gerar bitmap do barcode agrupado: ${e.message}")
        null
    }
    view.findViewById<ImageView>(R.id.barcodeImageView)?.setImageBitmap(barcodeBitmap)

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
        Timber.tag("ReportGenerator").e(e, "Erro ao gerar relatório")
        null
    }
}

/**
 * Valida e processa informações de um código de barras
 * @param barcodeResult Resultado do scan do barcode
 * @return BarcodeInfo se válido, null se inválido
 */
fun validateAndReadBarcode(barcodeResult: BarcodeResult?): BarcodeInfo? {
    // Verifica se o resultado não é nulo
    if (barcodeResult == null) {
        Timber.tag("BarcodeValidator").w("Resultado do barcode é nulo")
        return null
    }

    val text = barcodeResult.text
    val format = barcodeResult.barcodeFormat

    // Valida se o texto não está vazio
    if (text.isNullOrBlank()) {
        Timber.tag("BarcodeValidator").w("Texto do barcode está vazio")
        return null
    }

    // Valida o formato e comprimento do código
    val isValid = when (format) {
        BarcodeFormat.EAN_13 -> text.length == 13 && text.all { it.isDigit() } && validateEAN13(text)
        BarcodeFormat.EAN_8 -> text.length == 8 && text.all { it.isDigit() } && validateEAN8(text)
        BarcodeFormat.UPC_A -> text.length == 12 && text.all { it.isDigit() } && validateUPCA(text)
        BarcodeFormat.UPC_E -> text.length == 8 && text.all { it.isDigit() }
        BarcodeFormat.CODE_128 -> text.isNotEmpty()
        BarcodeFormat.CODE_39 -> text.isNotEmpty()
        BarcodeFormat.CODE_93 -> text.isNotEmpty()
        BarcodeFormat.ITF -> text.length % 2 == 0 && text.all { it.isDigit() }
        else -> false
    }

    if (!isValid) {
        Timber.tag("BarcodeValidator").w("Barcode inválido: formato=$format, texto=$text")
        return null
    }

    // Retorna informações do barcode validado
    return BarcodeInfo(
        text = text,
        format = format.toString(),
        timestamp = barcodeResult.timestamp,
        isValid = true
    )
}

/**
 * Valida checksum de código EAN-13
 */
private fun validateEAN13(code: String): Boolean {
    if (code.length != 13) return false

    val digits = code.map { it.toString().toInt() }
    val checksum = digits.last()

    val sum = digits.dropLast(1).mapIndexed { index, digit ->
        if (index % 2 == 0) digit else digit * 3
    }.sum()

    val calculatedChecksum = (10 - (sum % 10)) % 10
    return checksum == calculatedChecksum
}

/**
 * Valida checksum de código EAN-8
 */
private fun validateEAN8(code: String): Boolean {
    if (code.length != 8) return false

    val digits = code.map { it.toString().toInt() }
    val checksum = digits.last()

    val sum = digits.dropLast(1).mapIndexed { index, digit ->
        if (index % 2 == 0) digit * 3 else digit
    }.sum()

    val calculatedChecksum = (10 - (sum % 10)) % 10
    return checksum == calculatedChecksum
}

/**
 * Valida checksum de código UPC-A
 */
private fun validateUPCA(code: String): Boolean {
    if (code.length != 12) return false

    val digits = code.map { it.toString().toInt() }
    val checksum = digits.last()

    val sum = digits.dropLast(1).mapIndexed { index, digit ->
        if (index % 2 == 0) digit * 3 else digit
    }.sum()

    val calculatedChecksum = (10 - (sum % 10)) % 10
    return checksum == calculatedChecksum
}

/**
 * Classe de dados para informações do barcode
 */
data class BarcodeInfo(
    val text: String,
    val format: String,
    val timestamp: Long,
    val isValid: Boolean,
    val metadata: Map<String, String> = emptyMap()
)