package app.pluct.api

import app.pluct.config.AppConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

object EngineApiProvider {
    val instance: EngineApi by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val apiKeyHeaderInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            // Pull from env when available (debug/local). Do not hardcode.
            val devApiKey = System.getenv("DEV_API_KEY")
            if (!devApiKey.isNullOrBlank()) {
                builder.addHeader("X-API-Key", devApiKey)
            }
            chain.proceed(builder.build())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(apiKeyHeaderInterceptor)
            .build()
            
        Retrofit.Builder()
            .baseUrl(AppConfig.engineBase)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(EngineApi::class.java)
    }
}
