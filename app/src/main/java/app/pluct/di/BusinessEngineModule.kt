package app.pluct.di

import app.pluct.data.BusinessEngineClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BusinessEngineModule {
    
    @Provides
    @Singleton
    fun provideBusinessEngineClient(): BusinessEngineClient {
        return BusinessEngineClient("https://pluct-business-engine.romeo-lya2.workers.dev")
    }
}
