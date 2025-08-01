package br.com.ticpass.pos.data.acquirers.workers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import androidx.work.CoroutineWorker
import br.com.ticpass.pos.util.savePassAsBitmap
import br.com.ticpass.pos.view.ui.pass.PassData
import br.com.ticpass.pos.view.ui.pass.PassType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Dados da imagem para processamento
 */
@Serializable
data class ImageData(
    val id: String,
    val filePath: String,
    val fileName: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Configurações para o processamento em batch
 */
@Serializable
data class BatchConfig(
    val batchSize: Int = 10,
    val maxConcurrency: Int = 2,
    val delayBetweenBatches: Long = 0L
)

/**
 * Resultado do processamento de uma imagem
 */
sealed class ImageProcessResult {
    data class Success(
        val imageData: ImageData,
        val processedPath: String,
        val processingTime: Long
    ) : ImageProcessResult()

    data class Error(
        val imageData: ImageData,
        val errorMessage: String,
        val exception: String?
    ) : ImageProcessResult()
}

/**
 * Callbacks para o processamento de imagens
 */
interface ImageProcessorCallbacks {
    /**
     * Chamado quando uma imagem individual é processada
     */
    suspend fun onImageProcessed(result: ImageProcessResult) {}

    /**
     * Chamado quando um batch de imagens é processado
     */
    suspend fun onBatchCompleted(batchResults: List<ImageProcessResult>) {}

    /**
     * Chamado quando todas as imagens são processadas
     */
    suspend fun onAllImagesCompleted(allResults: List<ImageProcessResult>) {}
}

/**
 * Worker que executa o processamento de imagens em batch
 */
class ImageBatchProcessingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val imagesJson = inputData.getString(KEY_IMAGES) ?: return Result.failure()
            val configJson = inputData.getString(KEY_CONFIG) ?: return Result.failure()

            val images: List<ImageData> = Json.decodeFromString(imagesJson)
            val config: BatchConfig = Json.decodeFromString(configJson)

            var processedCount = 0
            val totalImages = images.size

            val callbacks = object : ImageProcessorCallbacks {
                override suspend fun onImageProcessed(result: ImageProcessResult) {
                    processedCount++
                    val progress = (processedCount * 100) / totalImages

                    when (result) {
                        is ImageProcessResult.Success -> {
                            setProgress(createProgressData(
                                message = "Imagem processada: ${result.imageData.fileName}",
                                progress = progress,
                                currentImage = result.imageData.fileName,
                                processedCount = processedCount,
                                totalCount = totalImages
                            ))
                        }
                        is ImageProcessResult.Error -> {
                            setProgress(createProgressData(
                                message = "Erro ao processar: ${result.imageData.fileName}",
                                progress = progress,
                                currentImage = result.imageData.fileName,
                                processedCount = processedCount,
                                totalCount = totalImages,
                                hasError = true
                            ))
                        }
                    }
                }

                override suspend fun onBatchCompleted(batchResults: List<ImageProcessResult>) {
                    val successInBatch = batchResults.count { it is ImageProcessResult.Success }
                    val errorsInBatch = batchResults.count { it is ImageProcessResult.Error }

                    setProgress(createProgressData(
                        message = "Batch concluído: $successInBatch sucessos, $errorsInBatch erros",
                        progress = (processedCount * 100) / totalImages,
                        currentImage = "Processando batch...",
                        processedCount = processedCount,
                        totalCount = totalImages
                    ))
                }

                override suspend fun onAllImagesCompleted(allResults: List<ImageProcessResult>) {
                    setProgress(createProgressData(
                        message = "Todas as imagens processadas!",
                        progress = 100,
                        currentImage = "Concluído",
                        processedCount = totalImages,
                        totalCount = totalImages
                    ))
                }
            }

            val processor = ImageBatchProcessor(config, callbacks)
            processor.enqueueAll(images)

            val results = processor.startProcessing { imageData ->
                processImage(imageData)
            }

            val successCount = results.count { it is ImageProcessResult.Success }
            val errorCount = results.count { it is ImageProcessResult.Error }
            val successfulImages = results.filterIsInstance<ImageProcessResult.Success>()
                .map { it.imageData.fileName }
            val failedImages = results.filterIsInstance<ImageProcessResult.Error>()
                .map { "${it.imageData.fileName}: ${it.errorMessage}" }

            val outputData = workDataOf(
                KEY_SUCCESS_COUNT to successCount,
                KEY_ERROR_COUNT to errorCount,
                KEY_TOTAL_IMAGES to totalImages,
                KEY_SUCCESSFUL_IMAGES to Json.encodeToString(successfulImages),
                KEY_FAILED_IMAGES to Json.encodeToString(failedImages)
            )

            Result.success(outputData)

        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to e.message))
        }
    }

    private suspend fun processImage(imageData: ImageData): String {
        val startTime = System.currentTimeMillis()

        try {
            val passDataJson = imageData.metadata["passData"]
                ?: throw IllegalArgumentException("PassData ausente")
            val passTypeName = imageData.metadata["passType"]
                ?: throw IllegalArgumentException("PassType ausente")

            val passData = Json.decodeFromString<PassData>(passDataJson)

            val passType = when (passTypeName) {
                "ProductCompact" -> PassType.ProductCompact
                "ProductExpanded" -> PassType.ProductExpanded
                "ProductGrouped" -> PassType.ProductGrouped
                else -> throw IllegalArgumentException("Tipo desconhecido: $passTypeName")
            }

            val file = savePassAsBitmap(applicationContext, passType, passData)
            val processingTime = System.currentTimeMillis() - startTime

            return file.absolutePath

        } catch (e: Exception) {
            throw e
        }
    }


    private fun createProgressData(
        message: String,
        progress: Int,
        currentImage: String,
        processedCount: Int,
        totalCount: Int,
        hasError: Boolean = false
    ): Data {
        return workDataOf(
            KEY_PROGRESS_MESSAGE to message,
            KEY_PROGRESS_PERCENT to progress,
            KEY_CURRENT_IMAGE to currentImage,
            KEY_PROCESSED_COUNT to processedCount,
            KEY_TOTAL_COUNT to totalCount,
            KEY_HAS_ERROR to hasError
        )
    }

    companion object {
        const val KEY_IMAGES = "images"
        const val KEY_CONFIG = "config"
        const val KEY_SUCCESS_COUNT = "success_count"
        const val KEY_ERROR_COUNT = "error_count"
        const val KEY_TOTAL_IMAGES = "total_images"
        const val KEY_SUCCESSFUL_IMAGES = "successful_images"
        const val KEY_FAILED_IMAGES = "failed_images"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_PROGRESS_MESSAGE = "progress_message"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_CURRENT_IMAGE = "current_image"
        const val KEY_PROCESSED_COUNT = "processed_count"
        const val KEY_TOTAL_COUNT = "total_count"
        const val KEY_HAS_ERROR = "has_error"
    }
}

/**
 * Processador de imagens em batch
 */
class ImageBatchProcessor(
    private val config: BatchConfig = BatchConfig(),
    private val callbacks: ImageProcessorCallbacks? = null
) {
    private val queue = ConcurrentLinkedQueue<ImageData>()
    private val mutex = Mutex()
    private var isProcessing = false

    fun enqueue(imageData: ImageData) {
        queue.offer(imageData)
    }

    fun enqueueAll(images: Collection<ImageData>) {
        images.forEach { queue.offer(it) }
    }

    suspend fun startProcessing(processor: suspend (ImageData) -> String): List<ImageProcessResult> {
        mutex.withLock {
            if (isProcessing) {
                throw IllegalStateException("Processamento já está em andamento")
            }
            isProcessing = true
        }

        return try {
            processAllImages(processor)
        } finally {
            mutex.withLock {
                isProcessing = false
            }
        }
    }

    private suspend fun processAllImages(processor: suspend (ImageData) -> String): List<ImageProcessResult> {
        val allResults = mutableListOf<ImageProcessResult>()
        val batches = createBatches()

        batches.forEachIndexed { index, batch ->
            val batchResults = processBatch(batch, processor)
            allResults.addAll(batchResults)
            callbacks?.onBatchCompleted(batchResults)

            if (index != batches.lastIndex && config.delayBetweenBatches > 0) {
                delay(config.delayBetweenBatches)
            }
        }

        callbacks?.onAllImagesCompleted(allResults)
        return allResults
    }

    private fun createBatches(): List<List<ImageData>> {
        val batches = mutableListOf<List<ImageData>>()
        val currentBatch = mutableListOf<ImageData>()

        while (queue.isNotEmpty()) {
            currentBatch.add(queue.poll())

            if (currentBatch.size >= config.batchSize) {
                batches.add(currentBatch.toList())
                currentBatch.clear()
            }
        }

        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch.toList())
        }

        return batches
    }

    private suspend fun processBatch(
        batch: List<ImageData>,
        processor: suspend (ImageData) -> String
    ): List<ImageProcessResult> = coroutineScope {
        val semaphore = Semaphore(config.maxConcurrency)

        batch.map { imageData ->
            async {
                semaphore.withPermit {
                    val startTime = System.currentTimeMillis()
                    try {
                        val processedPath = processor(imageData)
                        val processingTime = System.currentTimeMillis() - startTime

                        val result = ImageProcessResult.Success(
                            imageData, processedPath, processingTime
                        )
                        callbacks?.onImageProcessed(result)
                        result
                    } catch (e: Exception) {
                        val result = ImageProcessResult.Error(
                            imageData,
                            e.message ?: "Erro desconhecido",
                            e.toString()
                        )
                        callbacks?.onImageProcessed(result)
                        result
                    }
                }
            }
        }.awaitAll()
    }



    fun getQueueSize(): Int = queue.size
    fun isProcessing(): Boolean = isProcessing
}

/**
 * Manager para controlar o processamento de imagens com WorkManager
 */
class ImageBatchWorkManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Inicia o processamento de imagens em batch
     */
    fun processImages(
        images: List<ImageData>,
        config: BatchConfig = BatchConfig()
    ): LiveData<WorkInfo?> {

        val inputData = workDataOf(
            ImageBatchProcessingWorker.KEY_IMAGES to Json.encodeToString(images),
            ImageBatchProcessingWorker.KEY_CONFIG to Json.encodeToString(config)
        )

        val workRequest = OneTimeWorkRequestBuilder<ImageBatchProcessingWorker>()
            .setInputData(inputData)
            .addTag("image_batch_processing")
            .build()

        workManager.enqueue(workRequest)

        return workManager.getWorkInfoByIdLiveData(workRequest.id)
    }

    /**
     * Cancela o processamento de imagens
     */
    fun cancelImageProcessing() {
        workManager.cancelAllWorkByTag("image_batch_processing")
    }
}