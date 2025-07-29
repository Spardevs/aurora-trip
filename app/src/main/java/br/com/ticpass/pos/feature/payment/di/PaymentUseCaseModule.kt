package br.com.ticpass.pos.feature.payment.di

import br.com.ticpass.pos.feature.payment.usecases.ErrorHandlingUseCase
import br.com.ticpass.pos.feature.payment.usecases.ConfirmationUseCase
import br.com.ticpass.pos.feature.payment.usecases.QueueManagementUseCase
import br.com.ticpass.pos.feature.payment.usecases.StateManagementUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Dagger module for providing payment use case dependencies
 */
@Module
@InstallIn(ViewModelComponent::class)
object PaymentUseCaseModule {
    
    @Provides
    @ViewModelScoped
    fun provideQueueManagementUseCase(): QueueManagementUseCase {
        return QueueManagementUseCase()
    }
    
    @Provides
    @ViewModelScoped
    fun provideErrorHandlingUseCase(): ErrorHandlingUseCase {
        return ErrorHandlingUseCase()
    }
    
    @Provides
    @ViewModelScoped
    fun provideProcessorConfirmationUseCase(): ConfirmationUseCase {
        return ConfirmationUseCase()
    }
    
    @Provides
    @ViewModelScoped
    fun provideStateManagementUseCase(): StateManagementUseCase {
        return StateManagementUseCase()
    }
}
