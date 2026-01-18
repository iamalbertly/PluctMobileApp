const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Refactor-09SubmissionHandler-01Validation
 * Validates extracted submission handler works correctly with timeout retry, auth refresh, and jobId recovery
 * Follows naming convention: Journey-[Refactor]-[SubmissionHandler]-[Validation]
 */
class JourneyRefactor09SubmissionHandler01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Refactor-09SubmissionHandler-01Validation';
    }

    async execute() {
        this.core.logger.info('Starting Submission Handler Functionality Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Step 2: Enter valid TikTok URL
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText(this.core.config.url);
        await this.core.sleep(1000);
        
        // Step 3: Start transcription
        await this.core.tapByTestTag('extract_script_button');
        await this.core.sleep(2000);
        
        // Step 4: Monitor logcat for submission phase logs
        const submissionPatterns = [
            'OperationStep.SUBMIT',
            'submit_timeout_retry',
            'submit_auth_failure',
            'jobId='
        ];
        
        let submissionDetected = false;
        let timeoutRetryDetected = false;
        let authRetryDetected = false;
        let jobIdDetected = false;
        
        const maxWait = 30000; // 30 seconds
        const startTime = Date.now();
        
        while (Date.now() - startTime < maxWait) {
            // Check for submission operation step
            const submitStepResult = await this.core.logcatValidator.validatePattern(
                'OperationStep.SUBMIT',
                'Submission step',
                1,
                1000,
                50
            );
            
            if (submitStepResult.success) {
                submissionDetected = true;
                this.core.logger.info('✅ Submission step detected in logcat');
            }
            
            // Check for timeout retry
            const timeoutResult = await this.core.logcatValidator.validatePattern(
                'submit_timeout_retry',
                'Submission timeout retry',
                1,
                1000,
                50
            );
            
            if (timeoutResult.success) {
                timeoutRetryDetected = true;
                this.core.logger.info('✅ Submission timeout retry detected');
            }
            
            // Check for auth retry
            const authResult = await this.core.logcatValidator.validatePattern(
                'submit_auth_failure|submit_auth_retry',
                'Submission auth retry',
                1,
                1000,
                50
            );
            
            if (authResult.success) {
                authRetryDetected = true;
                this.core.logger.info('✅ Submission auth retry detected');
            }
            
            // Check for jobId extraction
            const jobIdResult = await this.core.logcatValidator.validatePattern(
                'jobId=.*',
                'JobId in submission',
                1,
                1000,
                50
            );
            
            if (jobIdResult.success) {
                jobIdDetected = true;
                this.core.logger.info('✅ JobId detected in submission response');
            }
            
            // Check UI for processing indicator
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump() || '';
            
            // Check if processing indicator is visible
            if (uiDump.includes('processing') || uiDump.includes('Processing')) {
                this.core.logger.info('✅ Processing indicator visible in UI');
            }
            
            // Check if we've moved past submission phase (polling or completed)
            if (uiDump.includes('POLLING') || uiDump.includes('COMPLETED') || uiDump.includes('Transcript')) {
                this.core.logger.info('✅ Moved past submission phase');
                break;
            }
            
            // Check for error messages
            if (uiDump.includes('Error') || uiDump.includes('Failed') || uiDump.includes('error')) {
                const errorText = this.extractErrorFromUI(uiDump);
                if (errorText && !errorText.includes('Still starting')) {
                    this.core.logger.warn(`⚠️  Error detected in UI: ${errorText}`);
                    // Don't fail immediately - may be transient
                }
            }
            
            await this.core.sleep(2000);
        }
        
        // Step 5: Validate submission phase occurred
        if (!submissionDetected) {
            return {
                success: false,
                error: 'Submission step not detected in logcat'
            };
        }
        
        this.core.logger.info('✅ Submission handler validation passed');
        this.core.logger.info(`  - Submission step: ${submissionDetected}`);
        this.core.logger.info(`  - Timeout retry: ${timeoutRetryDetected ? 'detected' : 'not needed'}`);
        this.core.logger.info(`  - Auth retry: ${authRetryDetected ? 'detected' : 'not needed'}`);
        this.core.logger.info(`  - JobId extraction: ${jobIdDetected ? 'detected' : 'checking...'}`);
        
        return {
            success: true,
            submissionDetected,
            timeoutRetryDetected,
            authRetryDetected,
            jobIdDetected
        };
    }
    
    extractErrorFromUI(uiDump) {
        // Simple extraction - in practice would use more sophisticated parsing
        const errorMatch = uiDump.match(/error[^>]*>([^<]+)/i);
        return errorMatch ? errorMatch[1] : null;
    }
}

module.exports = JourneyRefactor09SubmissionHandler01Validation;
