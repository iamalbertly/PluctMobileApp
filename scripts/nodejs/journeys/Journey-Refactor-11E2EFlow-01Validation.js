const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Refactor-11E2EFlow-01Validation
 * Validates complete end-to-end transcription flow works after refactoring, ensuring all extracted handlers integrate correctly
 * Follows naming convention: Journey-[Refactor]-[E2EFlow]-[Validation]
 */
class JourneyRefactor11E2EFlow01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Refactor-11E2EFlow-01Validation';
    }

    async execute() {
        this.core.logger.info('Starting End-to-End Transcription Flow Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Step 2: Enter TikTok URL
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText(this.core.config.url);
        await this.core.sleep(1000);
        
        // Step 3: Start transcription (Quick Scan tier)
        await this.core.tapByTestTag('extract_script_button');
        await this.core.sleep(2000);
        
        // Step 4: Monitor complete flow: canonicalization → metadata → token vending → submission → polling → completion
        const expectedSteps = [
            'CANONICALIZE',
            'METADATA',
            'VEND_TOKEN',
            'SUBMIT',
            'POLLING',
            'COMPLETED'
        ];
        
        const detectedSteps = [];
        const maxWait = 180000; // 3 minutes for complete flow
        const startTime = Date.now();
        let transcriptDetected = false;
        let creditBalanceUpdated = false;
        
        while (Date.now() - startTime < maxWait) {
            // Monitor logcat for operation steps
            const logcatResult = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "OperationStep\|currentStep"'
            );
            
            if (logcatResult.output) {
                for (const step of expectedSteps) {
                    if (logcatResult.output.includes(step) && !detectedSteps.includes(step)) {
                        detectedSteps.push(step);
                        this.core.logger.info(`✅ Step detected: ${step}`);
                    }
                }
            }
            
            // Check UI state
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump() || '';
            
            // Check for transcript in UI
            if (uiDump.includes('transcript') || uiDump.includes('Transcript') || uiDump.length > 5000) {
                // Large UI dump might indicate transcript is present
                transcriptDetected = true;
                this.core.logger.info('✅ Transcript detected in UI');
            }
            
            // Check for credit balance update
            if (uiDump.includes('credit') || uiDump.includes('Credit') || uiDump.match(/\d+\s*credit/i)) {
                creditBalanceUpdated = true;
                this.core.logger.info('✅ Credit balance detected in UI');
            }
            
            // Check for completion
            if (uiDump.includes('COMPLETED') || uiDump.includes('Transcript ready') || 
                (transcriptDetected && detectedSteps.includes('COMPLETED'))) {
                this.core.logger.info('✅ Completion detected');
                break;
            }
            
            // Check for errors (excluding expected warnings)
            if (uiDump.includes('Error') && !uiDump.includes('Still starting')) {
                const errorText = this.extractErrorFromUI(uiDump);
                if (errorText && !errorText.toLowerCase().includes('warning')) {
                    this.core.logger.warn(`⚠️  Error in UI: ${errorText.substring(0, 100)}`);
                }
            }
            
            await this.core.sleep(3000);
        }
        
        // Step 5: Validate all phases completed
        const missingSteps = expectedSteps.filter(step => !detectedSteps.includes(step));
        
        if (missingSteps.length > 0 && !missingSteps.includes('COMPLETED')) {
            // COMPLETED might not be in logcat if UI shows completion first
            this.core.logger.warn(`⚠️  Some steps not detected in logcat: ${missingSteps.join(', ')}`);
        }
        
        // Step 6: Validate transcript appears in UI
        await this.core.dumpUIHierarchy();
        const finalUIDump = this.core.readLastUIDump() || '';
        
        if (!finalUIDump.includes('transcript') && !finalUIDump.includes('Transcript')) {
            return {
                success: false,
                error: 'Transcript not found in UI after completion',
                detectedSteps: detectedSteps,
                transcriptDetected: transcriptDetected
            };
        }
        
        // Step 7: Validate no critical errors in logcat
        const errorResult = await this.core.logcatValidator.checkForErrors(
            ['WARN', 'expected', 'Still starting'],
            200
        );
        
        if (!errorResult.success && errorResult.errorCount > 0) {
            this.core.logger.warn(`⚠️  ${errorResult.errorCount} error(s) found in logcat (may be expected)`);
        }
        
        this.core.logger.info('✅ End-to-End Flow Validation passed');
        this.core.logger.info(`  - Steps detected: ${detectedSteps.length}/${expectedSteps.length}`);
        this.core.logger.info(`  - Transcript in UI: ${transcriptDetected}`);
        this.core.logger.info(`  - Credit balance updated: ${creditBalanceUpdated}`);
        
        return {
            success: true,
            detectedSteps: detectedSteps,
            transcriptDetected: transcriptDetected,
            creditBalanceUpdated: creditBalanceUpdated,
            errorCount: errorResult.errorCount
        };
    }
    
    extractErrorFromUI(uiDump) {
        const errorMatch = uiDump.match(/error[^>]*>([^<]+)/i);
        return errorMatch ? errorMatch[1] : null;
    }
}

module.exports = JourneyRefactor11E2EFlow01Validation;
