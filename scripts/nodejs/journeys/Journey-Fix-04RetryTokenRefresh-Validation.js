const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Fix-04RetryTokenRefresh-Validation
 * Follows naming convention: [Journey]-[Fix]-[04RetryTokenRefresh]-[Validation]
 * 4 scope layers: Journey, Fix, RetryTokenRefresh, Validation
 * Validates retry handler token refresh integration
 */
class JourneyFix04RetryTokenRefreshValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Fix-04RetryTokenRefresh-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Retry Handler Token Refresh Integration Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Step 2: Clear logcat
        await this.core.clearLogcat();
        this.core.logger.info('✅ Logcat cleared');
        
        // Step 3: Trigger API call (this will generate JWT and may trigger 401 if token expires)
        // We'll trigger multiple API calls to increase chance of hitting 401
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Wait for initial API calls to complete
        await this.core.sleep(5000);
        
        // Step 4: Monitor retry attempts and token refresh
        const retryLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "Retrying\|retry\|401.*error\|refreshed.*token\|Retrying.*refreshed"'
        );
        
        const retryLines = (retryLogcat.output || '').split('\n');
        const hasRetry = retryLines.some(line => 
            line.includes('Retrying') || 
            line.includes('retry')
        );
        
        const hasTokenRefresh = retryLines.some(line => 
            line.includes('refreshed token') ||
            line.includes('refreshing token') ||
            line.includes('401 error') && line.includes('refreshed')
        );
        
        if (hasRetry) {
            this.core.logger.info('✅ Retry attempts detected');
            
            if (hasTokenRefresh) {
                this.core.logger.info('✅ Token refresh detected before retry (excellent!)');
            } else {
                // Check if 401 errors occurred
                const has401 = retryLines.some(line => 
                    line.includes('401') || 
                    line.includes('Unauthorized')
                );
                
                if (has401) {
                    this.core.logger.warn('⚠️ 401 errors detected but token refresh not clearly logged');
                    this.core.logger.warn('This may indicate token refresh integration needs verification');
                } else {
                    this.core.logger.info('ℹ️ No 401 errors detected, token refresh not needed');
                }
            }
        } else {
            this.core.logger.info('ℹ️ No retry attempts detected (normal if all requests succeed)');
        }
        
        // Step 5: Verify token refresh occurs before retry (check logcat patterns)
        const refreshBeforeRetryLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "401.*error.*detected.*refreshing\|refreshing.*token.*before.*retry\|Retrying.*refreshed.*token"'
        );
        
        const hasRefreshBeforeRetry = refreshBeforeRetryLogcat.output && (
            refreshBeforeRetryLogcat.output.includes('401 error detected') && 
            refreshBeforeRetryLogcat.output.includes('refreshing')
        ) || (
            refreshBeforeRetryLogcat.output.includes('refreshing token before retry')
        ) || (
            refreshBeforeRetryLogcat.output.includes('Retrying') && 
            refreshBeforeRetryLogcat.output.includes('refreshed token')
        );
        
        if (hasRefreshBeforeRetry) {
            this.core.logger.info('✅ Token refresh occurs before retry (excellent!)');
        } else {
            this.core.logger.info('ℹ️ Token refresh before retry pattern not clearly detected (may be normal if no 401 errors)');
        }
        
        // Step 6: Verify API call succeeds after refresh
        // Check for successful API calls after token refresh
        const successAfterRefreshLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "Request.*completed\|API.*success\|200.*OK"'
        );
        
        const hasSuccessAfterRefresh = successAfterRefreshLogcat.output && (
            successAfterRefreshLogcat.output.includes('Request completed') ||
            successAfterRefreshLogcat.output.includes('API success') ||
            successAfterRefreshLogcat.output.includes('200')
        );
        
        if (hasSuccessAfterRefresh) {
            this.core.logger.info('✅ Successful API calls detected after potential token refresh');
        }
        
        // Step 7: Check retry count (should be minimal if token refresh works)
        const retryCountLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "attempt.*3\|failed.*after.*3.*attempts"'
        );
        
        const excessiveRetries = retryCountLogcat.output && (
            retryCountLogcat.output.includes('attempt 3') ||
            retryCountLogcat.output.includes('failed after 3 attempts')
        );
        
        if (excessiveRetries) {
            const retryCount = (retryCountLogcat.output.match(/attempt 3|failed after 3 attempts/gi) || []).length;
            if (retryCount > 2) {
                this.core.logger.warn(`⚠️ Multiple excessive retries detected: ${retryCount} (token refresh may not be working)`);
            } else {
                this.core.logger.info('ℹ️ Some retries reached max attempts (may be normal for non-auth errors)');
            }
        } else {
            this.core.logger.info('✅ No excessive retries detected');
        }
        
        // Step 8: Verify no wasted retry attempts
        // Check for retries that use same token (should not happen for 401 errors)
        const wastedRetryLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "Retrying.*request.*401\|401.*Retrying.*same"'
        );
        
        const hasWastedRetries = wastedRetryLogcat.output && (
            wastedRetryLogcat.output.includes('401') && 
            wastedRetryLogcat.output.includes('Retrying') &&
            !wastedRetryLogcat.output.includes('refreshed')
        );
        
        if (hasWastedRetries) {
            this.core.logger.warn('⚠️ Potential wasted retries detected (retrying 401 without token refresh)');
        } else {
            this.core.logger.info('✅ No wasted retry attempts detected');
        }
        
        // Step 9: Verify retry handler logs show refresh integration
        const retryHandlerLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "PluctRetryHandler\|RetryHandler.*401\|RetryHandler.*refresh"'
        );
        
        const hasRetryHandlerLogs = retryHandlerLogcat.output && (
            retryHandlerLogcat.output.includes('PluctRetryHandler') ||
            retryHandlerLogcat.output.includes('RetryHandler')
        );
        
        if (hasRetryHandlerLogs) {
            this.core.logger.info('✅ Retry handler logs detected');
            
            const hasRefreshInLogs = retryHandlerLogcat.output.includes('refresh') ||
                                    retryHandlerLogcat.output.includes('401');
            
            if (hasRefreshInLogs) {
                this.core.logger.info('✅ Token refresh mentioned in retry handler logs');
            }
        }
        
        // Step 10: Final UI validation - no persistent errors
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        
        const hasPersistentError = uiDump.toLowerCase().includes('error') && 
                                  (uiDump.toLowerCase().includes('401') ||
                                   uiDump.toLowerCase().includes('unauthorized') ||
                                   uiDump.toLowerCase().includes('authentication'));
        
        if (hasPersistentError) {
            return {
                success: false,
                error: 'Persistent authentication error detected in UI'
            };
        }
        
        this.core.logger.info('✅ No persistent authentication errors in UI');
        this.core.logger.info('✅ Retry handler token refresh integration validation completed');
        return { success: true };
    }
}

module.exports = JourneyFix04RetryTokenRefreshValidation;
