package br.com.ticpass.pos.data.room.entity

import android.os.Environment
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.ticpass.pos.MainActivity
import br.com.ticpass.pos.compose.utils.getCurrentDateString
import br.com.ticpass.pos.compose.utils.generateRandomEAN
import java.io.File

@Entity(tableName = "acquisitions" )
data class AcquisitionEntity(
    @PrimaryKey val id: String = generateRandomEAN(),
    val createdAt: String = getCurrentDateString(),

    var name: String,
    var logo: String,
    val price: Long,
    @ColumnInfo(defaultValue = "0")
    val commission: Long = 0L,
    var category: String,

    val product: String, // product id
    val order: String, // order id
    val pass: String, // pass id
    val event: String, // event id
    val pos: String, // pos id

    var synced: Boolean = false,
) {

    val isConsumed: Boolean
        get() {
            return !consumption.isNullOrEmpty()
        }

    val isRefunded: Boolean
        get() {
            return !refund.isNullOrEmpty()
        }

    val isVouchered: Boolean
        get() {
            return !voucher.isNullOrEmpty()
        }

    override fun toString() = id

    var voucher: String = ""
        set(voucherId: String) {
            if(isVouchered || isRefunded || isConsumed) return
            field = voucherId
        }

    var refund: String = ""
        set(refundId: String) {
            if(isVouchered || isRefunded || isConsumed) return
            field = refundId
        }

    var consumption: String = ""
        set(consumptionId: String) {
            if(isVouchered || isRefunded || isConsumed) return
            field = consumptionId
        }

    val logoLocalFile: File
        get() {
            val dir = MainActivity.appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imagesDir = File(dir, "products")
            imagesDir.mkdirs()

            val file = File(imagesDir, logo.substringAfterLast('/'))

            Log.d("getLogoPath", "imagesDir: $logo")
            Log.d("getLogoPath", "imagesDir: ${file.absolutePath}")

            return file
        }

    val logoRemoteURL: String
        get() {
            Log.d("getLogoPath", "imagesDir: $logo")
            return "https://ticpass-storage.s3.sa-east-1.amazonaws.com/product/thumbnail/$logo"
        }
}


data class _AcquisitionPopulated(
    @Embedded val acquisition: AcquisitionEntity,
)

data class AcquisitionPopulated(
    val acquisition: AcquisitionEntity,
) {
    val id: String
        get() = acquisition.id

    val createdAt: String
        get() = acquisition.createdAt

    val name: String
        get() = acquisition.name
    val logo: String
        get() = acquisition.logo
    val price: Long
        get() = acquisition.price
    val category: String
        get() = acquisition.category

    val product: String
        get() = acquisition.product // product id
    val order: String
        get() = acquisition.order // order id
    val pass: String
        get() = acquisition.pass // pass id
    val event: String
        get() = acquisition.event // event id
    var pos: String = ""
        get() = acquisition.pos // pos id

    val commission: Long
        get() = acquisition.commission // event id

    val isConsumed: Boolean
        get() = acquisition.isConsumed // pos id

    val isRefunded: Boolean
        get() = acquisition.isRefunded // pos id

    val isVouchered: Boolean
        get() = acquisition.isVouchered // pos id
}
