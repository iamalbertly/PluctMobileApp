package app.pluct.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import app.pluct.core.credit.PluctCoreCredit01AtomicReservation01Service
import app.pluct.services.PluctCoreAPI01UnifiedService02TokenRefresh01Manager
import app.pluct.services.PluctCoreAPIJWTGenerator
import app.pluct.services.PluctCoreUserIdentification
import javax.inject.Singleton

/**
 * Pluct-Core-TrustFixes-01Module
 * Follows naming convention: [Project]-[Core]-[TrustFixes]-[Module]
 * 4 scope layers: Project, Core, TrustFixes, Module
 * Hilt module for trust fixes services dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object PluctCoreTrustFixesModule {
    
    @Provides
    @Singleton
    fun provideAtomicCreditReservationService(): PluctCoreCredit01AtomicReservation01Service {
        return PluctCoreCredit01AtomicReservation01Service()
    }
    
    @Provides
    @Singleton
    fun provideTokenRefreshManager(
        jwtGenerator: PluctCoreAPIJWTGenerator,
        userIdentification: PluctCoreUserIdentification
    ): PluctCoreAPI01UnifiedService02TokenRefresh01Manager {
        return PluctCoreAPI01UnifiedService02TokenRefresh01Manager(
            jwtGenerator = jwtGenerator,
            userIdentification = userIdentification
        )
    }
    
}

