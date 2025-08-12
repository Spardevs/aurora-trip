package br.com.ticpass.pos.feature.printing.di

import br.com.ticpass.pos.feature.printing.usecases.ErrorHandlingUseCase
import br.com.ticpass.pos.feature.printing.usecases.ConfirmationUseCase
import br.com.ticpass.pos.feature.printing.usecases.QueueManagementUseCase
import br.com.ticpass.pos.feature.printing.usecases.StateManagementUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Dagger module for providing printing use case dependencies
 */
@Module
@InstallIn(ViewModelComponent::class)
object PrintingUseCaseModule {
    
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
