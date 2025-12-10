package br.com.ticpass.pos.di

import android.content.Context
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import br.com.ticpass.pos.core.queue.processors.nfc.utils.PagSeguroNFCOperations
import br.com.ticpass.pos.core.sdk.AcquirerCapabilities
import br.com.ticpass.pos.core.sdk.FlavorCapabilities
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for PagSeguro acquirer dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AcquirerModule {
    
    @Provides
    @Singleton
    fun providePlugPag(@ApplicationContext context: Context): PlugPag {
        return PlugPag(context)
    }
    
    @Provides
    @Singleton
    fun provideAcquirerCapabilities(): AcquirerCapabilities {
        return FlavorCapabilities.capabilities
    }
    
    @Provides
    @Singleton
    fun provideNFCOperations(plugPag: PlugPag): NFCOperations {
        return PagSeguroNFCOperations(plugPag)
    }
}
