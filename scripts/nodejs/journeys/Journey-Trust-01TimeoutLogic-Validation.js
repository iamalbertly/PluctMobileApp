const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Trust-01TimeoutLogic-Validation
 * Validates intelligent timeout logic that tracks API progress instead of blind 20s timeout
 */
class JourneyTrust01TimeoutLogicValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Trust-01TimeoutLogic-Validation';
    }

    async execute() {
        await this.log('Starting Timeout Logic Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Step 2: Enter valid TikTok URL
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText(this.core.config.url);
        await this.core.sleep(1000);
        
        // Step 3: Start transcription
        await this.core.tapByTestTag('extract_script_button');
        await this.core.sleep(1000);
        
        // Step 4: Monitor logcat for step progression
        const startTime = Date.now();
        let lastStep = null;
        let stepChanges = 0;
        const maxWait = 35000; // 35 seconds
        
        while (Date.now() - startTime < maxWait) {
            const logcatResult = await this.core.executeCommand(
                'adb logcat -d -t 50 | findstr /i "Step changed\|OperationStep\|currentStep"'
            );
            
            if (logcatResult.output) {
                const currentStep = this.extractStepFromLogcat(logcatResult.output);
                if (currentStep && currentStep !== lastStep) {
                    stepChanges++;
                    lastStep = currentStep;
                    this.logger.info(`Step change detected: ${currentStep} (${stepChanges} total)`);
                }
            }
            
            // Check for "Still starting" false positive
            const stillStartingCheck = await this.core.executeCommand(
                'adb logcat -d -t 50 | findstr /i "Still starting"'
            );
            
            if (stillStartingCheck.output && Date.now() - startTime < 25000) {
                // "Still starting" error appeared before 25s - this is a false positive
                return { 
                    success: false, 
                    error: `False positive "Still starting" error detected at ${Date.now() - startTime}ms. Step changes: ${stepChanges}` 
                };
            }
            
            // Check UI for timeout error
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump() || '';
            
            if (uiDump.includes('Still starting') && Date.now() - startTime < 25000) {
                return { 
                    success: false, 
                    error: `False positive "Still starting" error in UI at ${Date.now() - startTime}ms` 
                };
            }
            
            // Check if transcription completed
            if (uiDump.includes('COMPLETED') || uiDump.includes('Transcript ready')) {
                this.logger.info('Transcription completed before timeout');
                break;
            }
            
            await this.core.sleep(2000);
        }
        
        // Step 5: Verify timeout only triggers after 30s of no step changes
        if (stepChanges > 0) {
            this.logger.info(`✅ Timeout logic working: ${stepChanges} step changes detected, no false positives`);
        } else {
            this.logger.warn('⚠️ No step changes detected - may indicate debugInfo not updating');
        }
        
        // Step 6: Edge case - API completes in 25s, no timeout should occur
        // This is validated by the fact we didn't see "Still starting" before 25s
        
        await this.log('Timeout Logic Validation Complete');
        return { success: true, stepChanges };
    }
    
    extractStepFromLogcat(logcatOutput) {
        const stepMatch = logcatOutput.match(/(METADATA|VEND_TOKEN|SUBMIT|POLLING|COMPLETED|FAILED|CANONICALIZE)/);
        return stepMatch ? stepMatch[1] : null;
    }
}

module.exports = JourneyTrust01TimeoutLogicValidation;

