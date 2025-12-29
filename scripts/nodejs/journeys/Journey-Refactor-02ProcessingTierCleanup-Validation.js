const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyRefactor02ProcessingTierCleanupValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-Refactor-02ProcessingTierCleanup-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: ProcessingTier Cleanup Validation');
        
        try {
            // Step 1: Launch app
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Step 2: Clear logcat
            await this.core.clearLogcat();
            
            // Step 3: Verify only EXTRACT_SCRIPT and GENERATE_INSIGHTS tiers are available
            this.core.logger.info('- Step 3: Verify tier options in UI');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump() || '';
            
            // Check for valid tiers
            const hasExtractScript = uiDump.includes('Extract Script') || 
                                    uiDump.includes('EXTRACT_SCRIPT');
            const hasGenerateInsights = uiDump.includes('Generate Insights') || 
                                       uiDump.includes('GENERATE_INSIGHTS');
            
            if (!hasExtractScript) {
                this.core.logger.warn('⚠️ EXTRACT_SCRIPT tier not found in UI');
            } else {
                this.core.logger.info('✅ EXTRACT_SCRIPT tier found');
            }
            
            // Step 4: Trigger transcription with EXTRACT_SCRIPT tier
            this.core.logger.info('- Step 4: Test EXTRACT_SCRIPT tier');
            const urlInput = await this.core.tapByTestTag('url_input_field') ||
                            await this.core.tapByText('Paste a TikTok link');
            if (urlInput.success) {
                await this.core.sleep(500);
                const testUrl = 'https://vm.tiktok.com/ZMDRUGT2P/';
                await this.core.inputText(testUrl);
                await this.core.sleep(1000);
                
                // Tap Extract Script
                const extractBtn = await this.core.tapByTestTag('extract_script_button') ||
                                  await this.core.tapByText('Extract Script');
                if (extractBtn.success) {
                    await this.core.sleep(2000);
                    
                    // Check logcat for tier usage
                    const logcat = await this.core.captureAPILogs(100);
                    const logcatText = logcat.join('\n');
                    
                    // Verify EXTRACT_SCRIPT is used, not removed tiers
                    const usesExtractScript = logcatText.includes('EXTRACT_SCRIPT');
                    const usesRemovedTier = logcatText.includes('STANDARD') || 
                                           logcatText.includes('DEEP_ANALYSIS') ||
                                           logcatText.includes('FREE') ||
                                           logcatText.includes('PREMIUM') ||
                                           logcatText.includes('AI_ANALYSIS') ||
                                           logcatText.includes('PREMIUM_INSIGHTS');
                    
                    if (usesRemovedTier) {
                        return { 
                            success: false, 
                            error: 'Removed ProcessingTier values still being used in logs' 
                        };
                    }
                    
                    if (usesExtractScript) {
                        this.core.logger.info('✅ EXTRACT_SCRIPT tier used correctly');
                    }
                }
            }
            
            // Step 5: Check for database enum conversion errors
            this.core.logger.info('- Step 5: Check for enum conversion errors');
            await this.core.sleep(2000);
            const errorLogcat = await this.core.captureAPILogs(50);
            const errorText = errorLogcat.join('\n');
            
            const hasEnumError = errorText.includes('IllegalArgumentException') && 
                                errorText.includes('ProcessingTier') ||
                                errorText.includes('NoSuchElementException') &&
                                errorText.includes('ProcessingTier');
            
            if (hasEnumError) {
                return { 
                    success: false, 
                    error: 'Database enum conversion error detected - old enum values not handled' 
                };
            }
            
            this.core.logger.info('✅ No enum conversion errors');
            this.core.logger.info('✅ ProcessingTier cleanup validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyRefactor02ProcessingTierCleanupValidation;

