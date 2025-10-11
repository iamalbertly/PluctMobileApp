package app.pluct.di

import android.content.Context
import app.pluct.data.database.PluctDatabase
import app.pluct.data.dao.OutputArtifactDao
import app.pluct.data.dao.TranscriptDao
import app.pluct.data.dao.VideoItemDao
import app.pluct.data.service.VideoMetadataService
import app.pluct.data.service.VideoMetadataExtractor
import app.pluct.data.service.HuggingFaceTranscriptionService
import app.pluct.data.manager.PluctTranscriptionManagerCoordinator
import app.pluct.data.manager.UserManager
import app.pluct.data.provider.PluctHuggingFaceProviderCoordinator
import app.pluct.data.service.ApiService
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

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
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
    fun provideHuggingFaceTranscriptionService(
        provider: PluctHuggingFaceProviderCoordinator
    ): HuggingFaceTranscriptionService {
        return HuggingFaceTranscriptionService(provider)
    }
    
    @Provides
    @Singleton
    fun providePluctTranscriptionManagerCoordinator(
        @ApplicationContext context: Context,
        huggingFaceService: HuggingFaceTranscriptionService
    ): PluctTranscriptionManagerCoordinator {
        return PluctTranscriptionManagerCoordinator(context, huggingFaceService)
    }
    
    @Provides
    @Singleton
    fun providePluctHuggingFaceProviderCoordinator(): PluctHuggingFaceProviderCoordinator {
        return PluctHuggingFaceProviderCoordinator()
    }
    
    @Provides
    @Singleton
    fun provideUserManager(
        @ApplicationContext context: Context,
        apiService: ApiService
    ): UserManager {
        return UserManager(context, apiService)
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Log API calls for debugging
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
    fun provideApiService(okHttpClient: OkHttpClient, moshi: Moshi): ApiService {
        // We need a dummy base URL for Retrofit, even though we use full URLs in the interface.
        return Retrofit.Builder()
            .baseUrl("https://placeholder.com/") 
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}

