package br.com.ticpass.pos.di

import android.content.Context
import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import br.com.ticpass.pos.core.queue.processors.nfc.utils.ProprietaryGertecNFCOperations
import br.com.ticpass.pos.core.sdk.AcquirerCapabilities
import br.com.ticpass.pos.core.sdk.FlavorCapabilities
import br.com.ticpass.pos.core.sdk.factory.AcquirerPrintingProvider
import br.com.ticpass.pos.core.sdk.factory.AcquirerPrintingProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for ProprietaryGertec acquirer dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AcquirerModule {
    
    @Provides
    @Singleton
    fun providePrintingProvider(@ApplicationContext context: Context): AcquirerPrintingProvider {
        return AcquirerPrintingProviderFactory(context).create()
    }
    
    @Provides
    @Singleton
    fun provideAcquirerCapabilities(): AcquirerCapabilities {
        return FlavorCapabilities.capabilities
    }
    
    @Provides
    @Singleton
    fun provideNFCOperations(): NFCOperations {
        return ProprietaryGertecNFCOperations()
    }
}
