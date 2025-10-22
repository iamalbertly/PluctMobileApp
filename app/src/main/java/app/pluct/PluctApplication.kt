package app.pluct

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Pluct-Application-01Hilt - Hilt application class
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@HiltAndroidApp
class PluctApplication : Application()