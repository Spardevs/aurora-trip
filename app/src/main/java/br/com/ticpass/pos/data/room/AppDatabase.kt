package br.com.ticpass.pos.data.room

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.data.acquirers.workers.SeedDatabaseWorker
import br.com.ticpass.pos.data.acquirers.workers.SeedDatabaseWorker.Companion.KEY_FILENAME
import br.com.ticpass.pos.data.room.dao.AcquisitionDao
import br.com.ticpass.pos.data.room.entity.AcquisitionEntity
import br.com.ticpass.pos.data.room.dao.CartOrderLineDao
import br.com.ticpass.pos.data.room.entity._CartOrderLineEntity
import br.com.ticpass.pos.data.room.dao.CashupDao
import br.com.ticpass.pos.data.room.entity.CashupEntity
import br.com.ticpass.pos.data.room.dao.CategoryDao
import br.com.ticpass.pos.data.room.entity.CategoryEntity
import br.com.ticpass.pos.data.room.dao.ConsumptionDao
import br.com.ticpass.pos.data.room.entity.ConsumptionEntity
import br.com.ticpass.pos.data.room.dao.EventDao
import br.com.ticpass.pos.data.room.entity.EventEntity
import br.com.ticpass.pos.data.room.dao.OrderDao
import br.com.ticpass.pos.data.room.entity.OrderEntity
import br.com.ticpass.pos.data.room.dao.PassDao
import br.com.ticpass.pos.data.room.entity.PassEntity
import br.com.ticpass.pos.data.room.dao.PaymentDao
import br.com.ticpass.pos.data.room.entity.PaymentEntity
import br.com.ticpass.pos.data.room.dao.PosDao
import br.com.ticpass.pos.data.room.entity.PosEntity
import br.com.ticpass.pos.data.room.dao.ProductDao
import br.com.ticpass.pos.data.room.entity.ProductEntity
import br.com.ticpass.pos.data.room.dao.RefundDao
import br.com.ticpass.pos.data.room.entity.RefundEntity
import br.com.ticpass.pos.data.room.entity.CashierEntity
import br.com.ticpass.pos.data.room.dao.CashierDao
import br.com.ticpass.pos.data.room.dao.VoucherDao
import br.com.ticpass.pos.data.room.entity.VoucherEntity
import br.com.ticpass.pos.data.room.dao.VoucherExchangeProductDao
import br.com.ticpass.pos.data.room.entity.VoucherExchangeProductEntity
import br.com.ticpass.pos.data.room.dao.VoucherRedemptionDao
import br.com.ticpass.pos.data.room.entity.VoucherRedemptionEntity
import br.com.ticpass.pos.queue.payment.ProcessingPaymentEntity
import br.com.ticpass.pos.queue.payment.ProcessingPaymentQueueDao
import br.com.ticpass.pos.util.API_HOST
import br.com.ticpass.pos.util.DATABASE_NAME
import br.com.ticpass.pos.util.SEED_DATA_FILENAME
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url
import java.io.File

/**
 * The Room database for this app
 */
@Database(
    entities = [
        CashierEntity::class,
        EventEntity::class,
        PosEntity::class,
        ProductEntity::class,
        CategoryEntity::class,
        OrderEntity::class,
        PaymentEntity::class,
        CashupEntity::class,
        VoucherEntity::class,
        VoucherRedemptionEntity::class,
        VoucherExchangeProductEntity::class,
        RefundEntity::class,
        AcquisitionEntity::class,
        ConsumptionEntity::class,
        PassEntity::class,
        _CartOrderLineEntity::class,
        ProcessingPaymentEntity::class,
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
        AutoMigration (from = 2, to = 3),
        AutoMigration (from = 3, to = 4),
        AutoMigration (from = 4, to = 5),
        AutoMigration (from = 5, to = 6),
        AutoMigration (from = 6, to = 7),
        AutoMigration (from = 7, to = 8),
    ]
)
    @TypeConverters()
abstract class AppDatabase : RoomDatabase() {
    abstract fun cashierDao(): CashierDao
    abstract fun eventDao(): EventDao
    abstract fun posDao(): PosDao
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun cashupDao(): CashupDao
    abstract fun voucherDao(): VoucherDao
    abstract fun voucherRedemptionDao(): VoucherRedemptionDao
    abstract fun voucherExchangeProductDao(): VoucherExchangeProductDao
    abstract fun refundDao(): RefundDao
    abstract fun acquisitionDao(): AcquisitionDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun passDao(): PassDao
    abstract fun cartOrderLineDao(): CartOrderLineDao
    abstract fun processingPaymentQueueDao(): ProcessingPaymentQueueDao

    override fun clearAllTables() {
        val database = Room.databaseBuilder(MainActivity.appContext, AppDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

        database.clearAllTables()
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>()
                                .setInputData(workDataOf(KEY_FILENAME to SEED_DATA_FILENAME))
                                .build()
                            WorkManager.getInstance(context).enqueue(request)
                        }
                    }
                )
                .build()
        }
    }
}

@SuppressLint("Range")
fun getAllTableNames(database: SupportSQLiteDatabase): List<String> {
    val tableNames = mutableListOf<String>()
    // passa um array vazio no segundo parâmetro
    val cursor = database.query(
        "SELECT name FROM sqlite_master WHERE type='table'",
        emptyArray<Any?>()
    )
    cursor.use {
        while (it.moveToNext()) {
            tableNames += it.getString(it.getColumnIndex("name"))
        }
    }
    return tableNames
}

fun getAllDataFromTable(database: SupportSQLiteDatabase, tableName: String): JSONArray {
    val dataArray = JSONArray()
    // idem: args vazio
    val cursor = database.query(
        "SELECT * FROM $tableName",
        emptyArray<Any?>()
    )
    cursor.use {
        while (it.moveToNext()) {
            val rowData = JSONObject()
            for (i in 0 until it.columnCount) {
                rowData.put(it.getColumnName(i), it.getString(i))
            }
            dataArray.put(rowData)
        }
    }
    return dataArray
}


fun exportDataToJson(
    eventId: String,
    posId: String,
): String {
    val context = MainActivity.appContext
    val database = AppDatabase.getInstance(context).openHelper.writableDatabase
    val tableNames = getAllTableNames(database)

    val jsonData = JSONObject()
    for (tableName in tableNames) {
        val tableData = getAllDataFromTable(database, tableName)
        jsonData.put(tableName, tableData)
    }

    val timestamp = System.currentTimeMillis()
    val jsonFileName = "${eventId}_${posId}_$timestamp.json"

    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    val exportsDir = File(dir, "exports")
    exportsDir.mkdirs()
    val jsonFile = File(exportsDir, jsonFileName)

    jsonFile.writeText(jsonData.toString())

    return jsonFile.absolutePath
}

interface FileUploadService {
    @Multipart
    @POST
    fun uploadFile(
        @Url url: String,
        @Part file: MultipartBody.Part
    ): Call<Void>
}

fun sendFile(
    filePath: String,
    eventId: String,
    posId: String,
    onFailure: (cause: String) -> Unit = {},
    onSuccess: () -> Unit = {},
) {
    val targetUrl = "$API_HOST/export/$eventId/pos/$posId"

    val retrofit = Retrofit.Builder()
        .baseUrl("https://example.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create<FileUploadService>()
    val file = File(filePath)

    val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
    val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
    val call = service.uploadFile(targetUrl, filePart)

    call.enqueue(object : retrofit2.Callback<Void> {
        override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
            if (response.isSuccessful) {
                println("File uploaded successfully.")
                onSuccess()

            } else {
                println("File upload failed. ${response.toString()}")
                onFailure(response.message())
            }
        }

        override fun onFailure(call: Call<Void>, t: Throwable) {
            println("File upload failed. Error: ${t.message}")
            onFailure(t.message.toString())
        }
    })
}