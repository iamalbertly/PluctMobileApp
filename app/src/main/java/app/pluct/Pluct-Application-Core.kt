package app.pluct

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Pluct Application class with Hilt setup for dependency injection.
 * 
 * Why @HiltAndroidApp: Enables Hilt dependency injection throughout the app,
 * providing a clean architecture for managing dependencies and improving testability.
 */
@HiltAndroidApp
class PluctApplication : Application()
