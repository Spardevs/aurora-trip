package br.com.ticpass.pos.module

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedPrefsModule {

    private const val SESSION_PREFS = "SessionPrefs"

    @Provides
    @Singleton
    fun provideSessionSharedPrefs(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
    }
}
