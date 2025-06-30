/*
 * Copyright (c) 2025 Ticpass. All rights reserved.
 *
 * PROPRIETARY AND CONFIDENTIAL
 *
 * This software is the confidential and proprietary information of Ticpass
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Ticpass.
 *
 * Unauthorized copying, distribution, or use of this software, via any medium,
 * is strictly prohibited without the express written permission of Ticpass.
 */

package br.com.ticpass.pos.acquirers.pagseguro.works

import android.util.Log
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.acquirers.AcquirerAdapter
import br.com.ticpass.pos.acquirers.AcquirerWorker
import br.com.ticpass.pos.acquirers.cash.works.CashPaymentWork
import br.com.ticpass.pos.acquirers.pagseguro.PagseguroProvider
import br.com.ticpass.pos.paymentProcessors.cash.CashProcessor
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import br.com.uol.pagseguro.plugpagservice.wrapper.listeners.PlugPagAbortListener
import br.com.uol.pagseguro.plugpagservice.wrapper.listeners.PlugPagPrintActionListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class PagseguroPaymentWork(
    override val provider: PlugPag,
    override var data: AcquirerAdapter.Payment.Data,
) : AcquirerWorker.Work<PlugPag, AcquirerAdapter.Payment.Data, Int> {

    override var succeeded: Boolean = false
    private val lock: Lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()

    override fun abort() {
        provider.asyncAbort(
            object : PlugPagAbortListener {
                override fun onAbortRequested(
                    abortRequested: Boolean
                ) {
                }

                override fun onError(errorMessage: String) {}

            }
        )
    }

    /* plugpag takes too fucking long to become available */
    private fun _waitForAvailability() {
        lock.lock()
        try {
            while (provider.isServiceBusy()) {
                // You can add a timeout or other conditions to prevent infinite waiting
                condition.await(300, TimeUnit.MILLISECONDS)
            }
        } finally {
            lock.unlock()
        }
    }

    private fun _setCustomerReceiptListener(
        listener: AcquirerWorker.QueueListener<AcquirerAdapter.Payment.Data, Int>
    ) {
        provider.setPrintActionListener(
            object : PlugPagPrintActionListener {
                override fun onError(exception: PlugPagException?) {
                    Log.wtf("actionListernet:onPrint", exception?.toString())
                }

                override fun onPrint(
                    phoneNumber: String?,
                    transactionResult: PlugPagTransactionResult?,
                    onFinishActions: PlugPagPrintActionListener.OnFinishPlugPagPrintActions?
                ) {
                    listener.onInquiry<Boolean>(
                        AcquirerWorker.Inquiry.Type.CUSTOMER_RECEIPT_PRINTING,
                        AcquirerWorker.QueueProgress(
                            data,
                            transactionResult?.result as Int
                        )
                    ) { granted ->
                        if (granted) {
                            onFinishActions?.doPrint(provider)
                        } else {
                            onFinishActions?.doNothing(provider)
                        }
                    }
                }
            }
        )
    }

    private fun _setPaymentListener(
        listener: AcquirerWorker.QueueListener<AcquirerAdapter.Payment.Data, Int>
    ) {
        val eventListener = object : PlugPagEventListener {
            override fun onEvent(eventData: PlugPagEventData) {
                listener.onProgress(
                    AcquirerWorker.QueueProgress(
                        data,
                        eventData.eventCode,
                    )
                )
            }
        }

        provider.setEventListener(eventListener)
    }

    private fun _resetPaymentListener() {
        val eventListener = object : PlugPagEventListener {
            override fun onEvent(eventData: PlugPagEventData) {}
        }

        provider.setEventListener(eventListener)
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

    private fun _doWork(listener: AcquirerWorker.QueueListener<AcquirerAdapter.Payment.Data, Int>) {
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
                val commission = (data.amount * data.commission) / 100L
                val plugPaymentData = PlugPagPaymentData(
                    PagseguroProvider.Payment.Type.fromKey(data.type).code,
                    commission.toInt() + data.amount.toInt(),
                    PlugPag.INSTALLMENT_TYPE_A_VISTA,
                    1,
                    data.id
                )

                _setCustomerReceiptListener(listener)
                _setPaymentListener(listener)

                val result = withContext(Dispatchers.Default) {
                    provider.doPayment(plugPaymentData)
                }

                if (result.result == PlugPag.RET_OK) {
                    data = data.copy(atk = result.transactionCode ?: "")

                    _resetPaymentListener()

                    listener.onDone(
                        AcquirerWorker.QueueProgress(
                            data,
                            PlugPag.RET_OK
                        )
                    )
                } else {
                    listener.onError(
                        AcquirerWorker.QueueError(
                            data,
                            status = result.message ?: ""
                        )
                    )
                }
            } catch (e: Exception) {
                MainActivity.logCrashException(e)
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