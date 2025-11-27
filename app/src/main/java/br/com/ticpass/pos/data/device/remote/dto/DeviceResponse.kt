package br.com.ticpass.pos.data.device.remote.dto

data class RegisterDeviceRequest(
    val serial: String,
    val acquirer: String,
    val variant: String
)

data class RegisterDeviceResponse(
    val id: String,
    val serial: String,
    val acquirer: String,
    val variant: String,
    val stoneCodes: List<Any>,
    val createdAt: String,
    val updatedAt: String
)