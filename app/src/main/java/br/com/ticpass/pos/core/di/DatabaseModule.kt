package br.com.ticpass.pos.core.di

import android.content.Context
import androidx.room.Room
import br.com.ticpass.pos.data.local.database.AppDatabase
import br.com.ticpass.pos.data.menu.local.dao.MenuDao
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

    @Provides
    @Singleton
    fun provideMenuDao(appDatabase: AppDatabase): MenuDao {
        return appDatabase.menuDao()
    }
}