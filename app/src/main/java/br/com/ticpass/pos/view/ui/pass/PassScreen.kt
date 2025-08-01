package br.com.ticpass.pos.view.ui.pass

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import br.com.ticpass.pos.R
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.serialization.Serializable
import androidx.core.graphics.createBitmap

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
    // Validate and clean input
    if (code.isBlank()) {
        throw IllegalArgumentException("Barcode cannot be empty")
    }

    val cleanCode = code.replace("[^0-9]".toRegex(), "")

    // Handle different length cases with more forgiving approach
    val finalCode = when {
        cleanCode.length == 12 -> {
            try {
                calculateEAN13Checksum(cleanCode)
            } catch (e: IllegalArgumentException) {
                // If we can't calculate checksum, try using it as-is
                cleanCode + "0" // Append dummy checksum
            }
        }
        cleanCode.length == 13 -> {
            if (!isValidEAN13(cleanCode)) {
                // For invalid checksums, regenerate with correct checksum
                calculateEAN13Checksum(cleanCode.substring(0, 12))
            } else {
                cleanCode
            }
        }
        else -> throw IllegalArgumentException("EAN-13 requires 12 or 13 digits")
    }

    val writer = MultiFormatWriter()
    val height: Int = (width / 4.0).toInt()

    try {
        val bitMatrix = writer.encode(finalCode, BarcodeFormat.EAN_13, width + 30, height)

        val barcodeBitmap = createBitmap(bitMatrix.width, height, Bitmap.Config.RGB_565)
        for (x in 0 until bitMatrix.width) {
            val column = IntArray(height) { if (bitMatrix[x, 0]) Color.BLACK else Color.WHITE }
            barcodeBitmap.setPixels(column, 0, 1, x, 0, 1, height)
        }

        val textWidth = (bitMatrix.width * 0.7).toInt()
        val textHeight = (height / 3.0).toInt()
        val textBitmap = createBitmap(textWidth, textHeight, Bitmap.Config.RGB_565)

        val canvas = Canvas(textBitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = (height / 2.8).toFloat()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Display the original code (with formatting) even if we fixed the checksum
        val formattedCode = splitString(code, 3)
        canvas.drawText(formattedCode, (textWidth / 2).toFloat(), (textHeight * 0.75).toFloat(), paint)

        val combinedHeight = height + textHeight
        val combinedBitmap = createBitmap(bitMatrix.width, combinedHeight, Bitmap.Config.RGB_565)
        val combinedCanvas = Canvas(combinedBitmap)
        combinedCanvas.drawColor(Color.WHITE)
        combinedCanvas.drawBitmap(barcodeBitmap, 0f, 0f, null)
        combinedCanvas.drawBitmap(textBitmap, ((bitMatrix.width - textWidth) / 2f), height.toFloat(), null)

        barcodeBitmap.recycle()
        textBitmap.recycle()

        return combinedBitmap
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to generate barcode for '$code': ${e.message}")
    }
}
fun splitString(input: String, chunkSize: Int): String {
    return input.chunked(chunkSize).joinToString(" ")
}

@Composable
fun PassScreen(
    passType: PassType,
    passList: List<PassData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        when (passType) {
            is PassType.ProductCompact,
            is PassType.ProductExpanded -> {
                val layoutRes = when (passType) {
                    is PassType.ProductCompact -> R.layout.printer_pass_compact
                    is PassType.ProductExpanded -> R.layout.printer_pass_expanded
                    else -> error("Invalid pass type for ProductPass")
                }

                passList.forEach { passData ->
                    ProductPass(
                        layoutRes = layoutRes,
                        passData = passData
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            is PassType.ProductGrouped -> {
                passList.firstOrNull()?.let { passData ->
                    GroupedPass(passData)
                }
            }
        }
    }
}

@Composable
private fun ProductPass(
    layoutRes: Int,
    passData: PassData,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            val view = LayoutInflater.from(context).inflate(layoutRes, null)
            val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
            val barcodeBitmap = generateEAN13BarcodeBitmap(passData.header.barcode)
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
        modifier = modifier.wrapContentSize()
    )
}

@Composable
private fun GroupedPass(
    passData: PassData,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            val view = LayoutInflater.from(context).inflate(R.layout.printer_pass_grouped, null)

            view.findViewById<TextView>(R.id.headerTitle)?.text = passData.header.title
            view.findViewById<TextView>(R.id.headerDate)?.text = passData.header.date

            val barcodeImage = view.findViewById<ImageView>(R.id.barcodeImageView)
            val barcodeBitmap = generateEAN13BarcodeBitmap(passData.header.barcode)
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
        modifier = modifier.wrapContentSize()
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

@Preview(showBackground = true)
@Composable
fun PassScreenPreviewListCompact() {
    val sampleList = listOf(
        PassData(
            header = PassData.HeaderData(
                title = "ticpass",
                date = "27/01/2024",
                barcode = "3154786720168"
            ),
            productData = PassData.ProductData(
                name = "Cerveja Brahma 160ml",
                price = "R$ 10,00",
                eventTitle = "Ticpass",
                eventTime = "31/07/2025 12:38"
            ),
            footer = PassData.FooterData(
                cashierName = "Gabriel",
                menuName = "caixa-001",
                printerInfo = "1/4",
                description = "Ficha válida por 15 dias...",
                printTime = "31/07/2025 12:38"
            ),
            showCutLine = true
        ),
        PassData(
            header = PassData.HeaderData(
                title = "ticpass",
                date = "27/01/2024",
                barcode = "3154786720168"
            ),
            productData = PassData.ProductData(
                name = "Refrigerante Guaraná",
                price = "R$ 6,00",
                eventTitle = "Ticpass",
                eventTime = "31/07/2025 12:40"
            ),
            footer = PassData.FooterData(
                cashierName = "Gabriel",
                menuName = "caixa-001",
                printerInfo = "2/4",
                description = "Ficha válida por 15 dias...",
                printTime = "31/07/2025 12:40"
            ),
            showCutLine = true
        )
    )

    PassScreen(
        passType = PassType.ProductCompact,
        passList = sampleList
    )
}

@Preview(showBackground = true)
@Composable
fun PassScreenPreviewListExpanded() {
    val sampleList = listOf(
        PassData(
            header = PassData.HeaderData(
                title = "ticpass",
                date = "27/01/2024",
                barcode = "3154786720168"
            ),
            productData = PassData.ProductData(
                name = "Spaten 600ml",
                price = "R$ 13,20",
                eventTitle = "Ticpass Festival",
                eventTime = "31/07/2025 13:42"
            ),
            footer = PassData.FooterData(
                cashierName = "Gabriel",
                menuName = "caixa-002",
                printerInfo = "1/2",
                description = "Válido até o fim do evento",
                printTime = "31/07/2025 13:42"
            ),
            showCutLine = true
        ),
        PassData(
            header = PassData.HeaderData(
                title = "ticpass",
                date = "27/01/2024",
                barcode = "315-4786720168"
            ),
            productData = PassData.ProductData(
                name = "Água Mineral",
                price = "R$ 4,00",
                eventTitle = "Ticpass Festival",
                eventTime = "31/07/2025 13:43"
            ),
            footer = PassData.FooterData(
                cashierName = "Gabriel",
                menuName = "caixa-002",
                printerInfo = "2/2",
                description = "Válido até o fim do evento",
                printTime = "31/07/2025 13:43"
            ),
            showCutLine = true
        )
    )

    PassScreen(
        passType = PassType.ProductExpanded,
        passList = sampleList
    )
}

@Preview(showBackground = true)
@Composable
fun PassScreenPreviewGrouped() {
    val groupedPass = PassData(
        header = PassData.HeaderData(
            title = "ticpass",
            date = "27/01/2024",
            barcode = "315-478-672-016-8"
        ),
        groupedData = PassData.GroupedData(
            items = listOf(
                PassData.GroupedData.GroupedItem(2, "CRÉDITO À VISTA N", "R$1,21"),
                PassData.GroupedData.GroupedItem(1, "SPATEN 600ML", "R$13,20")
            ),
            totalItems = 3,
            totalPrice = "R$14,41"
        ),
        footer = PassData.FooterData(
            cashierName = "teteteaTeste",
            menuName = "CAIXA-003",
            description = "FICHA VÁLIDA POR 15 DIAS APÓS A EMISSÃO.",
            printTime = "31/07/2025 12:42"
        ),
        showCutLine = true
    )

    PassScreen(
        passType = PassType.ProductGrouped,
        passList = listOf(groupedPass)
    )
}