package br.com.ticpass.pos.util

import android.text.Editable
import android.text.TextWatcher

/**
 * Utility class for Brazilian phone number validation and masking.
 */
object BrazilianPhoneUtils {

    /**
     * Validates if a Brazilian phone number is valid.
     * Supports both mobile (9 digits) and landline (8 digits) formats.
     * @param phone The phone string (with or without formatting)
     * @return true if valid, false otherwise
     */
    fun isValidBrazilianPhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        
        // Must have 10 digits (landline) or 11 digits (mobile)
        if (cleanPhone.length !in 10..11) return false
        
        // Area code must be between 11 and 99
        val areaCode = cleanPhone.substring(0, 2).toIntOrNull() ?: return false
        if (areaCode !in 11..99) return false
        
        if (cleanPhone.length == 11) {
            // Mobile number: must start with 9
            val firstDigit = cleanPhone[2].digitToInt()
            if (firstDigit != 9) return false
        }
        
        return true
    }

    /**
     * Formats a Brazilian phone number with the standard mask: (XX) 9 XXXX-XXXX or (XX) XXXX-XXXX
     * @param phone The phone string (digits only)
     * @return Formatted phone string
     */
    fun formatBrazilianPhone(phone: String): String {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        return when (cleanPhone.length) {
            0 -> ""
            1 -> "($cleanPhone"
            2 -> "($cleanPhone"
            3 -> "($cleanPhone"
            4 -> "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2)}"
            5 -> "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2)}"
            6 -> "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2)}"
            7 -> "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2, 3)} ${cleanPhone.substring(3)}"
            8 -> "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2, 3)} ${cleanPhone.substring(3)}"
            9 -> "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2, 3)} ${cleanPhone.substring(3, 7)}-${cleanPhone.substring(7)}"
            10 -> {
                // Landline: (XX) XXXX-XXXX
                "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2, 6)}-${cleanPhone.substring(6)}"
            }
            11 -> {
                // Mobile: (XX) 9 XXXX-XXXX
                "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2, 3)} ${cleanPhone.substring(3, 7)}-${cleanPhone.substring(7)}"
            }
            else -> {
                // Truncate to 11 digits max
                val truncated = cleanPhone.substring(0, 11)
                "(${truncated.substring(0, 2)}) ${truncated.substring(2, 3)} ${truncated.substring(3, 7)}-${truncated.substring(7)}"
            }
        }
    }

    /**
     * Removes formatting from phone number, keeping only digits.
     * @param phone The formatted phone string
     * @return Clean phone string with digits only
     */
    fun cleanPhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }

    /**
     * Creates a TextWatcher for Brazilian phone masking in EditText fields.
     * @return TextWatcher that applies Brazilian phone formatting
     */
    fun createBrazilianPhoneTextWatcher(): TextWatcher {
        return object : TextWatcher {
            private var isUpdating = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                
                isUpdating = true
                val formatted = formatBrazilianPhone(s.toString())
                s?.replace(0, s.length, formatted)
                isUpdating = false
            }
        }
    }
}
