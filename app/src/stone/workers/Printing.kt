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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import br.com.stone.posandroid.providers.PosPrintProvider
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.acquirers.AcquirerAdapter
import br.com.ticpass.pos.acquirers.AcquirerWorker
import br.com.ticpass.pos.acquirers.stone.StoneProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import stone.application.enums.Action
import stone.application.enums.ErrorsEnum
import stone.application.interfaces.StoneActionCallback

class StonePrintingWorker(
    override val provider: Any?,
    override val data: AcquirerAdapter.Printing.Data,
) : AcquirerWorker.Work<Any?, AcquirerAdapter.Printing.Data, Int> {

    override var succeeded: Boolean = false

    private var _printProvider: PosPrintProvider? = null

    override fun abort() {}

    private fun _bitmapFromPath(filePath: String): Bitmap {
        return BitmapFactory.decodeFile(filePath)
    }

    private fun _splitBitmapIntoParts(bitmap: Bitmap, xHeight: Int): List<Bitmap> = runBlocking {
        val desiredWidth = bitmap.width
        val desiredHeight = bitmap.height
        val numParts = Math.ceil(desiredHeight.toDouble() / xHeight.toDouble()).toInt()

        val deferredBitmapParts = mutableListOf<Deferred<Bitmap>>()

        for (i in 0 until numParts) {
            val startY = i * xHeight
            val endY = ((i + 1) * xHeight).coerceAtMost(desiredHeight)

            val deferredPart = async(Dispatchers.Default) {
                Bitmap.createBitmap(bitmap, 0, startY, desiredWidth, endY - startY)
            }

            deferredBitmapParts.add(deferredPart)
        }

        val bitmapParts = deferredBitmapParts.awaitAll()
        bitmapParts
    }

    private fun processBitmapParts(filePath: String): List<Bitmap> {

        val bitmap = _bitmapFromPath(filePath)
        val desiredWidth = 380 // Set your desired width here
        val scaleRatio = desiredWidth.toFloat() / bitmap.width.toFloat()
        val desiredHeight = (bitmap.height * scaleRatio).toInt()

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, desiredWidth, desiredHeight, false)

        val bitmapParts = _splitBitmapIntoParts(resizedBitmap, 595)
        bitmap.recycle()

        System.gc()

        return bitmapParts
    }

    override fun doWork(listener: AcquirerWorker.QueueListener<AcquirerAdapter.Printing.Data, Int>) {
        try {
            val defaultScope = CoroutineScope(Dispatchers.Default)
            val handler = CoroutineExceptionHandler { _, _ -> }

            defaultScope.launch(handler) {
                val bitmapParts = withContext(Dispatchers.IO) {
                    processBitmapParts(data.filePath)
                }

                listener.onProgress(
                    AcquirerWorker.QueueProgress(
                        data,
                        StoneProvider.Printing.Status.STARTING.code
                    )
                )

                listener.onProgress(
                    AcquirerWorker.QueueProgress(
                        data,
                        StoneProvider.Printing.Status.PRINTING.code
                    )
                )

                _printProvider = PosPrintProvider(MainActivity.appContext)
                _printProvider?.setConnectionCallback(
                    object : StoneActionCallback {
                        override fun onStatusChanged(action: Action?) {
                            listener.onProgress(
                                AcquirerWorker.QueueProgress(
                                    data = data,
                                    status = StoneProvider.Printing.Status.PRINTING.code
                                )
                            )
                        }

                        override fun onSuccess() {
                            bitmapParts.forEach { part -> part.recycle() }

                            System.gc()

                            listener.onDone(
                                AcquirerWorker.QueueProgress(
                                    data = data,
                                    status = StoneProvider.Printing.Status.SUCCESS.code
                                )
                            )
                        }

                        override fun onError() {
                            bitmapParts.forEach { part -> part.recycle() }

                            System.gc()

                            listener.onError(
                                AcquirerWorker.QueueError(
                                    data = data,
                                    status = StoneProvider.Printing.Error.fromCode(
                                        _printProvider?.listOfErrors?.get(
                                            0
                                        ) as ErrorsEnum
                                    ).message
                                )
                            )
                        }
                    }
                )

                bitmapParts.forEach { part ->
                    _printProvider?.addBitmap(part)
                }

                withContext(Dispatchers.Default) {
                    _printProvider?.execute()
                }
            }
        } catch (e: Exception) {
            Log.wtf("startPrinting:stone", "SINGLE PRINTING DONE exception ${e}")

            listener.onError(
                AcquirerWorker.QueueError(
                    data,
                    status = e.toString()
                )
            )
        }
    }
}