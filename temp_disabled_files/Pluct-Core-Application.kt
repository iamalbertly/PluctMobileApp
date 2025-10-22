package app.pluct

import android.app.Application
import app.pluct.config.AppConfig
import dagger.hilt.android.HiltAndroidApp

/**
 * Pluct Application class with Hilt setup for dependency injection.
 * 
 * Why @HiltAndroidApp: Enables Hilt dependency injection throughout the app,
 * providing a clean architecture for managing dependencies and improving testability.
 */
@HiltAndroidApp
class PluctApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AppConfig early so DI providers depending on it are safe
        AppConfig.initialize(this)
    }
}
