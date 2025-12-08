package br.com.ticpass.pos.core.di

import br.com.ticpass.pos.core.queue.processors.nfc.data.NFCQueueDao
import br.com.ticpass.pos.core.queue.processors.nfc.data.NFCStorage
import br.com.ticpass.pos.core.queue.processors.payment.data.PaymentProcessingQueueDao
import br.com.ticpass.pos.core.queue.processors.payment.data.PaymentProcessingStorage
import br.com.ticpass.pos.core.queue.processors.printing.data.PrintingQueueDao
import br.com.ticpass.pos.core.queue.processors.printing.data.PrintingStorage
import br.com.ticpass.pos.core.queue.processors.refund.data.RefundQueueDao
import br.com.ticpass.pos.core.queue.processors.refund.data.RefundStorage
import br.com.ticpass.pos.presentation.payment.utils.PixCodeGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing queue processor dependencies for the testing activities.
 * This module provides Storage and QueueFactory instances for Payment, NFC, Printing, and Refund processors.
 */
@Module
@InstallIn(SingletonComponent::class)
object QueueStorageModule {

    @Provides
    @Singleton
    fun providePaymentProcessingStorage(
        dao: PaymentProcessingQueueDao
    ): PaymentProcessingStorage {
        return PaymentProcessingStorage(dao)
    }

    @Provides
    @Singleton
    fun provideNFCStorage(
        dao: NFCQueueDao
    ): NFCStorage {
        return NFCStorage(dao)
    }

    @Provides
    @Singleton
    fun providePrintingStorage(
        dao: PrintingQueueDao
    ): PrintingStorage {
        return PrintingStorage(dao)
    }

    @Provides
    @Singleton
    fun provideRefundStorage(
        dao: RefundQueueDao
    ): RefundStorage {
        return RefundStorage(dao)
    }

    @Provides
    @Singleton
    fun providePixCodeGenerator(): PixCodeGenerator {
        return PixCodeGenerator()
    }
}

// Note: QueueFactory classes have @Inject constructors and will be provided automatically by Hilt
// The ProcessorRegistry classes also have @Inject constructors and will be injected into the factories
