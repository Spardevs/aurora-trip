package br.com.ticpass.pos.core.queue.processors.printing.processors.models

data class PrinterNetworkInfo(
    val ipAddress: String,
    val port: Int
)