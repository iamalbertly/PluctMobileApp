package app.pluct.di

import app.pluct.core.error.ErrorCenter
import app.pluct.data.api.PluctBusinessEngineClient
import app.pluct.data.transcription.PluctTranscriptionProgressTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct-DI-Consolidated-Module - Consolidated dependency injection module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctDIConsolidatedModule {
    
    @Provides
    @Singleton
    fun provideErrorCenter(): ErrorCenter = ErrorCenter()
    
    @Provides
    @Singleton
    fun provideBusinessEngineClient(
        errorCenter: ErrorCenter
    ): PluctBusinessEngineClient = PluctBusinessEngineClient(errorCenter)
    
    @Provides
    @Singleton
    fun provideProgressTracker(
        errorCenter: ErrorCenter
    ): PluctTranscriptionProgressTracker = PluctTranscriptionProgressTracker(errorCenter)
}
