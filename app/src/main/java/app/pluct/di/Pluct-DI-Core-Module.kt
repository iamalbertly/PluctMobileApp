package app.pluct.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Pluct Core DI Module - Main module that includes all sub-modules
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Simplified to just include other modules for better organization
 */
@Module(includes = [
    PluctDIDatabaseModule::class,
    PluctDINetworkModule::class,
    PluctDIServicesModule::class
])
@InstallIn(SingletonComponent::class)
object PluctDICoreModule
