package app.pluct.di

import app.pluct.ui.error.ErrorCenter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PluctErrorModule {
    
    @Provides
    @Singleton
    fun provideErrorCenter(): ErrorCenter {
        return ErrorCenter()
    }
}
