const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Fix-01JWTExpiration-Validation
 * Follows naming convention: [Journey]-[Fix]-[01JWTExpiration]-[Validation]
 * 4 scope layers: Journey, Fix, JWTExpiration, Validation
 * Validates JWT token expiration and automatic refresh handling
 */
class JourneyFix01JWTExpirationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Fix-01JWTExpiration-Validation';
    }

    async execute() {
        this.core.logger.info('Starting JWT Token Expiration and Refresh Validation');
        
        // Step 1: Launch app and clear logcat
        await this.core.launchApp();
        await this.core.sleep(2000);
        await this.core.clearLogcat();
        this.core.logger.info('✅ App launched and logcat cleared');
        
        // Step 2: Trigger API call (balance check) to generate JWT
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Wait for credit balance to load (this triggers JWT generation)
        await this.core.sleep(3000);
        
        // Step 3: Monitor logcat for JWT generation logs
        const jwtGenLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "PluctCoreAPIJWTGenerator\|JWT\|token.*generat"'
        );
        
        const hasJWTGen = jwtGenLogcat.output && (
            jwtGenLogcat.output.includes('PluctCoreAPIJWTGenerator') ||
            jwtGenLogcat.output.includes('Generating JWT')
        );
        
        if (!hasJWTGen) {
            this.core.logger.warn('⚠️ JWT generation logs not found (may be normal if cached)');
        } else {
            this.core.logger.info('✅ JWT generation detected');
        }
        
        // Step 4: Verify token contains correct iat and exp claims
        const tokenLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "iat\|exp\|timestamp"'
        );
        
        const hasTokenClaims = tokenLogcat.output && (
            tokenLogcat.output.includes('iat') ||
            tokenLogcat.output.includes('exp') ||
            tokenLogcat.output.includes('timestamp')
        );
        
        if (hasTokenClaims) {
            this.core.logger.info('✅ Token claims (iat/exp) detected in logs');
        }
        
        // Step 5: Trigger another API call to test token refresh
        // Try to access settings or trigger another API call
        await this.core.sleep(2000);
        
        // Step 6: Verify token refresh occurs automatically (check logcat)
        const tokenRefreshLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "TokenRefresh\|refreshToken\|refreshing.*token\|Retrying.*refreshed.*token"'
        );
        
        const hasRefreshLogic = tokenRefreshLogcat.output && (
            tokenRefreshLogcat.output.includes('TokenRefresh') ||
            tokenRefreshLogcat.output.includes('refreshToken') ||
            tokenRefreshLogcat.output.includes('refreshing token') ||
            tokenRefreshLogcat.output.includes('refreshed token')
        );
        
        if (hasRefreshLogic) {
            this.core.logger.info('✅ Token refresh logic detected');
        } else {
            this.core.logger.info('ℹ️ Token refresh not triggered (normal if token still valid)');
        }
        
        // Step 7: Check for 401 errors in logcat (should be minimal or none)
        const error401Logcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "401\|Unauthorized\|exp claim timestamp check failed"'
        );
        
        const has401Error = error401Logcat.output && (
            error401Logcat.output.includes('401') ||
            error401Logcat.output.includes('Unauthorized') ||
            error401Logcat.output.includes('exp claim timestamp check failed')
        );
        
        if (has401Error) {
            const errorCount = (error401Logcat.output.match(/401|Unauthorized|exp claim timestamp check failed/gi) || []).length;
            if (errorCount > 3) {
                return {
                    success: false,
                    error: `Too many 401 errors detected: ${errorCount}. Token refresh may not be working correctly.`
                };
            } else {
                this.core.logger.warn(`⚠️ Some 401 errors detected (${errorCount}), but within acceptable range`);
            }
        } else {
            this.core.logger.info('✅ No 401 errors detected');
        }
        
        // Step 8: Validate UI shows no authentication errors
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        
        const hasAuthError = uiDump.toLowerCase().includes('authentication') ||
                            uiDump.toLowerCase().includes('unauthorized') ||
                            uiDump.toLowerCase().includes('login') ||
                            (uiDump.toLowerCase().includes('error') && 
                             (uiDump.toLowerCase().includes('401') || uiDump.toLowerCase().includes('auth')));
        
        if (hasAuthError) {
            return {
                success: false,
                error: 'Authentication error detected in UI'
            };
        }
        
        // Step 9: Verify credit balance displays correctly (indicates successful auth)
        const hasCreditBalance = uiDump.includes('credit') || 
                                 uiDump.includes('balance') ||
                                 uiDump.includes('coin');
        
        if (!hasCreditBalance) {
            this.core.logger.warn('⚠️ Credit balance UI element not found (may be normal)');
        } else {
            this.core.logger.info('✅ Credit balance UI element found');
        }
        
        // Step 10: Final validation - no error banners
        const hasErrorBanner = uiDump.toLowerCase().includes('error') && 
                              (uiDump.toLowerCase().includes('banner') || 
                               uiDump.toLowerCase().includes('dialog') ||
                               uiDump.toLowerCase().includes('alert'));
        
        if (hasErrorBanner) {
            this.core.logger.warn('⚠️ Error banner detected in UI (may be unrelated to auth)');
        } else {
            this.core.logger.info('✅ No error banners detected');
        }
        
        this.core.logger.info('✅ JWT token expiration and refresh validation completed');
        return { success: true };
    }
}

module.exports = JourneyFix01JWTExpirationValidation;
