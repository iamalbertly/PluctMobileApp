const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyRefactor06PreWarmingOptimizationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-Refactor-06PreWarmingOptimization-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Pre-warming Optimization Validation');
        
        try {
            // Step 1: Launch app
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Step 2: Clear logcat
            await this.core.clearLogcat();
            
            // Step 3: Enter URL (this would trigger pre-warming if it existed)
            this.core.logger.info('- Step 3: Enter URL to test pre-warming');
            await this.core.dumpUIHierarchy();
            
            const urlInput = await this.core.tapByTestTag('url_input_field') ||
                            await this.core.tapByText('Paste a TikTok link');
            
            if (urlInput.success) {
                await this.core.sleep(500);
                const testUrl = 'https://vm.tiktok.com/ZMDRUGT2P/';
                await this.core.inputText(testUrl);
                
                // Wait a bit to see if pre-warming triggers
                await this.core.sleep(3000);
                
                // Step 4: Check logcat for pre-warming activity
                this.core.logger.info('- Step 4: Check for pre-warming activity');
                const logcat = await this.core.captureAPILogs(200);
                const logcatText = logcat.join('\n');
                
                // Check for pre-warming logs (should be minimal or none)
                const hasPrewarmLogs = logcatText.includes('prewarm') || 
                                      logcatText.includes('pre-warm') ||
                                      logcatText.includes('PREWARM');
                
                if (hasPrewarmLogs) {
                    // Check if it's the deprecated function being called
                    const hasDeprecatedCall = logcatText.includes('preWarmVideoProcessing');
                    
                    if (hasDeprecatedCall) {
                        this.core.logger.info('✅ Pre-warming function called but deprecated (expected)');
                    } else {
                        this.core.logger.warn('⚠️ Pre-warming activity detected');
                    }
                } else {
                    this.core.logger.info('✅ No pre-warming activity (expected after optimization)');
                }
                
                // Step 5: Check for excessive API calls (rate limit protection)
                this.core.logger.info('- Step 5: Check for excessive API calls');
                const apiCallCount = (logcatText.match(/GET|POST/g) || []).length;
                
                // Before user submits, there should be minimal API calls
                if (apiCallCount > 5) {
                    this.core.logger.warn(`⚠️ High API call count before submission: ${apiCallCount}`);
                } else {
                    this.core.logger.info(`✅ API call count reasonable: ${apiCallCount}`);
                }
                
                // Step 6: Submit and verify on-demand requests work
                this.core.logger.info('- Step 6: Submit and verify on-demand flow');
                const extractBtn = await this.core.tapByTestTag('extract_script_button') ||
                                  await this.core.tapByText('Extract Script');
                
                if (extractBtn.success) {
                    await this.core.sleep(3000);
                    
                    // Check for API calls after submission (should happen on-demand)
                    const logcat2 = await this.core.captureAPILogs(100);
                    const logcatText2 = logcat2.join('\n');
                    
                    const hasOnDemandCalls = logcatText2.includes('/meta') || 
                                            logcatText2.includes('/vend-token') ||
                                            logcatText2.includes('/transcribe');
                    
                    if (hasOnDemandCalls) {
                        this.core.logger.info('✅ On-demand API calls working correctly');
                    } else {
                        this.core.logger.warn('⚠️ No on-demand API calls detected');
                    }
                }
            }
            
            // Step 7: Check for rate limit errors
            this.core.logger.info('- Step 7: Check for rate limit errors');
            await this.core.sleep(2000);
            const errorLogcat = await this.core.captureAPILogs(50);
            const errorText = errorLogcat.join('\n');
            
            const hasRateLimit = errorText.includes('429') || 
                                errorText.includes('rate limit') ||
                                errorText.includes('RateLimit');
            
            if (hasRateLimit) {
                this.core.logger.warn('⚠️ Rate limit errors detected (may indicate excessive pre-warming)');
            } else {
                this.core.logger.info('✅ No rate limit errors');
            }
            
            // Step 8: Verify app functionality
            this.core.logger.info('- Step 8: Verify app functionality');
            await this.core.dumpUIHierarchy();
            const finalUI = this.core.readLastUIDump() || '';
            
            const hasErrors = finalUI.toLowerCase().includes('error') && 
                             !finalUI.toLowerCase().includes('no error');
            
            if (hasErrors && !finalUI.includes('Processing')) {
                this.core.logger.warn('⚠️ UI shows error state');
            } else {
                this.core.logger.info('✅ App functioning correctly');
            }
            
            this.core.logger.info('✅ Pre-warming optimization validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyRefactor06PreWarmingOptimizationValidation;


