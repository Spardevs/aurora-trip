package br.com.ticpass.pos.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.ticpass.pos.data.room.converters.Converters
import br.com.ticpass.pos.data.room.dao.*
import br.com.ticpass.pos.data.room.entity.*

@Database(
    entities = [
        MenuEntity::class,
        PaymentSettingsEntity::class,
        PassSettingsEntity::class,
        PosEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        CartOrderLineEntity::class,
        OrderEntity::class,
        PaymentEntity::class,
        PassEntity::class,
        AcquisitionEntity::class,
        CashupEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun paymentSettingsDao(): PaymentSettingsDao
    abstract fun passSettingsDao(): PassSettingsDao
    abstract fun posDao(): PosDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun cartOrderLineDao(): CartOrderLineDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun passDao(): PassDao
    abstract fun acquisitionDao(): AcquisitionDao
    abstract fun cashupDao(): CashupDao
}