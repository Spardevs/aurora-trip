package br.com.ticpass.pos.di

import br.com.ticpass.pos.core.queue.processors.nfc.utils.NFCOperations
import br.com.ticpass.pos.core.queue.processors.nfc.utils.StoneNFCOperations
import br.com.ticpass.pos.core.sdk.AcquirerCapabilities
import br.com.ticpass.pos.core.sdk.AcquirerSdk
import br.com.ticpass.pos.core.sdk.FlavorCapabilities
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Stone acquirer dependencies.
 * Note: Most providers are accessed directly via AcquirerSdk singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object AcquirerModule {
    
    @Provides
    @Singleton
    fun provideAcquirerCapabilities(): AcquirerCapabilities {
        return FlavorCapabilities.capabilities
    }
    
    @Provides
    @Singleton
    fun provideNFCOperations(): NFCOperations {
        return StoneNFCOperations(AcquirerSdk.nfc.getInstance())
    }
}
