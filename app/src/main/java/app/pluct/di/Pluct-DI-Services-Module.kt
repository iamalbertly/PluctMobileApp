package app.pluct.di

import android.content.Context
import app.pluct.analytics.PluctAnalyticsCoreService
import app.pluct.collaboration.PluctCollaborationCoreManager
import app.pluct.search.PluctSearchCoreEngine
import app.pluct.transcription.PluctTranscriptionProcessor
import app.pluct.transcription.PluctTranscriptionCoordinator
import app.pluct.data.service.VideoMetadataService
import app.pluct.data.service.VideoMetadataExtractor
import app.pluct.data.provider.PluctHuggingFaceProviderCoordinator
import app.pluct.data.manager.UserManager
import app.pluct.data.processor.UrlProcessor
import app.pluct.utils.PluctUtilsValuePropositionGenerator
import app.pluct.purchase.PluctCoinManager
import app.pluct.status.PluctStatusTrackingManager
import app.pluct.api.PluctCoreApiService
import app.pluct.api.PluctTTTranscribeService
import app.pluct.data.dao.UserCoinsDao
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct Services DI Module - Core service providers
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctDIServicesModule {
    
    @Provides
    @Singleton
    fun provideVideoMetadataService(): VideoMetadataService {
        return VideoMetadataService()
    }
    
    @Provides
    @Singleton
    fun provideVideoMetadataExtractor(): VideoMetadataExtractor {
        return VideoMetadataExtractor()
    }
    
    @Provides
    @Singleton
    fun providePluctHuggingFaceProviderCoordinator(): PluctHuggingFaceProviderCoordinator {
        return PluctHuggingFaceProviderCoordinator()
    }
    
    @Provides
    @Singleton
    fun provideUrlProcessor(): UrlProcessor {
        return UrlProcessor()
    }
    
    @Provides
    @Singleton
    fun provideValuePropositionGenerator(): PluctUtilsValuePropositionGenerator {
        return PluctUtilsValuePropositionGenerator
    }
    
    @Provides
    @Singleton
    fun providePluctTranscriptionProcessor(
        @ApplicationContext context: Context,
        apiService: PluctCoreApiService,
        ttTranscribeService: PluctTTTranscribeService,
        urlProcessor: UrlProcessor
    ): PluctTranscriptionProcessor {
        return PluctTranscriptionProcessor(
            context = context,
            apiService = apiService,
            ttTranscribeService = ttTranscribeService,
            urlProcessor = urlProcessor
        )
    }
    
    @Provides
    @Singleton
    fun providePluctTranscriptionCoordinator(
        @ApplicationContext context: Context,
        huggingFaceProvider: PluctHuggingFaceProviderCoordinator,
        valuePropositionGenerator: PluctUtilsValuePropositionGenerator
    ): PluctTranscriptionCoordinator {
        return PluctTranscriptionCoordinator(
            context = context,
            huggingFaceProvider = huggingFaceProvider,
            valuePropositionGenerator = valuePropositionGenerator
        )
    }
    
    @Provides
    @Singleton
    fun providePluctAnalyticsCoreService(
        @ApplicationContext context: Context
    ): PluctAnalyticsCoreService {
        return PluctAnalyticsCoreService(context)
    }
    
    @Provides
    @Singleton
    fun providePluctCollaborationCoreManager(
        @ApplicationContext context: Context
    ): PluctCollaborationCoreManager {
        return PluctCollaborationCoreManager(context)
    }
    
    @Provides
    @Singleton
    fun providePluctSearchCoreEngine(
        @ApplicationContext context: Context
    ): PluctSearchCoreEngine {
        return PluctSearchCoreEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideUserManager(
        @ApplicationContext context: Context,
        apiService: PluctCoreApiService
    ): UserManager {
        return UserManager(context, apiService)
    }
    
    @Provides
    @Singleton
    fun provideCoinManager(userCoinsDao: UserCoinsDao): PluctCoinManager {
        return PluctCoinManager(userCoinsDao)
    }
    
    @Provides
    @Singleton
    fun providePluctStatusTrackingManager(
        @ApplicationContext context: Context
    ): PluctStatusTrackingManager {
        return PluctStatusTrackingManager(context)
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}
