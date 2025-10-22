package app.pluct.di

import app.pluct.error.PluctErrorHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct-Error-Handler-Module - Hilt module for error handler dependencies
 * Provides PluctErrorHandler as singleton
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctErrorHandlerModule {

    @Provides
    @Singleton
    fun providePluctErrorHandler(): PluctErrorHandler {
        return PluctErrorHandler
    }
}
