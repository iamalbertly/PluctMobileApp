package app.pluct.di

import android.content.Context
import app.pluct.data.database.PluctDatabase
import app.pluct.data.dao.OutputArtifactDao
import app.pluct.data.dao.TranscriptDao
import app.pluct.data.dao.VideoItemDao
import app.pluct.data.service.VideoMetadataService
import app.pluct.data.service.VideoMetadataExtractor
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
}

