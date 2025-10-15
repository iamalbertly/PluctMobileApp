package app.pluct.di

import android.content.Context
import app.pluct.data.database.PluctDatabase
import app.pluct.data.dao.OutputArtifactDao
import app.pluct.data.dao.TranscriptDao
import app.pluct.data.dao.VideoItemDao
import app.pluct.data.dao.UserCoinsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pluct Database DI Module - Database and DAO providers
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctDIDatabaseModule {
    
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
}
