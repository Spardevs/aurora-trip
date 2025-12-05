package br.com.ticpass.pos.di

import android.content.Context
import br.com.ticpass.pos.queue.processors.nfc.utils.NFCOperations
import br.com.ticpass.pos.queue.processors.nfc.utils.StoneNFCOperations
import br.com.ticpass.pos.sdk.AcquirerCapabilities
import br.com.ticpass.pos.sdk.FlavorCapabilities
import br.com.ticpass.pos.sdk.SdkInstance
import br.com.ticpass.pos.sdk.factory.AcquirerNFCProvider
import br.com.ticpass.pos.sdk.factory.AcquirerNFCProviderFactory
import br.com.ticpass.pos.sdk.factory.AcquirerPrintingProvider
import br.com.ticpass.pos.sdk.factory.AcquirerPrintingProviderFactory
import br.com.ticpass.pos.sdk.factory.AcquirerRefundProvider
import br.com.ticpass.pos.sdk.factory.CustomerReceiptProvider
import br.com.ticpass.pos.sdk.factory.CustomerReceiptProviderFactory
import br.com.ticpass.pos.sdk.factory.RefundProviderFactory
import br.com.ticpass.pos.sdk.factory.TransactionProvider
import br.com.ticpass.pos.sdk.factory.TransactionProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import stone.user.UserModel
import javax.inject.Singleton

/**
 * Hilt module for Stone acquirer dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AcquirerModule {
    
    @Provides
    @Singleton
    fun provideStoneSdk(@ApplicationContext context: Context): Pair<UserModel, Context> {
        return SdkInstance.initialize(context)
    }
    
    @Provides
    @Singleton
    fun provideTransactionProvider(sdk: Pair<UserModel, Context>): TransactionProvider {
        val (userModel, context) = sdk
        return TransactionProviderFactory(context, userModel).create()
    }
    
    @Provides
    @Singleton
    fun provideCustomerReceiptProvider(sdk: Pair<UserModel, Context>): CustomerReceiptProvider {
        val (_, context) = sdk
        return CustomerReceiptProviderFactory(context).create()
    }
    
    @Provides
    @Singleton
    fun provideNFCProvider(sdk: Pair<UserModel, Context>): AcquirerNFCProvider {
        val (_, context) = sdk
        return AcquirerNFCProviderFactory(context).create()
    }
    
    @Provides
    @Singleton
    fun providePrintingProvider(sdk: Pair<UserModel, Context>): AcquirerPrintingProvider {
        val (_, context) = sdk
        return AcquirerPrintingProviderFactory(context).create()
    }
    
    @Provides
    @Singleton
    fun provideRefundProvider(sdk: Pair<UserModel, Context>): AcquirerRefundProvider {
        val (_, context) = sdk
        return RefundProviderFactory(context).create()
    }
    
    @Provides
    @Singleton
    fun provideAcquirerCapabilities(): AcquirerCapabilities {
        return FlavorCapabilities.capabilities
    }
    
    @Provides
    @Singleton
    fun provideNFCOperations(nfcProvider: AcquirerNFCProvider): NFCOperations {
        return StoneNFCOperations(nfcProvider)
    }
}
