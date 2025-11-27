package br.com.ticpass.pos.data.device.repository

import br.com.ticpass.pos.data.device.remote.service.DeviceService
import br.com.ticpass.pos.data.device.remote.dto.RegisterDeviceRequest
import br.com.ticpass.pos.data.device.remote.dto.RegisterDeviceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val deviceService: DeviceService
) {
    suspend fun registerDevice(
        serial: String,
        acquirer: String,
        variant: String
    ): Response<RegisterDeviceResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterDeviceRequest(
                    serial = serial,
                    acquirer = acquirer,
                    variant = variant
                )

                deviceService.registerDevice(request)
            } catch (e: Exception) {
                throw IOException("Erro de rede ao registrar dispositivo", e)
            }
        }
    }
}