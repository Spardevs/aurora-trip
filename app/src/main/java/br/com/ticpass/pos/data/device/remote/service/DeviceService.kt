package br.com.ticpass.pos.data.device.remote.service

import br.com.ticpass.pos.data.device.remote.dto.RegisterDeviceRequest
import br.com.ticpass.pos.data.device.remote.dto.RegisterDeviceResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceService {
    @POST("devices")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<RegisterDeviceResponse>
}