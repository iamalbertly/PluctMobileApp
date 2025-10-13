package app.pluct.api

import app.pluct.config.AppConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object EngineApiProvider {
    val instance: EngineApi by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            
        Retrofit.Builder()
            .baseUrl(AppConfig.engineBase)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EngineApi::class.java)
    }
}
