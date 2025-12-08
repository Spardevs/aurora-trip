package br.com.ticpass.pos.util

import android.text.Editable
import android.text.TextWatcher
import android.util.Log

/**
 * Utility class for CPF validation and masking.
 */
object CpfUtils {

    /**
     * Validates if a CPF is valid according to Brazilian rules.
     * @param cpf The CPF string (with or without formatting)
     * @return true if valid, false otherwise
     */
    fun isValidCpf(cpf: String): Boolean {
        val cleanCpf = cpf.replace(Regex("[^0-9]"), "")

        Log.i("CpfUtils", "Validating CPF: $cleanCpf")
        
        // Check if has 11 digits
        if (cleanCpf.length != 11) return false
        
        // Check if all digits are the same (invalid CPF)
        if (cleanCpf.all { it == cleanCpf[0] }) return false
        
        // Calculate first verification digit
        var sum = 0
        for (i in 0..8) {
            sum += cleanCpf[i].digitToInt() * (10 - i)
        }
        var remainder = sum % 11
        val firstDigit = if (remainder < 2) 0 else 11 - remainder
        
        if (cleanCpf[9].digitToInt() != firstDigit) return false
        
        // Calculate second verification digit
        sum = 0
        for (i in 0..9) {
            sum += cleanCpf[i].digitToInt() * (11 - i)
        }
        remainder = sum % 11
        val secondDigit = if (remainder < 2) 0 else 11 - remainder
        
        return cleanCpf[10].digitToInt() == secondDigit
    }

    /**
     * Formats a CPF string with the standard mask: XXX.XXX.XXX-XX
     * @param cpf The CPF string (digits only)
     * @return Formatted CPF string
     */
    fun formatCpf(cpf: String): String {
        val cleanCpf = cpf.replace(Regex("[^0-9]"), "")
        Log.d("CpfUtils", "Formatting CPF: $cleanCpf")
        return when (cleanCpf.length) {
            0 -> ""
            in 1..3 -> cleanCpf
            in 4..6 -> "${cleanCpf.substring(0, 3)}.${cleanCpf.substring(3)}"
            in 7..9 -> "${cleanCpf.substring(0, 3)}.${cleanCpf.substring(3, 6)}.${cleanCpf.substring(6)}"
            in 10..11 -> "${cleanCpf.substring(0, 3)}.${cleanCpf.substring(3, 6)}.${cleanCpf.substring(6, 9)}-${cleanCpf.substring(9)}"
            else -> "${cleanCpf.substring(0, 3)}.${cleanCpf.substring(3, 6)}.${cleanCpf.substring(6, 9)}-${cleanCpf.substring(9, 11)}"
        }
    }

    /**
     * Removes formatting from CPF, keeping only digits.
     * @param cpf The formatted CPF string
     * @return Clean CPF string with digits only
     */
    fun cleanCpf(cpf: String): String {
        return cpf.replace(Regex("[^0-9]"), "")
    }

    /**
     * Creates a TextWatcher for CPF masking in EditText fields.
     * @return TextWatcher that applies CPF formatting
     */
    fun createCpfTextWatcher(): TextWatcher {
        return object : TextWatcher {
            private var isUpdating = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                
                isUpdating = true
                val formatted = formatCpf(s.toString())
                Log.i("CpfUtils", "Formatted CPF: $formatted")
                s?.replace(0, s.length, formatted)
                isUpdating = false
            }
        }
    }
}
