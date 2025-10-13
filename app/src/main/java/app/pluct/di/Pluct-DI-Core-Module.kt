package app.pluct.di

import android.content.Context
import app.pluct.api.PluctCoreApiService
import app.pluct.api.PluctTTTranscribeAuthenticator
import app.pluct.api.PluctTTTranscribeService
import app.pluct.analytics.PluctAnalyticsCoreService
import app.pluct.collaboration.PluctCollaborationCoreManager
import app.pluct.search.PluctSearchCoreEngine
import app.pluct.transcription.PluctTranscriptionCoreManager
import app.pluct.data.database.PluctDatabase
import app.pluct.data.dao.OutputArtifactDao
import app.pluct.data.dao.TranscriptDao
import app.pluct.data.dao.VideoItemDao
import app.pluct.data.dao.UserCoinsDao
import app.pluct.data.service.VideoMetadataService
import app.pluct.data.service.VideoMetadataExtractor
import app.pluct.data.provider.PluctHuggingFaceProviderCoordinator
import app.pluct.data.manager.UserManager
import app.pluct.data.processor.UrlProcessor
import app.pluct.utils.PluctUtilsValuePropositionGenerator
import app.pluct.purchase.PluctCoinManager
import app.pluct.status.PluctStatusTrackingManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct Core DI Module - Single source of truth for dependency injection
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Consolidated from AppModule.kt with simplified structure
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctDICoreModule {
    
    // --- Database Providers ---
    @Provides
    @Singleton
    fun providePluctDatabase(@ApplicationContext context: Context): PluctDatabase {
        return PluctDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideVideoItemDao(database: PluctDatabase): VideoItemDao {
        return database.videoItemDao()
    }
    
    @Provides
    fun provideTranscriptDao(database: PluctDatabase): TranscriptDao {
        return database.transcriptDao()
    }
    
    @Provides
    fun provideOutputArtifactDao(database: PluctDatabase): OutputArtifactDao {
        return database.outputArtifactDao()
    }
    
    @Provides
    fun provideUserCoinsDao(database: PluctDatabase): UserCoinsDao {
        return database.userCoinsDao()
    }
    
    // --- Core Service Providers ---
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
    
    // --- HTTP Client Providers ---
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun providePluctCoreApiService(okHttpClient: OkHttpClient, moshi: Moshi): PluctCoreApiService {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PluctCoreApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun providePluctTTTranscribeAuthenticator(): PluctTTTranscribeAuthenticator {
        return PluctTTTranscribeAuthenticator()
    }
    
    @Provides
    @Singleton
    fun providePluctTTTranscribeService(
        apiService: PluctCoreApiService,
        authenticator: PluctTTTranscribeAuthenticator,
        statusTracker: PluctStatusTrackingManager
    ): PluctTTTranscribeService {
        return PluctTTTranscribeService(apiService, authenticator, statusTracker)
    }
    
    // --- Core Manager Providers ---
    @Provides
    @Singleton
    fun providePluctTranscriptionCoreManager(
        @ApplicationContext context: Context,
        apiService: PluctCoreApiService,
        ttTranscribeService: PluctTTTranscribeService,
        urlProcessor: UrlProcessor,
        huggingFaceProvider: PluctHuggingFaceProviderCoordinator,
        valuePropositionGenerator: PluctUtilsValuePropositionGenerator
    ): PluctTranscriptionCoreManager {
        return PluctTranscriptionCoreManager(
            context = context,
            apiService = apiService,
            ttTranscribeService = ttTranscribeService,
            urlProcessor = urlProcessor,
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
}
