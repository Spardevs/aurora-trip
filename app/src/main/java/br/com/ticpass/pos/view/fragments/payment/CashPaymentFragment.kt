package br.com.ticpass.pos.view.fragments.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import br.com.ticpass.pos.R
import br.com.ticpass.pos.view.ui.pass.PassScreen
import br.com.ticpass.pos.view.ui.pass.PassType
import br.com.ticpass.pos.view.ui.pass.PassData
import br.com.ticpass.pos.data.acquirers.workers.BatchConfig
import br.com.ticpass.pos.data.acquirers.workers.ImageBatchWorkManager
import br.com.ticpass.pos.data.acquirers.workers.ImageData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CashPaymentFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_payment_cash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val composeView = view.findViewById<ComposeView>(R.id.passComposeView)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val passType = PassType.ProductCompact

        val barcodes = listOf(
            "5901234123457",
            "4006381333931",
            "7891234567892",
            "9780201379624"
        )

        val passList = listOf(
            PassData(
                header = PassData.HeaderData(
                    barcode = barcodes[0]
                ),
                productData = PassData.ProductData(
                    name = "Cerveja Brahma 160ml",
                    price = "R$ 10,00",
                    eventTitle = "Ticpass",
                    eventTime = "31/07/2025 12:38"
                ),
                footer = PassData.FooterData(
                    cashierName = "Gabriel Teste",
                    menuName = "caixa-003",
                    printerInfo = "1/4",
                    description = "Ficha Válida por 15 dias após a emissão...",
                    printTime = "31/07/2025 12:38"
                ),
                showCutLine = true
            ),
            PassData(
                header = PassData.HeaderData(
                    barcode = barcodes[1]  // Use valid EAN-13 barcode
                ),
                productData = PassData.ProductData(
                    name = "Cerveja Brahma 160ml",
                    price = "R$ 10,00",
                    eventTitle = "Ticpass",
                    eventTime = "31/07/2025 12:38"
                ),
                footer = PassData.FooterData(
                    cashierName = "Gabriel Teste",
                    menuName = "caixa-003",
                    printerInfo = "2/4",
                    description = "Ficha Válida por 15 dias após a emissão...",
                    printTime = "31/07/2025 12:38"
                ),
                showCutLine = true
            ),
            PassData(
                header = PassData.HeaderData(
                    barcode = barcodes[2]  // Use valid EAN-13 barcode
                ),
                productData = PassData.ProductData(
                    name = "Cerveja Brahma 160ml",
                    price = "R$ 10,00",
                    eventTitle = "Ticpass",
                    eventTime = "31/07/2025 12:38"
                ),
                footer = PassData.FooterData(
                    cashierName = "Gabriel Teste",
                    menuName = "caixa-003",
                    printerInfo = "3/4",
                    description = "Ficha Válida por 15 dias após a emissão...",
                    printTime = "31/07/2025 12:38"
                ),
                showCutLine = true
            ),
            PassData(
                header = PassData.HeaderData(
                    barcode = barcodes[3]  // Use valid EAN-13 barcode
                ),
                productData = PassData.ProductData(
                    name = "Cerveja Brahma 160ml",
                    price = "R$ 10,00",
                    eventTitle = "Ticpass",
                    eventTime = "31/07/2025 12:38"
                ),
                footer = PassData.FooterData(
                    cashierName = "Gabriel Teste",
                    menuName = "caixa-003",
                    printerInfo = "4/4",
                    description = "Ficha Válida por 15 dias após a emissão...",
                    printTime = "31/07/2025 12:38"
                ),
                showCutLine = true
            )
        )

        val images = passList.mapIndexed { index, pass ->
            val typeName = when (passType) {
                is PassType.ProductCompact -> "ProductCompact"
                is PassType.ProductExpanded -> "ProductExpanded"
                is PassType.ProductGrouped -> "ProductGrouped"
            }

            ImageData(
                id = "${System.currentTimeMillis()}_$index",
                filePath = "",
                fileName = "$typeName-pass-${index + 1}.png",
                metadata = mapOf(
                    "passData" to Json.encodeToString(pass),
                    "passType" to typeName,
                    "barcode" to pass.header.barcode  // Include the valid barcode in metadata
                )
            )
        }

        val imageBatchWorkManager = ImageBatchWorkManager(requireContext())

        val config = BatchConfig(
            batchSize = 2,
            maxConcurrency = 2,
            delayBetweenBatches = 100L
        )

        imageBatchWorkManager.processImages(images, config)

        composeView.setContent {
            PassScreen(
                passType = passType,
                passList = passList
            )
        }
    }
}