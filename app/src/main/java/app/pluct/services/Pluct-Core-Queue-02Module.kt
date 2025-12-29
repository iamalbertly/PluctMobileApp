package app.pluct.services

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct-Core-Queue-02Module
 * Hilt module for providing QueueManager
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Sequence][Responsibility]
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctQueueModule {
    
    @Provides
    @Singleton
    fun provideQueueManager(
        videoRepository: app.pluct.data.repository.PluctVideoRepository
    ): PluctQueueManager {
        return PluctQueueManager(videoRepository)
    }
}



