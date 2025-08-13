package br.com.ticpass.pos.feature.refund.di

import br.com.ticpass.pos.feature.refund.usecases.ErrorHandlingUseCase
import br.com.ticpass.pos.feature.refund.usecases.ConfirmationUseCase
import br.com.ticpass.pos.feature.refund.usecases.QueueManagementUseCase
import br.com.ticpass.pos.feature.refund.usecases.StateManagementUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Dagger module for providing refund use case dependencies
 */
@Module
@InstallIn(ViewModelComponent::class)
object RefundUseCaseModule {
    
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
