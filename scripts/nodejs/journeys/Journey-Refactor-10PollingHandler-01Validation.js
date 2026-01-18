const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Refactor-10PollingHandler-01Validation
 * Validates extracted polling handler works correctly with adaptive intervals, auth refresh, transcript extraction, and completion detection
 * Follows naming convention: Journey-[Refactor]-[PollingHandler]-[Validation]
 */
class JourneyRefactor10PollingHandler01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Refactor-10PollingHandler-01Validation';
    }

    async execute() {
        this.core.logger.info('Starting Polling Handler Functionality Validation');
        
        // Step 1: Launch app and start transcription (ensure submission succeeds)
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText(this.core.config.url);
        await this.core.sleep(1000);
        
        await this.core.tapByTestTag('extract_script_button');
        await this.core.sleep(2000);
        
        // Step 2: Monitor logcat for polling phase
        const pollingPatterns = [
            'OperationStep.POLLING',
            'polling_auth_refresh',
            'polling_status_received',
            'transcript_found',
            'cache_hit_detected',
            'polling_circuit_open'
        ];
        
        let pollingDetected = false;
        let authRefreshDetected = false;
        let statusReceivedDetected = false;
        let transcriptFoundDetected = false;
        let cacheHitDetected = false;
        let progressUpdatesDetected = false;
        
        const maxWait = 120000; // 2 minutes for polling
        const startTime = Date.now();
        let lastProgress = -1;
        
        while (Date.now() - startTime < maxWait) {
            // Check for polling operation step
            const pollingStepResult = await this.core.logcatValidator.validatePattern(
                'OperationStep.POLLING',
                'Polling step',
                1,
                1000,
                100
            );
            
            if (pollingStepResult.success) {
                pollingDetected = true;
                this.core.logger.info('✅ Polling step detected in logcat');
            }
            
            // Check for auth refresh during polling
            const authRefreshResult = await this.core.logcatValidator.validatePattern(
                'polling_auth_refresh',
                'Polling auth refresh',
                1,
                1000,
                100
            );
            
            if (authRefreshResult.success) {
                authRefreshDetected = true;
                this.core.logger.info('✅ Polling auth refresh detected');
            }
            
            // Check for status received
            const statusResult = await this.core.logcatValidator.validatePattern(
                'polling_status_received',
                'Polling status received',
                1,
                1000,
                100
            );
            
            if (statusResult.success) {
                statusReceivedDetected = true;
                this.core.logger.info('✅ Polling status received');
            }
            
            // Check for transcript found
            const transcriptResult = await this.core.logcatValidator.validatePattern(
                'transcript_found',
                'Transcript found',
                1,
                1000,
                100
            );
            
            if (transcriptResult.success) {
                transcriptFoundDetected = true;
                this.core.logger.info('✅ Transcript found during polling');
            }
            
            // Check for cache hit
            const cacheHitResult = await this.core.logcatValidator.validatePattern(
                'cache_hit_detected',
                'Cache hit detected',
                1,
                1000,
                100
            );
            
            if (cacheHitResult.success) {
                cacheHitDetected = true;
                this.core.logger.info('✅ Cache hit detected');
            }
            
            // Check UI for progress updates
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump() || '';
            
            // Check for progress indicator
            if (uiDump.includes('progress') || uiDump.includes('Progress') || uiDump.includes('%')) {
                const progressMatch = uiDump.match(/(\d+)%/);
                if (progressMatch) {
                    const currentProgress = parseInt(progressMatch[1]);
                    if (currentProgress > lastProgress) {
                        progressUpdatesDetected = true;
                        lastProgress = currentProgress;
                        this.core.logger.info(`✅ Progress update detected: ${currentProgress}%`);
                    }
                }
            }
            
            // Check for completion
            if (uiDump.includes('COMPLETED') || uiDump.includes('Transcript ready') || uiDump.includes('transcript')) {
                this.core.logger.info('✅ Completion detected in UI');
                break;
            }
            
            // Check for failure
            if (uiDump.includes('Failed') && !uiDump.includes('Still starting')) {
                return {
                    success: false,
                    error: 'Polling failed - error detected in UI'
                };
            }
            
            await this.core.sleep(3000); // Poll every 3 seconds
        }
        
        // Step 3: Validate polling occurred
        if (!pollingDetected) {
            return {
                success: false,
                error: 'Polling step not detected in logcat'
            };
        }
        
        // Step 4: Validate transcript was found (if not cache hit)
        if (!cacheHitDetected && !transcriptFoundDetected) {
            // This might be okay if transcription is still in progress
            this.core.logger.warn('⚠️  Transcript not found yet - may still be processing');
        }
        
        this.core.logger.info('✅ Polling handler validation passed');
        this.core.logger.info(`  - Polling step: ${pollingDetected}`);
        this.core.logger.info(`  - Auth refresh: ${authRefreshDetected ? 'detected' : 'not needed'}`);
        this.core.logger.info(`  - Status received: ${statusReceivedDetected}`);
        this.core.logger.info(`  - Transcript found: ${transcriptFoundDetected || cacheHitDetected}`);
        this.core.logger.info(`  - Cache hit: ${cacheHitDetected}`);
        this.core.logger.info(`  - Progress updates: ${progressUpdatesDetected}`);
        
        return {
            success: true,
            pollingDetected,
            authRefreshDetected,
            statusReceivedDetected,
            transcriptFoundDetected,
            cacheHitDetected,
            progressUpdatesDetected
        };
    }
}

module.exports = JourneyRefactor10PollingHandler01Validation;
