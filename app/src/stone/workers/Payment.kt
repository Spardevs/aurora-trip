/*           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
*                   Version 2, December 2004
*
* Copyright (C) 2004 Satoshi Nakamoto <satoshi@bitcoin.org>
*
* Everyone is permitted to copy and distribute verbatim or modified
* copies of this license document, and changing it is allowed as long
* as the name is changed.
*
*            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
*   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
*
*  0. You just DO WHAT THE FUCK YOU WANT TO.
*/

package br.com.ticpass.pos.acquirers.stone.works

import android.util.Log
import br.com.stone.posandroid.providers.PosPrintReceiptProvider
import br.com.stone.posandroid.providers.PosTransactionProvider
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.acquirers.AcquirerAdapter
import br.com.ticpass.pos.acquirers.AcquirerWorker
import br.com.ticpass.pos.acquirers.cash.works.CashPaymentWork
import br.com.ticpass.pos.acquirers.stone.StoneProvider
import br.com.ticpass.pos.paymentProcessors.cash.CashProcessor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stone.application.enums.Action
import stone.application.enums.ErrorsEnum
import stone.application.enums.InstalmentTransactionEnum
import stone.application.enums.ReceiptType
import stone.application.enums.TransactionStatusEnum
import stone.application.interfaces.StoneActionCallback
import stone.application.interfaces.StoneCallbackInterface
import stone.database.transaction.TransactionObject
import stone.utils.Stone
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class StonePaymentWork(
    override val provider: Any?,
    override var data: AcquirerAdapter.Payment.Data,
) : AcquirerWorker.Work<Any?, AcquirerAdapter.Payment.Data, Int> {

    override var succeeded: Boolean = false
    var isCancelled: AtomicBoolean = AtomicBoolean(false)

    private var _transProvider: PosTransactionProvider? = null

    private suspend fun _printCustomerReceipt(
        transaction: TransactionObject,
        onDone: () -> Unit
    ) {
        val customer =
            PosPrintReceiptProvider(
                MainActivity.appContext,
                transaction,
                ReceiptType.CLIENT
            )


        val defaultScope = CoroutineScope(Dispatchers.Default)
        val handler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(handler) {
            withContext(Dispatchers.IO) {
                customer.connectionCallback = object : StoneCallbackInterface {
                    override fun onSuccess() {
                        onDone()
                    }

                    override fun onError() {
                        Log.d("foobarbizbetprint:error", "printing:customer:receipt")
                    }
                }

                customer.execute()
            }
        }
    }

    override fun abort() {
        if (isCancelled.getAndSet(true)) return
        _transProvider?.abortPayment()
    }

    override fun doWork(listener: AcquirerWorker.QueueListener<AcquirerAdapter.Payment.Data, Int>) {
        if(data.confirmationRequired) {
            listener.onInquiry<AcquirerAdapter.Payment.Data>(
                AcquirerWorker.Inquiry.Type.PAYMENT_CONFIRMATION,
                AcquirerWorker.QueueProgress(
                    data,
                    0
                )
            ) { modifiedData ->
                data = modifiedData
                _doWork(listener)
            }
        }
        else {
            _doWork(listener)
        }
    }

    fun _doWork(listener: AcquirerWorker.QueueListener<AcquirerAdapter.Payment.Data, Int>) {
        val defaultScope = CoroutineScope(Dispatchers.Default)
        val handler = CoroutineExceptionHandler { _, _ -> }

        if(data.type == AcquirerAdapter.Payment.Type.CASH) {

//            data = data.copy(type = AcquirerAdapter.Payment.Type.CASH)

            val bar = CashProcessor()
            val foo = CashPaymentWork(bar, data)

            foo.doWork(listener)
            return
        }

        defaultScope.launch(handler) {
            try {
                isCancelled.set(false)

                val commission = (data.amount * data.commission) / 100L
                var isDone = false
                val transaction = TransactionObject()
                transaction.typeOfTransaction = StoneProvider.Payment.Type.fromKey(data.type).code
                transaction.instalmentTransaction = InstalmentTransactionEnum.ONE_INSTALMENT
                transaction.amount = (commission.toInt() + data.amount.toInt()).toString()
                transaction.isCapture = true
                transaction.externalId = data.id

                _transProvider = PosTransactionProvider(
                    MainActivity.appContext,
                    transaction,
                    Stone.getUserModel(0)
                )

                _transProvider?.setConnectionCallback(object : StoneActionCallback {
                    override fun onSuccess() {
                        isDone = true

                        if (data.qrcode != null) {
                            data.qrcode!!.recycle()
                            System.gc()
                        }

                        data = data.copy(atk = transaction.acquirerTransactionKey)

                        try {
                            val defaultScope = CoroutineScope(Dispatchers.Default)
                            val handler = CoroutineExceptionHandler { _, _ -> }

                            defaultScope.launch(handler) {
                                if (_transProvider?.listOfErrors?.isNotEmpty() == true) {
                                    val foo = withContext(Dispatchers.IO) {
                                        val error = _transProvider?.listOfErrors?.last()
                                            ?: ErrorsEnum.UNKNOWN_ERROR

                                        listener.onError(
                                            AcquirerWorker.QueueError(
                                                data,
                                                StoneProvider.Payment.Error.fromCode(error).message,
                                            )
                                        )

                                        return@withContext
                                    }

                                    return@launch
                                }

                                val progressAdapterEnum = StoneProvider
                                    .ActionCodeAdapter
                                    .fromEnum(
                                        transaction.transactionStatus ?: TransactionStatusEnum.UNKNOWN
                                    )

                                if (progressAdapterEnum.isError) {
                                    withContext(Dispatchers.IO) {
                                        listener.onError(
                                            AcquirerWorker.QueueError(
                                                data,
                                                progressAdapterEnum.message
                                            )
                                        )

                                        return@withContext
                                    }

                                    return@launch
                                }

                                withContext(Dispatchers.IO) {
                                    listener.onProgress(
                                        AcquirerWorker.QueueProgress(
                                            data,
                                            progressAdapterEnum.code
                                        )
                                    )
                                }

                                val granted = withContext(Dispatchers.Default) {
                                    suspendCoroutine { continuation ->
                                        listener.onInquiry<Boolean>(
                                            AcquirerWorker.Inquiry.Type.CUSTOMER_RECEIPT_PRINTING,
                                            AcquirerWorker.QueueProgress(
                                                data,
                                                progressAdapterEnum.code
                                            )
                                        ) { granted ->
                                            continuation.resume(granted)
                                        }
                                    }
                                }

                                withContext(Dispatchers.IO) {
                                    if (!granted) {
                                        listener.onDone(
                                            AcquirerWorker.QueueProgress(
                                                data,
                                                progressAdapterEnum.code
                                            )
                                        )

                                        return@withContext
                                    }

                                    _printCustomerReceipt(transaction) {
                                        listener.onDone(
                                            AcquirerWorker.QueueProgress(
                                                data,
                                                progressAdapterEnum.code
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            MainActivity.logCrashException(e)
                        }
                    }

                    override fun onStatusChanged(action: Action) {
                        if (isDone) return

                        val progressAdapterEnum = StoneProvider
                            .TransactionAdapter
                            .fromAction(action)

                        if (action == Action.TRANSACTION_WAITING_QRCODE_SCAN) {
                            data = data.copy(qrcode = transaction.qrCode)

                            listener.onProgress(
                                AcquirerWorker.QueueProgress(
                                    data,
                                    progressAdapterEnum.code
                                )
                            )

                            return
                        }

                        if (progressAdapterEnum.isError) {
                            listener.onError(
                                AcquirerWorker.QueueError(
                                    data,
                                    progressAdapterEnum.message,
                                )
                            )

                            return
                        }

                        listener.onProgress(
                            AcquirerWorker.QueueProgress(
                                data,
                                progressAdapterEnum.code
                            )
                        )
                    }

                    override fun onError() {
                        if (isDone) return

                        val error = _transProvider?.listOfErrors?.last() ?: ErrorsEnum.UNKNOWN_ERROR
                        val isInteralError = error == ErrorsEnum.INTERNAL_ERROR
                        if (isInteralError) return

                        if (data.qrcode != null) {
                            data.qrcode!!.recycle()
                            System.gc()
                        }

                        listener.onError(
                            AcquirerWorker.QueueError(
                                data,
                                StoneProvider.Payment.Error.fromCode(error).message
                            )
                        )
                    }
                })
                _transProvider?.execute()
            } catch (e: Exception) {
                MainActivity.logCrashException(e)

                if (data.qrcode != null) {
                    data.qrcode!!.recycle()
                    System.gc()
                }

                listener.onError(
                    AcquirerWorker.QueueError(
                        data,
                        status = e.toString()
                    )
                )
            }

        }
    }
}