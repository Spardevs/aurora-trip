package br.com.ticpass.pos.compose.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
fun generateObjectId(): String {
    val timestamp = Instant.now().epochSecond.toString(16)
    val randomValue = (0..1000).random().toString(16)
    val counter = (0..1000).random().toString(16)

    return "$timestamp$randomValue$counter"
}