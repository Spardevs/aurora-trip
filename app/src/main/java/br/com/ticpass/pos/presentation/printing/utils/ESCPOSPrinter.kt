package br.com.ticpass.pos.presentation.printing.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.Socket
import java.io.IOException

class ESCPOSPrinter(private val host: String, private val port: Int = 9100) {

    private val tag = this.javaClass.simpleName
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    // ESC/POS Commands
    companion object {
        val INIT = byteArrayOf(0x1B.toByte(), 0x40.toByte())
        val CUT_FULL = byteArrayOf(0x1D.toByte(), 0x56.toByte(), 0x00.toByte())
        val CUT_PARTIAL = byteArrayOf(0x1D.toByte(), 0x56.toByte(), 0x01.toByte())
        val LINE_FEED = byteArrayOf(0x0A.toByte())
        val FORM_FEED = byteArrayOf(0x0C.toByte())

        // Image printing commands
        val SELECT_BIT_IMAGE_MODE = byteArrayOf(0x1B.toByte(), 0x2A.toByte())
        val SET_LINE_SPACING_24 = byteArrayOf(0x1B.toByte(), 0x33.toByte(), 0x18.toByte())
        val SET_LINE_SPACING_30 = byteArrayOf(0x1B.toByte(), 0x33.toByte(), 0x1E.toByte())
    }

    /**
     * Connect to the printer
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket(host, port)
            outputStream = socket?.getOutputStream()
            isConnected = true

            // Initialize printer
            sendCommand(INIT)

            Log.d(tag, "Connected to printer at $host:$port")
            true
        } catch (e: Exception) {
            Log.d(tag, "Connection failed: ${e.message}")
            isConnected = false
            false
        }
    }

    /**
     * Disconnect from the printer
     */
    fun disconnect() {
        try {
            outputStream?.flush()
            outputStream?.close()
            socket?.close()
            isConnected = false
            Log.d(tag, "Disconnected from printer")
        } catch (e: Exception) {
            Log.d(tag, "Error during disconnect: ${e.message}")
        } finally {
            socket = null
            outputStream = null
        }
    }

    /**
     * Send raw command to printer
     */
    private fun sendCommand(command: ByteArray) {
        if (!isConnected || outputStream == null) {
            throw IllegalStateException("Not connected to printer")
        }

        try {
            outputStream?.write(command)
            outputStream?.flush()
        } catch (e: IOException) {
            throw IOException("Failed to send command: ${e.message}")
        }
    }

    /**
     * Send text to printer
     */
    fun sendText(text: String) {
        if (!isConnected) {
            throw IllegalStateException("Not connected to printer")
        }

        try {
            outputStream?.write(text.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: IOException) {
            throw IOException("Failed to send text: ${e.message}")
        }
    }

    /**
     * Print bitmap image using ESC/POS raster commands
     */
    fun sendImage(bitmap: Bitmap) {
        if (!isConnected) {
            throw IllegalStateException("Not connected to printer")
        }

        // Convert bitmap to monochrome and get print data
        val imageData = convertBitmapToESCPOS(bitmap)

        try {
            // Set line spacing for image
            sendCommand(SET_LINE_SPACING_24)

            // Send image data
            outputStream?.write(imageData)
            outputStream?.flush()

            // Reset line spacing
            sendCommand(SET_LINE_SPACING_30)

        } catch (e: IOException) {
            throw IOException("Failed to send image: ${e.message}")
        }
    }

    /**
     * Cut paper
     * @param fullCut true for full cut, false for partial cut
     */
    fun cut(fullCut: Boolean = true) {
        val cutCommand = if (fullCut) CUT_FULL else CUT_PARTIAL
        sendCommand(cutCommand)
    }

    /**
     * Add line feed
     */
    fun lineFeed(lines: Int = 1) {
        repeat(lines) {
            sendCommand(LINE_FEED)
        }
    }

    /**
     * Convert Android Bitmap to ESC/POS image data
     * Uses 24-dot single-density mode
     */
    private fun convertBitmapToESCPOS(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height

        // ESC/POS printers typically have max width of 576 dots (72mm at 203dpi)
        val maxWidth = 576
        val scaledBitmap = if (width > maxWidth) {
            val scaleFactor = maxWidth.toFloat() / width.toFloat()
            val newHeight = (height * scaleFactor).toInt()
            Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
        } else {
            bitmap
        }

        val finalWidth = scaledBitmap.width
        val finalHeight = scaledBitmap.height

        // Convert to monochrome data
        val result = mutableListOf<Byte>()

        // Process image in 24-dot height strips
        var y = 0
        while (y < finalHeight) {
            val stripHeight = minOf(24, finalHeight - y)

            // ESC * m nL nH - Select bit image mode
            result.addAll(SELECT_BIT_IMAGE_MODE.toList())
            result.add(0x21) // 24-dot single-density mode

            // Width in bytes (low byte, high byte)
            val widthBytes = finalWidth
            result.add((widthBytes % 256).toByte())
            result.add((widthBytes / 256).toByte())

            // Image data for this strip
            for (x in 0 until finalWidth) {
                var byte1 = 0
                var byte2 = 0
                var byte3 = 0

                // Pack 24 vertical pixels into 3 bytes
                for (bit in 0 until 8) {
                    if (y + bit < finalHeight) {
                        val pixel = scaledBitmap.getPixel(x, y + bit)
                        if (shouldPrintPixel(pixel)) {
                            byte1 = byte1 or (1 shl (7 - bit))
                        }
                    }
                }

                for (bit in 0 until 8) {
                    if (y + bit + 8 < finalHeight) {
                        val pixel = scaledBitmap.getPixel(x, y + bit + 8)
                        if (shouldPrintPixel(pixel)) {
                            byte2 = byte2 or (1 shl (7 - bit))
                        }
                    }
                }

                for (bit in 0 until 8) {
                    if (y + bit + 16 < finalHeight) {
                        val pixel = scaledBitmap.getPixel(x, y + bit + 16)
                        if (shouldPrintPixel(pixel)) {
                            byte3 = byte3 or (1 shl (7 - bit))
                        }
                    }
                }

                result.add(byte1.toByte())
                result.add(byte2.toByte())
                result.add(byte3.toByte())
            }

            // Line feed after each strip
            result.addAll(LINE_FEED.toList())

            y += 24
        }

        return result.toByteArray()
    }

    /**
     * Determine if pixel should be printed (black pixels)
     * Uses luminance threshold
     */
    private fun shouldPrintPixel(pixel: Int): Boolean {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)

        // Calculate luminance
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()

        // Print if pixel is dark (threshold can be adjusted)
        return luminance < 128
    }
}