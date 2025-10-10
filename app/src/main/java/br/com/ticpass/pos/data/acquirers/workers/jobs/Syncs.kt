package br.com.ticpass.pos.data.acquirers.workers.jobs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import android.webkit.URLUtil
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.data.event.ForYouViewModel
import br.com.ticpass.pos.data.room.AuthManager
import br.com.ticpass.pos.dataStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun syncPos(
    forYouViewModel: ForYouViewModel,
    onProgress: (Int) -> Unit,
    onFailure: (String) -> Unit,
    onDone: () -> Unit
) {
    try{
        val defaultScope = CoroutineScope(Dispatchers.Default)
        val handler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(handler) {

            val auth = withContext(Dispatchers.IO) {
                val authManager = AuthManager(MainActivity.appContext.dataStore)
                val jwt = authManager.getJwtToken()
                val cashierName = authManager.getCashierName()

                object {
                    val jwt = jwt
                    val cashierName = cashierName
                }
            }

            if(auth.jwt.isEmpty()) {
                onFailure("Token de autenticação é inválido.")
                return@launch
            }

            if(auth.cashierName.isBlank()) {
                onFailure("Nome do atendente é inválido.")
                return@launch
            }

            val data = withContext(Dispatchers.IO) {
                val event = forYouViewModel.eventRepository.getSelectedEvent()
                val pos = forYouViewModel.posRepository.getSelectedPos()
                val orders = forYouViewModel.orderRepository.getAllBySyncState(false)
                val payments = forYouViewModel.paymentRepository.getAllBySyncState(false)
                val cashups = forYouViewModel.cashupRepository.getAllBySyncState(false)
                val vouchers = forYouViewModel.voucherRepository.getAllBySyncState(false)
                val voucherRedemptions = forYouViewModel.voucherRedemptionRepository.getAllBySyncState(false)
                val refunds = forYouViewModel.refundRepository.getAllBySyncState(false)
                val acquisitions = forYouViewModel.acquisitionRepository.getAllBySyncState(false)
                val consumptions = forYouViewModel.consumptionRepository.getAllBySyncState(false)
                val passes = forYouViewModel.passRepository.getAllBySyncState(false)

                object {
                    val orders = orders
                    val payments = payments
                    val cashups = cashups
                    val vouchers = vouchers
                    val voucherRedemptions = voucherRedemptions
                    val refunds = refunds
                    val acquisitions = acquisitions
                    val consumptions = consumptions
                    val passes = passes
                    val event = event
                    val pos = pos

                    fun hasUnsynced(): Boolean {
                        val items = listOf(
                            orders,
                            payments,
                            cashups,
                            vouchers,
                            voucherRedemptions,
                            refunds,
                            acquisitions,
                            consumptions,
                            passes
                        )

                        return items.any { it.isNotEmpty() }
                    }
                }
            }

            if(!data.hasUnsynced()) {
                onDone()
                return@launch
            }

            val syncPos = withContext(Dispatchers.IO) {
                val result = forYouViewModel.apiRepository.syncPos(
                    data.event!!.id,
                    data.pos.id,
                    orders = data.orders,
                    payments = data.payments,
                    cashups = data.cashups,
                    vouchers = data.vouchers,
                    voucherRedemptions = data.voucherRedemptions,
                    refunds = data.refunds,
                    acquisitions = data.acquisitions,
                    consumptions = data.consumptions,
                    passes = data.passes,
                    auth.jwt as String,
                )

                result
            }

            if(syncPos.status != 201) {
                onFailure("Falha ao sincronizar os dados.")
                return@launch
            }

            val setManySynced = withContext(Dispatchers.IO) {
                forYouViewModel.orderRepository.setManySynced(
                    data.orders.map { it.id },
                    true
                )
                forYouViewModel.paymentRepository.setManySynced(
                    data.payments.map { it.id },
                    true
                )
                forYouViewModel.cashupRepository.setManySynced(
                    data.cashups.map { it.id },
                    true
                )
                forYouViewModel.voucherRepository.setManySynced(
                    data.vouchers.map { it.id },
                    true
                )
                forYouViewModel.voucherRedemptionRepository.setManySynced(
                    data.voucherRedemptions.map { it.id },
                    true
                )
                forYouViewModel.refundRepository.setManySynced(
                    data.refunds.map { it.id },
                    true
                )
                forYouViewModel.acquisitionRepository.setManySynced(
                    data.acquisitions.map { it.id },
                    true
                )
                forYouViewModel.consumptionRepository.setManySynced(
                    data.consumptions.map { it.id },
                    true
                )
                forYouViewModel.passRepository.setManySynced(
                    data.passes.map { it.id },
                    true
                )
            }

            onDone()
        }
    }
    catch (e: Exception) {
        onFailure(e.message.toString())
    }
}


suspend fun syncEvent(
    forYouViewModel: ForYouViewModel,
    downloadThumbnails: Boolean = true,
    onFailure: (cause: String) -> Unit = {},
    onSuccess: () -> Unit = {},
) {
    val context = MainActivity.appContext
    val authManager = AuthManager(context.dataStore)
    val selectedEvent = forYouViewModel.eventRepository.getSelectedEvent()
    val cashier = forYouViewModel.cashierRepository.getUser()
    val jwt = authManager.getJwtToken()

    try {

        /*
        * EVENT LIST
        * */
        val eventsResponse = cashier?.let {
            forYouViewModel.apiRepository.getEvents(
                it.id,
                jwt,
            )
        }

        if (eventsResponse?.status != 200 || eventsResponse.result.items.isEmpty()) {
            return
        }

        val event = eventsResponse?.result?.items?.find { it.id == selectedEvent!!.id }

        if (event != null) {
            var updatedEvent = selectedEvent

            if(downloadThumbnails) {
                val baseDir =
                    MainActivity.appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES) as File
                val imagesDir = File(baseDir, "events")
                imagesDir.mkdirs()

                downloadImages(
                    listOf(event.ticket),
                    imagesDir,
                )

                val fileName = URLUtil.guessFileName(event.ticket, null, null)
                val imagePath = "${imagesDir}/${fileName}"

                val file = File(imagePath)

                updatedEvent = updatedEvent!!.copy(
                    logo = file.absolutePath
                )
            }

            updatedEvent = updatedEvent!!.copy(
                name = event.name,
                dateStart = event.dateStart,
                dateEnd = event.dateEnd,
                details = event.details,
                printingPriceEnabled = event.isPrintTicket,
                pin = event.pin,
                mode = event.mode,

                isCreditEnabled = event.isCreditEnabled,
                isDebitEnabled = event.isDebitEnabled,
                isPIXEnabled = event.isPIXEnabled,
                isVREnabled = event.isVREnabled,
                isLnBTCEnabled = event.isLnBTCEnabled,
                isCashEnabled = event.isCashEnabled,
                isAcquirerPaymentEnabled = event.isAcquirerPaymentEnabled,
                isMultiPaymentEnabled = event.isMultiPaymentEnabled,
            )

            authManager.setAcquirerPaymentEnabled(event.isAcquirerPaymentEnabled)
            authManager.setMultiPaymentEnabled(event.isMultiPaymentEnabled)

            forYouViewModel.eventRepository.updateMany(
                listOf(
                    updatedEvent
                )
            )

            onSuccess()
        } else {
            onFailure("Evento não encontrado.")
        }
    } catch (e: Exception) {
        Log.d("syncEvent:error", e.toString())
        onFailure(e.message.toString())
    }
}

suspend fun downloadImages(
    urls: List<String>,
    dir: File,
): List<String> = coroutineScope {

    val deferredList = urls.map { url ->
        async(Dispatchers.IO) {
            val imageUrl = URL(url)
            val connection = imageUrl.openConnection()
            connection.doInput = true
            connection.connect()

            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val fileName = URLUtil.guessFileName(url, null, null)
            val file = File(dir, fileName)

            try {
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)

                fileOutputStream.close()
                inputStream.close()
                bitmap.recycle()

                System.gc()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            file.absolutePath
        }
    }

    deferredList.awaitAll()
}

