package br.com.ticpass.pos.di

import android.content.Context
import androidx.room.Room
import br.com.ticpass.pos.data.local.database.AppDatabase
import br.com.ticpass.pos.data.user.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "ticpass_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    // Se tiver NFCQueueDao / RefundQueueDao, adicione aqui:
    // @Provides fun provideNfcQueueDao(db: AppDatabase): NFCQueueDao = db.nfcQueueDao()
    // @Provides fun provideRefundQueueDao(db: AppDatabase): RefundQueueDao = db.refundQueueDao()
}