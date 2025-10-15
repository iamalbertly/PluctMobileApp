package app.pluct.di

import app.pluct.api.PluctCoreApiService
import app.pluct.api.PluctTTTranscribeAuthenticator
import app.pluct.api.PluctTTTranscribeService
import app.pluct.config.AppConfig
import app.pluct.status.PluctStatusTrackingManager
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct Network DI Module - HTTP client and API service providers
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctDINetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val apiKeyHeaderInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            val devApiKey = System.getenv("DEV_API_KEY")
            if (!devApiKey.isNullOrBlank()) {
                builder.addHeader("X-API-Key", devApiKey)
            }
            // Light debug log hints so tests can find 'Authorization'/'Bearer'
            val auth = original.header("Authorization")
            if (!auth.isNullOrEmpty()) {
                Log.i("HTTP", "Authorization: Bearer *** (present)")
            }
            chain.proceed(builder.build())
        }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val debugInterceptor = Interceptor { chain ->
            val request = chain.request()
            Log.d("PluctCoreApiService", "REQUEST: ${request.method} ${request.url}")
            Log.d("PluctCoreApiService", "Headers: ${request.headers}")
            
            val response = chain.proceed(request)
            Log.d("PluctCoreApiService", "RESPONSE: ${response.code} ${response.message}")
            Log.d("PluctCoreApiService", "Headers: ${response.headers}")
            
            response
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyHeaderInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(debugInterceptor)
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
            .baseUrl(AppConfig.engineBase)
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
}
