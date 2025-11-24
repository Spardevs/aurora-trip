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
import java.util.zip.CRC32
import br.com.ticpass.pos.R
import br.com.ticpass.pos.viewmodel.report.ReportData
import androidx.core.graphics.set
import com.journeyapps.barcodescanner.BarcodeResult
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

// Função para gerar QR Code
fun generateQRCodeBitmap(content: String, size: Int = 400): Bitmap {
    if (content.isBlank()) throw IllegalArgumentException("QR Code content cannot be empty")

    val writer = MultiFormatWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

    val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }

    return bitmap
}

/**
 * Gera código de barras automaticamente:
 * - Força CODE_128 quando o payload contém '|' ou qualquer caractere não numérico
 * - Para payloads numéricos adequados (12/13) usa EAN-13
 * - Fallback para CODE_128
 */
fun generateBarcodeBitmapAuto(payload: String, width: Int = 350, height: Int = (width / 2.5).toInt()): Bitmap {
    if (payload.isBlank()) throw IllegalArgumentException("Barcode cannot be empty")

    // Para compatibilidade, manter a função original mas usar QR Code como padrão
    return generateQRCodeBitmap(payload, width)
}

/**
 * Gera um token numérico determinístico de 12 dígitos a partir do paymentId (alfanumérico).
 * Usa CRC32 para derivar um valor 32-bit e mapeia para 12 dígitos.
 */
fun generate12DigitTokenFromPaymentId(paymentId: String): String {
    val crc = CRC32()
    crc.update(paymentId.toByteArray(Charsets.UTF_8))
    val v = crc.value and 0xFFFFL
    val num = (v % 1_000_000_000_000L).toString().padStart(12, '0')
    return num
}

/**
 * Converte paymentId em EAN-13 (13 dígitos) determinístico.
 * Também é responsável por gravar o mapeamento EAN13 -> paymentId em SharedPreferences
 * para posterior resolução no scanner.
 */
fun paymentIdToEan13AndSaveMapping(context: Context, paymentId: String): String {
    val token12 = generate12DigitTokenFromPaymentId(paymentId)
    val ean13 = calculateEAN13Checksum(token12) // retorna 13 dígitos

    try {
        val prefs = context.getSharedPreferences("BarcodeMappingPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("map_$ean13", paymentId).apply()
    } catch (e: Exception) {
        Timber.tag("BitmapUtils").w(e, "Falha ao salvar mapping EAN13 -> paymentId")
    }

    return ean13
}

/**
 * Decide o payload que será impresso no barcode (somente o token/EAN13 ou números já adequados).
 * Se o originalBarcode for um paymentId alfanumérico, gera EAN-13 curto e salva mapping.
 */
fun buildBarcodeForPrinting(context: Context, originalBarcode: String?): String {
    if (originalBarcode.isNullOrBlank()) return "0000" // fallback

    val numericOnly = originalBarcode.replace("[^0-9]".toRegex(), "")

    // Se já for um EAN-13 válido, usa direto
    if (numericOnly.length == 13 && isValidEAN13(numericOnly)) {
        return numericOnly
    }

    // Se for 12 dígitos numéricos, calcula checksum e usa
    if (numericOnly.length == 12) {
        return calculateEAN13Checksum(numericOnly)
    }

    // Caso comum: paymentId alfanumérico -> gerar token EAN13 e salvar mapeamento
    return paymentIdToEan13AndSaveMapping(context, originalBarcode)
}

fun savePassAsBitmap(context: Context, passData: PassData, atk: String? = null, transactionId: String? = null): File? {
    val configPrefs = context.getSharedPreferences("ConfigPrefs", Context.MODE_PRIVATE)
    val rawFormat = (configPrefs.getString("print_format", "DEFAULT") ?: "DEFAULT").uppercase()
    val printFormat = if (rawFormat == "DEFAULT") "EXPANDED" else rawFormat

    // Log do barcode usado para gerar o passe — útil para debug
    try {
        val payload = buildBarcodeForPrinting(context, passData.header.barcode)
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
    // Usa a função centralizada que gera um payload curto e grava mapping se necessário
    val payload = buildBarcodeForPrinting(inflater.context, data.header.barcode)
    val barcodeBitmap = try {
        Timber.tag("inflateProductLayout").d("Payload para QR Code: $payload")
        // Usar QR Code em vez de barcode tradicional
        generateQRCodeBitmap(payload)
    } catch (e: Exception) {
        Timber.tag("inflateProductLayout").e(e, "Erro ao gerar bitmap do QR Code: ${e.message}")
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
    val payload = buildBarcodeForPrinting(inflater.context, data.header.barcode)
    val barcodeBitmap = try {
        Timber.tag("inflateGroupedLayout").d("Payload para QR Code (grouped): $payload")
        // Usar QR Code em vez de barcode tradicional
        generateQRCodeBitmap(payload)
    } catch (e: Exception) {
        Timber.tag("inflateGroupedLayout").e(e, "Erro ao gerar bitmap do QR Code agrupado: ${e.message}")
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