const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-TikTok-Manual-URL-01Transcription - End-to-end test for TikTok transcription via manual URL input
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Tests the complete flow from manual URL input to transcript delivery
 */
class JourneyTikTokManualURL01Transcription extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'TikTokManualURLTranscription';
        this.maxDuration = 160000; // 160 seconds max
        this.startTime = Date.now();
    }

    async execute() {
        this.core.logger.info('🎯 Starting TikTok Manual URL Transcription Journey');
        this.core.logger.info(`Max duration: ${this.maxDuration}ms`);
        
        try {
            // Step 1: Launch app
            await this.launchApp();
            
            // Step 2: Open capture sheet
            await this.openCaptureSheet();
            
            // Step 3: Input TikTok URL manually
            await this.inputTikTokURL();
            
            // Step 4: Start transcription
            await this.startTranscription();
            
            // Step 5: Monitor progress with timeout
            const result = await this.monitorTranscriptionProgress();
            
            if (result.success) {
                this.core.logger.info('🎉 TikTok Manual URL Transcription Journey Completed Successfully!');
                return { 
                    success: true, 
                    message: 'TikTok transcription via manual URL completed successfully',
                    transcript: result.transcript,
                    duration: result.duration
                };
            } else {
                this.core.logger.error(`❌ TikTok Manual URL Transcription Journey Failed: ${result.error}`);
                return { 
                    success: false, 
                    error: result.error,
                    failedStep: result.failedStep,
                    duration: result.duration
                };
            }
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed with exception: ${error.message}`);
            return { 
                success: false, 
                error: `Journey exception: ${error.message}`,
                duration: Date.now() - this.startTime
            };
        }
    }
    
    async launchApp() {
        this.core.logger.info('📱 Step 1: Launching Pluct app...');
        await this.core.launchApp();
        await this.core.sleep(3000); // Give app time to settle
        
        // Verify app launched
        await this.core.dumpUIHierarchy();
        const uiDump = await this.core.getUIHierarchy();
        if (!uiDump.includes('Pluct') && !uiDump.includes('Home')) {
            throw new Error('App did not launch properly');
        }
        
        this.core.logger.info('✅ App launched successfully');
    }
    
    async openCaptureSheet() {
        this.core.logger.info('📝 Step 2: Opening capture sheet...');
        
        // Look for FAB or add button
        await this.core.dumpUIHierarchy();
        const uiDump = await this.core.getUIHierarchy();
        
        // Try to find and tap the FAB
        const fabTap = await this.core.tapByContentDesc('Add video');
        if (!fabTap.success) {
            // Try alternative selectors
            const altFabTap = await this.core.tapByText('Add');
            if (!altFabTap.success) {
                // Look for any button that might open the capture sheet
                const anyButtonTap = await this.core.tapFirstButton();
                if (!anyButtonTap.success) {
                    throw new Error('Could not find capture sheet trigger');
                }
            }
        }
        
        await this.core.sleep(2000); // Wait for capture sheet to appear
        
        // Verify capture sheet opened
        await this.core.dumpUIHierarchy();
        const captureUiDump = await this.core.getUIHierarchy();
        if (!captureUiDump.includes('TikTok URL') && !captureUiDump.includes('url_input')) {
            throw new Error('Capture sheet did not open');
        }
        
        this.core.logger.info('✅ Capture sheet opened successfully');
    }
    
    async inputTikTokURL() {
        this.core.logger.info('🔗 Step 3: Inputting TikTok URL...');
        
        const testUrl = 'https://www.tiktok.com/@testuser/video/1234567890';
        
        // Find and tap the URL input field
        const inputTap = await this.core.tapByContentDesc('url_input');
        if (!inputTap.success) {
            // Try alternative selectors
            const altInputTap = await this.core.tapFirstEditText();
            if (!altInputTap.success) {
                throw new Error('Could not find URL input field');
            }
        }
        
        await this.core.sleep(500); // Brief pause
        
        // Clear any existing text
        await this.core.clearEditText();
        
        // Input the URL
        const inputResult = await this.core.inputText(testUrl);
        if (!inputResult.success) {
            throw new Error(`Failed to input URL: ${inputResult.error}`);
        }
        
        await this.core.sleep(1000); // Wait for input to be processed
        
        this.core.logger.info(`✅ URL inputted: ${testUrl}`);
    }
    
    async startTranscription() {
        this.core.logger.info('🚀 Step 4: Starting transcription...');
        
        // Look for start/process button
        await this.core.dumpUIHierarchy();
        const uiDump = await this.core.getUIHierarchy();
        
        // Try different button selectors
        const startButtonSelectors = [
            'Start Transcription',
            'Process Video',
            'Quick Scan',
            'AI Analysis',
            'Submit',
            'Go'
        ];
        
        let buttonTapped = false;
        for (const selector of startButtonSelectors) {
            const tapResult = await this.core.tapByText(selector);
            if (tapResult.success) {
                buttonTapped = true;
                this.core.logger.info(`✅ Tapped button: ${selector}`);
                break;
            }
        }
        
        if (!buttonTapped) {
            // Try to find any button in the capture sheet
            const anyButtonTap = await this.core.tapFirstButton();
            if (!anyButtonTap.success) {
                throw new Error('Could not find transcription start button');
            }
        }
        
        await this.core.sleep(3000); // Wait for transcription to start
        
        this.core.logger.info('✅ Transcription start initiated');
    }
    
    async monitorTranscriptionProgress() {
        this.core.logger.info('⏱️ Step 5: Monitoring transcription progress...');
        
        let lastProgressUpdate = Date.now();
        const progressTimeout = 30000; // 30 seconds per step
        
        while (Date.now() - this.startTime < this.maxDuration) {
            const elapsed = Date.now() - this.startTime;
            const remaining = this.maxDuration - elapsed;
            
            this.core.logger.info(`Progress check: ${elapsed}ms elapsed, ${remaining}ms remaining`);
            
            // Check for completion
            await this.core.dumpUIHierarchy();
            const uiDump = await this.core.getUIHierarchy();
            
            if (uiDump.includes('Transcription Complete') || 
                uiDump.includes('transcript') ||
                uiDump.includes('Job ID:')) {
                this.core.logger.info('✅ Transcription completed successfully');
                return {
                    success: true,
                    transcript: this.extractTranscript(uiDump),
                    duration: elapsed
                };
            }
            
            // Check for errors
            if (uiDump.includes('Transcription Failed') || 
                uiDump.includes('Error:') ||
                uiDump.includes('Failed Step:')) {
                this.core.logger.error('❌ Transcription failed');
                return {
                    success: false,
                    error: this.extractError(uiDump),
                    failedStep: this.extractFailedStep(uiDump),
                    duration: elapsed
                };
            }
            
            // Check for progress updates
            const progressMatch = uiDump.match(/Step (\d+) of (\d+)/);
            if (progressMatch) {
                const stepNumber = parseInt(progressMatch[1]);
                const totalSteps = parseInt(progressMatch[2]);
                lastProgressUpdate = Date.now();
                this.core.logger.info(`📈 Progress: Step ${stepNumber}/${totalSteps}`);
            }
            
            // Check for timeout on current step
            if (Date.now() - lastProgressUpdate > progressTimeout) {
                return {
                    success: false,
                    error: `Step timeout: No progress for ${progressTimeout}ms`,
                    failedStep: 'progress_timeout',
                    duration: elapsed
                };
            }
            
            // Check overall timeout
            if (remaining < 10000) { // Less than 10 seconds remaining
                return {
                    success: false,
                    error: `Overall timeout: ${elapsed}ms elapsed (max: ${this.maxDuration}ms)`,
                    failedStep: 'timeout',
                    duration: elapsed
                };
            }
            
            await this.core.sleep(5000); // Check every 5 seconds
        }
        
        return {
            success: false,
            error: 'Maximum duration exceeded',
            failedStep: 'timeout',
            duration: Date.now() - this.startTime
        };
    }
    
    extractTranscript(uiDump) {
        // Extract transcript from UI dump
        const transcriptMatch = uiDump.match(/Transcript Preview: (.+?)\.\.\./);
        return transcriptMatch ? transcriptMatch[1] : 'Transcript not found in UI';
    }
    
    extractError(uiDump) {
        // Extract error message from UI dump
        const errorMatch = uiDump.match(/Error: (.+?)(?:\n|$)/);
        return errorMatch ? errorMatch[1] : 'Error message not found in UI';
    }
    
    extractFailedStep(uiDump) {
        // Extract failed step from UI dump
        const stepMatch = uiDump.match(/Failed Step: (.+?)(?:\n|$)/);
        return stepMatch ? stepMatch[1] : 'Failed step not found in UI';
    }
}

module.exports = JourneyTikTokManualURL01Transcription;
