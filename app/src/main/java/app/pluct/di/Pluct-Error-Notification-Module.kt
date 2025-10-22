package app.pluct.di

import app.pluct.ui.error.ErrorCenter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct-Error-Notification-Module - Hilt module for error notification dependencies
 * Provides ErrorCenter as singleton
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctErrorNotificationModule {
    
    @Provides
    @Singleton
    fun provideErrorCenter(): ErrorCenter {
        return ErrorCenter()
    }
}
