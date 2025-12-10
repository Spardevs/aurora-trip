package br.com.ticpass.pos.core.payment.utils

import com.emv.qrcode.core.model.mpm.TagLengthString
import com.emv.qrcode.model.mpm.AdditionalDataField
import com.emv.qrcode.model.mpm.MerchantAccountInformationReservedAdditional
import com.emv.qrcode.model.mpm.MerchantAccountInformationTemplate
import com.emv.qrcode.model.mpm.MerchantPresentedMode
import br.com.ticpass.utils.toMoney
import br.com.ticpass.utils.toMoneyAsDouble
import com.emv.qrcode.model.mpm.AdditionalDataFieldTemplate
import java.text.Normalizer

/**
 * Utility class for generating PIX payment strings using mvallim EMV library v0.1.2
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
        // PIX specific constants
        private const val GUI_PIX = "BR.GOV.BCB.PIX"
        private const val MERCHANT_CATEGORY_CODE = "0000"
        private const val COUNTRY_CODE_BRAZIL = "BR"
        private const val CURRENCY_CODE_BRL = "986"
        private const val PAYLOAD_FORMAT_INDICATOR = "01"

        // EMV Tag IDs for PIX (used inside Merchant Account Information)
        private const val TAG_PIX_KEY = "01"
        private const val TAG_DESCRIPTION = "02"

        // Merchant Account Information tag for PIX
        private const val PIX_MERCHANT_ACCOUNT_TAG = "26"
    }

    /**
     * Generates a PIX code using mvallim EMV library v0.1.2
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
        val pixKeyTag = TagLengthString()
        val merchant = MerchantPresentedMode()
        val merchantData = MerchantAccountInformationReservedAdditional()
        val additionalTemplate = AdditionalDataFieldTemplate()
        val additionalData = AdditionalDataField()

        additionalData.setReferenceLabel(options.transactionId)
        additionalTemplate.value = additionalData

        // Create PIX Merchant Account Information
        pixKeyTag.tag = TAG_PIX_KEY
        pixKeyTag.value = pixKey.trim().replace(" ", "")

        merchantData.setGloballyUniqueIdentifier(GUI_PIX)
        merchantData.addPaymentNetworkSpecific(pixKeyTag)

        // Add description if provided
        options.description?.takeIf { it.isNotBlank() }?.let { description ->
            val descriptionTag = TagLengthString()
            descriptionTag.tag = TAG_DESCRIPTION
            descriptionTag.value = description
            merchantData.addPaymentNetworkSpecific(descriptionTag)
        }

        val merchantAccountInformation = MerchantAccountInformationTemplate(PIX_MERCHANT_ACCOUNT_TAG, merchantData)

        // Create Merchant Presented Mode
        merchant.setPayloadFormatIndicator(PAYLOAD_FORMAT_INDICATOR)
        merchant.addMerchantAccountInformation(merchantAccountInformation)
        merchant.setMerchantCategoryCode(MERCHANT_CATEGORY_CODE)
        merchant.setTransactionCurrency(CURRENCY_CODE_BRL)
        merchant.setTransactionAmount(amount.toMoneyAsDouble().toString())
        merchant.setCountryCode(COUNTRY_CODE_BRAZIL)
        merchant.setMerchantName(normalizeText(options.merchantName).take(25))
        merchant.setMerchantCity(normalizeText(options.merchantCity).take(15))

        merchant.additionalDataField = additionalTemplate

        // Set postal code if provided
        options.postalCode?.let { merchant.setPostalCode(it) }

        return merchant.toString()
    }

    /**
     * Normalizes text removing accents and special characters
     * Converts to uppercase and removes diacritics (accents)
     */
    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("[\u0300-\u036f]"), "") // Remove diacritics (accents)
            .uppercase()
            .trim()
    }

    // Formatting is now handled by MoneyUtils.formatAmountFromCents
}