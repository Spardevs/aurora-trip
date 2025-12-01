package br.com.ticpass.pos.data.product.repository

import br.com.ticpass.pos.data.product.datasource.ProductLocalDataSource
import br.com.ticpass.pos.data.product.datasource.ProductRemoteDataSource
import br.com.ticpass.pos.data.product.mapper.toEntity
import br.com.ticpass.pos.domain.product.model.Product
import br.com.ticpass.pos.domain.product.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val localDataSource: ProductLocalDataSource,
    private val remoteDataSource: ProductRemoteDataSource
) : ProductRepository {

    override fun getEnabledProducts(): Flow<List<Product>> {
        return localDataSource.getEnabledProducts().map { entities ->
            entities.map { entity ->
                Product(
                    id = entity.id,
                    category = entity.category,
                    name = entity.name,
                    thumbnail = entity.thumbnail,
                    price = entity.price,
                    stock = entity.stock,
                    isEnabled = entity.isEnabled
                )
            }
        }
    }

    override suspend fun refreshProducts(menuId: String) {
        val response = remoteDataSource.getProducts(menuId)
        val entities = response.products.map { it.toEntity() }
        localDataSource.insertAll(entities)
    }

    suspend fun downloadAndExtractThumbnails(menuId: String, thumbnailsDir: File) {
        val responseBody = remoteDataSource.downloadThumbnails(menuId)
        saveAndExtractZip(responseBody, thumbnailsDir)
    }

    private fun saveAndExtractZip(responseBody: ResponseBody, thumbnailsDir: File) {
        if (!thumbnailsDir.exists()) {
            thumbnailsDir.mkdirs()
        }

        val inputStream = responseBody.byteStream()
        val zipInputStream = ZipInputStream(inputStream)

        try {
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                val outputFile = File(thumbnailsDir, entry.name)

                outputFile.parentFile?.let { parent ->
                    if (!parent.exists()) {
                        parent.mkdirs()
                    }
                }

                FileOutputStream(outputFile).use { output ->
                    zipInputStream.copyTo(output)
                }

                zipInputStream.closeEntry()

                entry = zipInputStream.nextEntry
            }
        } finally {
            zipInputStream.close()
            inputStream.close()
        }
    }
}