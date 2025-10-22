package app.pluct.di

import android.content.Context
import androidx.work.WorkManager
import app.pluct.data.PluctBusinessEngineUnifiedClientNew
import app.pluct.ui.error.ErrorCenter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct-DI-Consolidated-Module - Consolidated Hilt module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Single source of truth for all dependencies, eliminating duplication
 * Only provides classes that need manual provision (not @Singleton @Inject classes)
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctDIConsolidatedModule {
    
    @Provides
    @Singleton
    fun provideBusinessEngineClient(): PluctBusinessEngineUnifiedClientNew {
        return PluctBusinessEngineUnifiedClientNew("https://pluct-business-engine.romeo-lya2.workers.dev")
    }
    
    @Provides
    @Singleton
    fun provideErrorCenter(): ErrorCenter {
        return ErrorCenter()
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
