package br.com.ticpass.pos.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.ticpass.pos.data.menu.local.dao.MenuDao
import br.com.ticpass.pos.data.menu.local.entity.MenuEntity
import br.com.ticpass.pos.data.pos.local.dao.PosDao
import br.com.ticpass.pos.data.pos.local.entity.PosEntity
import br.com.ticpass.pos.data.user.local.dao.UserDao
import br.com.ticpass.pos.data.user.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, MenuEntity::class, PosEntity::class],
    version = 1,
    exportSchema = false
)


abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun menuDao(): MenuDao
    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ticpass_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}