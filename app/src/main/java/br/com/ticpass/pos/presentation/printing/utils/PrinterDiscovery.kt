import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap

data class PrinterInfo(
    val ipAddress: String,
    val port: Int,
    val manufacturer: String? = null,
    val model: String? = null,
    val isESCPOSSupported: Boolean = false,
    val responseTime: Long = 0,
    val capabilities: List<String> = emptyList()
)

class PrinterDiscovery(private val context: Context) {

    companion object {
        // Common printer ports (ordered by likelihood)
        val COMMON_PORTS = listOf(9100, 515, 631, 8080, 8000, 9101, 9102)
        val FAST_PORTS = listOf(9100, 9101, 9102) // Most likely ESC/POS ports

        // ESC/POS Status Commands
        val STATUS_INQUIRY = byteArrayOf(0x10.toByte(), 0x04.toByte(), 0x01.toByte()) // DLE EOT n
        val PRINTER_ID_INQUIRY = byteArrayOf(0x1D.toByte(), 0x49.toByte(), 0x01.toByte()) // GS I n

        // Timeouts - Standard
        const val CONNECTION_TIMEOUT = 2000 // 2 seconds
        const val READ_TIMEOUT = 1000 // 1 second

        // Timeouts - Fast mode
        const val FAST_CONNECTION_TIMEOUT = 500 // 0.5 seconds
        const val FAST_READ_TIMEOUT = 300 // 0.3 seconds

        // Timeouts - Ultra-fast mode
        const val ULTRA_FAST_TIMEOUT = 200 // 0.2 seconds
    }

    private val discoveredPrinters = ConcurrentHashMap<String, PrinterInfo>()

    /**
     * Ultra-fast discovery - Only port 9100 on all network IPs
     * Fastest method: assumes port 9100 connection = printer
     */
    suspend fun ultraFastDiscover(
        progressCallback: ((String) -> Unit)? = null
    ): List<PrinterInfo> = withContext(Dispatchers.IO) {

        discoveredPrinters.clear()

        try {
            val networkInfo = getNetworkInfo()
            if (networkInfo == null) {
                progressCallback?.invoke("❌ No network connection")
                return@withContext emptyList()
            }

            val ipRange = generateIPRange(networkInfo)
            progressCallback?.invoke("🚀 Ultra-fast scan: ${ipRange.size} IPs on port 9100")

            // Scan all IPs in parallel, only port 9100
            val jobs = ipRange.map { ip ->
                async {
                    ultraFastScanIP(ip, progressCallback)
                }
            }

            jobs.awaitAll()

            val results = discoveredPrinters.values.toList().sortedBy { it.ipAddress }
            progressCallback?.invoke("⚡ Ultra-fast completed: ${results.size} printers found")

            results

        } catch (e: Exception) {
            progressCallback?.invoke("❌ Ultra-fast scan failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fast discovery - Essential ports on all network IPs
     * Good balance: checks most likely printer ports with short timeouts
     */
    suspend fun fastDiscover(
        progressCallback: ((String) -> Unit)? = null
    ): List<PrinterInfo> = withContext(Dispatchers.IO) {

        discoveredPrinters.clear()

        try {
            val networkInfo = getNetworkInfo()
            if (networkInfo == null) {
                progressCallback?.invoke("❌ No network connection")
                return@withContext emptyList()
            }

            val ipRange = generateIPRange(networkInfo)
            progressCallback?.invoke("🚀 Fast scan: ${ipRange.size} IPs on ${FAST_PORTS.size} ports")

            // Scan all IPs in parallel, fast ports only
            val jobs = ipRange.map { ip ->
                async {
                    fastScanIP(ip, progressCallback)
                }
            }

            jobs.awaitAll()

            val results = discoveredPrinters.values.toList().sortedBy { it.ipAddress }
            progressCallback?.invoke("⚡ Fast scan completed: ${results.size} printers found")

            results

        } catch (e: Exception) {
            progressCallback?.invoke("❌ Fast scan failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Full discovery - All ports on all network IPs
     * Most thorough: complete port and protocol validation
     */
    suspend fun discover(
        progressCallback: ((String) -> Unit)? = null
    ): List<PrinterInfo> = withContext(Dispatchers.IO) {

        discoveredPrinters.clear()

        try {
            val networkInfo = getNetworkInfo()
            if (networkInfo == null) {
                progressCallback?.invoke("❌ No network connection")
                return@withContext emptyList()
            }

            val ipRange = generateIPRange(networkInfo)
            progressCallback?.invoke("🔍 Full scan: ${ipRange.size} IPs on ${COMMON_PORTS.size} ports")

            // Scan all IPs in parallel, all common ports
            val jobs = ipRange.map { ip ->
                async {
                    fullScanIP(ip, progressCallback)
                }
            }

            jobs.awaitAll()

            val results = discoveredPrinters.values.toList().sortedBy { it.ipAddress }
            progressCallback?.invoke("✅ Full scan completed: ${results.size} printers found")

            results

        } catch (e: Exception) {
            progressCallback?.invoke("❌ Full scan failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get actual network information from WiFi connection
     */
    private fun getNetworkInfo(): NetworkInfo? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        // Get device's current IP
        val ipAddress = wifiInfo.ipAddress
        val deviceIP = String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )

        // Get subnet mask to determine network range
        val dhcpInfo = wifiManager.dhcpInfo
        val subnetMask = dhcpInfo.netmask

        // Calculate network address and prefix length
        val networkAddress = calculateNetworkAddress(ipAddress, subnetMask)
        val prefixLength = calculatePrefixLength(subnetMask)

        return NetworkInfo(
            deviceIP = deviceIP,
            networkAddress = networkAddress,
            subnetMask = subnetMask,
            prefixLength = prefixLength
        )
    }

    data class NetworkInfo(
        val deviceIP: String,
        val networkAddress: String,
        val subnetMask: Int,
        val prefixLength: Int
    )

    /**
     * Calculate network address from IP and subnet mask
     */
    private fun calculateNetworkAddress(ip: Int, subnetMask: Int): String {
        val networkInt = ip and subnetMask
        return String.format(
            "%d.%d.%d.%d",
            networkInt and 0xff,
            networkInt shr 8 and 0xff,
            networkInt shr 16 and 0xff,
            networkInt shr 24 and 0xff
        )
    }

    /**
     * Calculate prefix length from subnet mask
     */
    private fun calculatePrefixLength(subnetMask: Int): Int {
        return Integer.bitCount(subnetMask)
    }

    /**
     * Generate actual IP range based on network info
     */
    private fun generateIPRange(networkInfo: NetworkInfo): List<String> {
        val parts = networkInfo.networkAddress.split(".")
        val baseIP = "${parts[0]}.${parts[1]}.${parts[2]}"

        // For most home/office networks (/24), scan 1-254
        // Skip our own device IP to avoid issues
        val deviceLastOctet = networkInfo.deviceIP.split(".").last().toInt()

        return (1..254)
            .filter { it != deviceLastOctet } // Skip our own IP
            .map { "$baseIP.$it" }
    }

    /**
     * Ultra-fast scan: Single IP, port 9100 only, minimal timeout
     */
    private suspend fun ultraFastScanIP(
        ipAddress: String,
        progressCallback: ((String) -> Unit)?
    ) = withContext(Dispatchers.IO) {

        try {
            val startTime = System.currentTimeMillis()
            val socket = Socket()

            withTimeout(ULTRA_FAST_TIMEOUT.toLong()) {
                socket.connect(InetSocketAddress(ipAddress, 9100), ULTRA_FAST_TIMEOUT)
            }

            val responseTime = System.currentTimeMillis() - startTime
            socket.close()

            // If connection succeeds on 9100, assume it's a printer
            val printerInfo = PrinterInfo(
                ipAddress = ipAddress,
                port = 9100,
                manufacturer = "Unknown",
                model = "ESC/POS Printer (Ultra-Fast)",
                isESCPOSSupported = true,
                responseTime = responseTime,
                capabilities = listOf("Port 9100 Response")
            )

            discoveredPrinters[ipAddress] = printerInfo
            progressCallback?.invoke("🚀 $ipAddress:9100")

        } catch (e: Exception) {
            // No response on port 9100
        }
    }

    /**
     * Fast scan: Single IP, essential ports, short timeout
     */
    private suspend fun fastScanIP(
        ipAddress: String,
        progressCallback: ((String) -> Unit)?
    ) = withContext(Dispatchers.IO) {

        val jobs = FAST_PORTS.map { port ->
            async {
                fastScanPort(ipAddress, port, progressCallback)
            }
        }

        jobs.awaitAll()
    }

    /**
     * Full scan: Single IP, all ports, complete validation
     */
    private suspend fun fullScanIP(
        ipAddress: String,
        progressCallback: ((String) -> Unit)?
    ) = withContext(Dispatchers.IO) {

        val jobs = COMMON_PORTS.map { port ->
            async {
                fullScanPort(ipAddress, port, progressCallback)
            }
        }

        jobs.awaitAll()
    }

    /**
     * Fast scan specific port with minimal ESC/POS validation
     */
    private suspend fun fastScanPort(
        ipAddress: String,
        port: Int,
        progressCallback: ((String) -> Unit)?
    ): PrinterInfo? = withContext(Dispatchers.IO) {

        try {
            val startTime = System.currentTimeMillis()
            val socket = Socket()
            socket.soTimeout = FAST_READ_TIMEOUT

            withTimeout(FAST_CONNECTION_TIMEOUT.toLong()) {
                socket.connect(InetSocketAddress(ipAddress, port), FAST_CONNECTION_TIMEOUT)
            }

            val responseTime = System.currentTimeMillis() - startTime

            // Quick validation
            val printerInfo = quickESCPOSTest(socket, ipAddress, port, responseTime)
            socket.close()

            if (printerInfo != null) {
                discoveredPrinters["$ipAddress:$port"] = printerInfo
                progressCallback?.invoke("⚡ $ipAddress:$port")
                return@withContext printerInfo
            }

        } catch (e: Exception) {
            // Port not responding
        }

        null
    }

    /**
     * Full scan specific port with complete ESC/POS validation
     */
    private suspend fun fullScanPort(
        ipAddress: String,
        port: Int,
        progressCallback: ((String) -> Unit)?
    ): PrinterInfo? = withContext(Dispatchers.IO) {

        try {
            val startTime = System.currentTimeMillis()
            val socket = Socket()
            socket.soTimeout = READ_TIMEOUT

            withTimeout(CONNECTION_TIMEOUT.toLong()) {
                socket.connect(InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT)
            }

            val responseTime = System.currentTimeMillis() - startTime

            // Complete validation
            val printerInfo = testESCPOSPrinter(socket, ipAddress, port, responseTime)
            socket.close()

            if (printerInfo != null) {
                discoveredPrinters["$ipAddress:$port"] = printerInfo
                progressCallback?.invoke("✅ $ipAddress:$port - ${printerInfo.model ?: "Unknown"}")
                return@withContext printerInfo
            }

        } catch (e: Exception) {
            // Port not responding
        }

        null
    }

    /**
     * Quick ESC/POS test for fast scanning
     */
    private suspend fun quickESCPOSTest(
        socket: Socket,
        ipAddress: String,
        port: Int,
        responseTime: Long
    ): PrinterInfo? = withContext(Dispatchers.IO) {

        try {
            // Port 9100 is standard ESC/POS - assume it's a printer
            if (port == 9100) {
                return@withContext PrinterInfo(
                    ipAddress = ipAddress,
                    port = port,
                    manufacturer = "Unknown",
                    model = "ESC/POS Printer (Fast)",
                    isESCPOSSupported = true,
                    responseTime = responseTime,
                    capabilities = listOf("ESC/POS Port", "Fast Discovery")
                )
            }

            // For other ports, try quick status check
            val outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()

            outputStream.write(STATUS_INQUIRY)
            outputStream.flush()
            delay(200) // Short wait

            if (inputStream.available() > 0) {
                return@withContext PrinterInfo(
                    ipAddress = ipAddress,
                    port = port,
                    manufacturer = "Unknown",
                    model = "Thermal Printer (Fast)",
                    isESCPOSSupported = true,
                    responseTime = responseTime,
                    capabilities = listOf("Status Response", "Fast Discovery")
                )
            }

        } catch (e: Exception) {
            // Not responding to ESC/POS
        }

        null
    }

    /**
     * Complete ESC/POS printer test with identification
     */
    private suspend fun testESCPOSPrinter(
        socket: Socket,
        ipAddress: String,
        port: Int,
        responseTime: Long
    ): PrinterInfo? = withContext(Dispatchers.IO) {

        try {
            val outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()

            // Send status inquiry
            outputStream.write(STATUS_INQUIRY)
            outputStream.flush()
            delay(500)

            var hasResponse = false
            val response = ByteArray(1024)
            var bytesRead = 0

            if (inputStream.available() > 0) {
                bytesRead = inputStream.read(response)
                hasResponse = true
            }

            // Try to get printer identification
            var model: String? = null
            var manufacturer: String? = null

            try {
                outputStream.write(PRINTER_ID_INQUIRY)
                outputStream.flush()
                delay(500)

                if (inputStream.available() > 0) {
                    val idResponse = ByteArray(256)
                    val idBytesRead = inputStream.read(idResponse)
                    if (idBytesRead > 0) {
                        val idString = String(idResponse, 0, idBytesRead).trim()
                        val parts = idString.split(",", ";", " ")
                        if (parts.isNotEmpty()) {
                            manufacturer = parts.getOrNull(0)
                            model = parts.getOrNull(1) ?: parts.getOrNull(0)
                        }
                    }
                }
            } catch (e: Exception) {
                // ID inquiry failed
            }

            // Accept if we got response or it's standard printer port
            if (hasResponse || port == 9100) {
                return@withContext PrinterInfo(
                    ipAddress = ipAddress,
                    port = port,
                    manufacturer = manufacturer ?: detectManufacturer(model),
                    model = model ?: "ESC/POS Printer",
                    isESCPOSSupported = true,
                    responseTime = responseTime,
                    capabilities = detectCapabilities(response, bytesRead)
                )
            }

        } catch (e: Exception) {
            // Not an ESC/POS printer
        }

        null
    }

    /**
     * Detect manufacturer from model string
     */
    private fun detectManufacturer(model: String?): String? {
        if (model == null) return null

        return when {
            model.contains("epson", ignoreCase = true) -> "Epson"
            model.contains("star", ignoreCase = true) -> "Star"
            model.contains("citizen", ignoreCase = true) -> "Citizen"
            model.contains("elgin", ignoreCase = true) -> "Elgin"
            model.contains("bematech", ignoreCase = true) -> "Bematech"
            model.contains("zebra", ignoreCase = true) -> "Zebra"
            else -> null
        }
    }

    /**
     * Detect printer capabilities from response
     */
    private fun detectCapabilities(response: ByteArray, length: Int): List<String> {
        val capabilities = mutableListOf<String>()

        if (length > 0) {
            capabilities.add("Status Response")

            if (length >= 1) {
                val status = response[0].toInt()
                if (status and 0x08 == 0) capabilities.add("Paper Present")
                if (status and 0x20 == 0) capabilities.add("Cover Closed")
            }
        }

        capabilities.addAll(listOf(
            "ESC/POS Commands",
            "Text Printing",
            "Paper Cut"
        ))

        return capabilities
    }

    /**
     * Test specific printer IP and port
     */
    suspend fun testPrinter(ipAddress: String, port: Int = 9100): PrinterInfo? =
        withContext(Dispatchers.IO) {
            fullScanPort(ipAddress, port, null)
        }
}