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

package br.com.ticpass.pos.acquirers.pagseguro.workers

import android.util.Log
import br.com.ticpass.pos.acquirers.AcquirerAdapter
import br.com.ticpass.pos.acquirers.AcquirerWorker
import br.com.ticpass.pos.acquirers.pagseguro.PagseguroProvider
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrinterData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PagseguroPrintingWorker(
    override val provider: PlugPag,
    override val data: AcquirerAdapter.Printing.Data,
) : AcquirerWorker.Work<PlugPag, AcquirerAdapter.Printing.Data, Int> {

    override var succeeded: Boolean = false

    override fun abort() {}

    override fun doWork(listener: AcquirerWorker.QueueListener<AcquirerAdapter.Printing.Data, Int>) {

        val defaultScope = CoroutineScope(Dispatchers.Default)
        val handler = CoroutineExceptionHandler { _, _ -> }

        defaultScope.launch(handler) {
            try {
                withContext(Dispatchers.Default) {
                    listener.onProgress(
                        AcquirerWorker.QueueProgress(
                            data,
                            PagseguroProvider.Printing.Status.STARTING.code
                        )
                    )
                }

                withContext(Dispatchers.Default) {
                    listener.onProgress(
                        AcquirerWorker.QueueProgress(
                            data,
                            PagseguroProvider.Printing.Status.PRINTING.code
                        )
                    )
                }

                val printResult = withContext(Dispatchers.Default) {
                    provider.printFromFile(
                        PlugPagPrinterData(
                            filePath = data.filePath,
                            4,
                            0
                        )
                    )
                }

                val isError = PagseguroProvider.Printing.Error.isError(printResult.result)
                val isDone = PagseguroProvider.Printing.Status.isDone(printResult.result)

                if(isError) {
                    val error = PagseguroProvider.Printing.Error.fromCode(printResult.result)

                    return@launch listener.onError(
                        AcquirerWorker.QueueError(
                            data = data,
                            status = error.message
                        )
                    )
                }

                return@launch listener.onDone(
                    AcquirerWorker.QueueProgress(
                        data,
                        printResult.result
                    )
                )
            }
            catch (e: Exception) {
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