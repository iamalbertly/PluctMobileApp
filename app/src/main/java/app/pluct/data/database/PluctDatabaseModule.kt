package app.pluct.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import app.pluct.data.dao.PluctVideoDao
import javax.inject.Singleton

/**
 * Pluct-Data-Database-Module - Hilt module for database dependency injection
 * Provides singleton database and DAO instances
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctDatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PluctDatabase {
        return Room.databaseBuilder(
            context,
            PluctDatabase::class.java,
            PluctDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For version 1, allow destructive migration
            .build()
    }
    
    @Provides
    @Singleton
    fun provideVideoDao(database: PluctDatabase): PluctVideoDao {
        return database.videoDao()
    }
}
