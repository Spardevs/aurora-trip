package br.com.ticpass.pos.queue.payment.processors

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.Normalizer
import java.util.*

/**
 * Utility class for generating PIX payment strings
 * Follows the Brazilian PIX standard for static and dynamic QR codes
 * Based on the specification by Bacen: https://www.bcb.gov.br/content/estabilidadefinanceira/forumpireunioes/AnexoI-PadroesParaIniciacaodoPix.pdf
 */

/**
 * Data class for additional PIX options
 */
data class PixOptions(
    val description: String? = null,
    val merchantName: String = "PAGAMENTO",
    val merchantCity: String = "SAO PAULO",
    val postalCode: String? = null,
    val transactionId: String = "***"
)
class PixCodeGenerator {

    companion object {
        // IDs dos campos conforme especificação EMV
        private const val ID_PAYLOAD_FORMAT_INDICATOR = "00"
        private const val ID_POINT_OF_INITIATION_METHOD = "01"
        private const val ID_MERCHANT_ACCOUNT_INFORMATION = "26"
        private const val ID_MERCHANT_CATEGORY_CODE = "52"
        private const val ID_TRANSACTION_CURRENCY = "53"
        private const val ID_TRANSACTION_AMOUNT = "54"
        private const val ID_COUNTRY_CODE = "58"
        private const val ID_MERCHANT_NAME = "59"
        private const val ID_MERCHANT_CITY = "60"
        private const val ID_POSTAL_CODE = "61"
        private const val ID_ADDITIONAL_DATA_FIELD = "62"
        private const val ID_CRC16 = "63"

        // Sub-IDs para Merchant Account Information (campo 26)
        private const val ID_GUI = "00"
        private const val ID_PIX_KEY = "01"
        private const val ID_DESCRIPTION = "02"

        // Sub-IDs para Additional Data Field (campo 62)
        private const val ID_REFERENCE_LABEL = "05"

        // Valores fixos
        private const val PAYLOAD_FORMAT_INDICATOR = "01"
        private const val POINT_OF_INITIATION_METHOD = "12" // 12 = reutilizável
        private const val MERCHANT_CATEGORY_CODE = "0000"
        private const val TRANSACTION_CURRENCY = "986" // 986 = BRL (Real Brasileiro)
        private const val COUNTRY_CODE = "BR"
        private const val GUI = "BR.GOV.BCB.PIX" // Identificador do PIX
    }

    /**
     * Generates a PIX code with maximum compatibility
     * @param pixKey PIX key (CPF, CNPJ, phone, email or random key)
     * @param amount Transaction amount in cents
     * @param options Additional options for the PIX code (description, merchant name, city, etc.)
     * @return PIX code for copy and paste
     */
    fun generate(
        pixKey: String,
        amount: Int,
        options: PixOptions = PixOptions()
    ): String {
        // Validação básica
        if (pixKey.isBlank()) {
            throw IllegalArgumentException("PIX key cannot be empty")
        }
        
        // Validação do CEP se fornecido
        options.postalCode?.let {
            if (it.length != 8) {
                throw IllegalArgumentException("Postal code must have 8 characters")
            }
        }
        
        // Validação do ID de transação
        if (options.transactionId.length > 25) {
            throw IllegalArgumentException("Transaction ID must have at most 25 characters")
        }
        
        val payloadList = mutableListOf<String>()
        
        // 00 - Payload Format Indicator
        payloadList.add(buildTLV(ID_PAYLOAD_FORMAT_INDICATOR, PAYLOAD_FORMAT_INDICATOR))
        
        // 01 - Point of Initiation Method (apenas para QR codes dinâmicos)
        // Não adicionamos este campo pois o amount é obrigatório
        
        // 26 - Merchant Account Information
        payloadList.add(buildTLV(ID_MERCHANT_ACCOUNT_INFORMATION, generateKeyInfo(pixKey, options.description)))
        
        // 52 - Merchant Category Code
        payloadList.add(buildTLV(ID_MERCHANT_CATEGORY_CODE, MERCHANT_CATEGORY_CODE))
        
        // 53 - Transaction Currency
        payloadList.add(buildTLV(ID_TRANSACTION_CURRENCY, TRANSACTION_CURRENCY))
        
        // 54 - Transaction Amount
        if (amount > 0) {
            payloadList.add(buildTLV(ID_TRANSACTION_AMOUNT, formatAmountFromCents(amount)))
        } else {
            throw IllegalArgumentException("Amount must be greater than zero")
        }
        
        // 58 - Country Code
        payloadList.add(buildTLV(ID_COUNTRY_CODE, COUNTRY_CODE))
        
        // 59 - Merchant Name (normalizado e limitado a 25 caracteres)
        val normalizedName = normalizeText(options.merchantName).take(25)
        payloadList.add(buildTLV(ID_MERCHANT_NAME, normalizedName))
        
        // 60 - Merchant City (normalizado e limitado a 15 caracteres)
        val normalizedCity = normalizeText(options.merchantCity).take(15)
        payloadList.add(buildTLV(ID_MERCHANT_CITY, normalizedCity))
        
        // 61 - Postal Code (opcional)
        options.postalCode?.let {
            payloadList.add(buildTLV(ID_POSTAL_CODE, it))
        }
        
        // 62 - Additional Data Field com Transaction ID
        payloadList.add(buildTLV(ID_ADDITIONAL_DATA_FIELD, buildTLV(ID_REFERENCE_LABEL, options.transactionId)))
        
        // 63 - CRC16 (será calculado e adicionado)
        payloadList.add("6304")
        
        val payloadString = payloadList.joinToString("")
        val crc = calculateCRC16(payloadString)
        
        return payloadString.substring(0, payloadString.length - 4) + crc
    }

    /**
     * Gera as informações da chave PIX para o campo Merchant Account Information (26)
     */
    private fun generateKeyInfo(pixKey: String, description: String?): String {
        val keyInfo = mutableListOf<String>()
        
        // 00 - GUI (Identificador PIX do Banco Central)
        keyInfo.add(buildTLV(ID_GUI, GUI))
        
        // 01 - PIX Key (removendo espaços)
        val cleanKey = pixKey.trim().replace(" ", "")
        keyInfo.add(buildTLV(ID_PIX_KEY, cleanKey))
        
        // 02 - Description (opcional)
        description?.let {
            if (it.isNotBlank()) {
                keyInfo.add(buildTLV(ID_DESCRIPTION, it))
            }
        }
        
        return keyInfo.joinToString("")
    }

    /**
     * Normaliza texto removendo acentos e caracteres especiais
     * Converte para maiúsculas e remove caracteres diacríticos (acentos)
     */
    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("[\u0300-\u036f]"), "") // Remove diacríticos (acentos)
            .uppercase()
            .trim()
    }

    /**
     * Constrói um campo TLV (Tag-Length-Value)
     */
    private fun buildTLV(id: String, value: String): String {
        val length = value.length.toString().padStart(2, '0')
        return "$id$length$value"
    }

    /**
     * Formata o valor monetário a partir de centavos
     */
    private fun formatAmountFromCents(amountInCents: Int): String {
        val amount = amountInCents / 100.0
        return formatAmount(amount)
    }

    /**
     * Formata o valor monetário conforme especificação
     */
    private fun formatAmount(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#0.00", symbols)
        return formatter.format(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP))
    }

    /**
     * Calcula o CRC16 usando algoritmo CCITT conforme especificação EMV
     */
    private fun calculateCRC16(data: String): String {
        val bytes = data.toByteArray(Charsets.UTF_8)
        var crc = 0xFFFF
        val polynomial = 0x1021

        for (byte in bytes) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            for (bit in 0 until 8) {
                if ((crc and 0x8000) != 0) {
                    crc = (crc shl 1) xor polynomial
                } else {
                    crc = crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }

        return String.format("%04X", crc)
    }
}