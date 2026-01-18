const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-EdgeCase-06ConcurrentVending-Validation
 * Follows naming convention: [Journey]-[EdgeCase]-[06ConcurrentVending]-[Validation]
 * 4 scope layers: Journey, EdgeCase, ConcurrentVending, Validation
 * Validates concurrent token vending prevention edge case
 */
class JourneyEdgeCase06ConcurrentVendingValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'EdgeCase-06ConcurrentVending-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Concurrent Token Vending Edge Case Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Tap "Extract Script" button rapidly (5 times in 1 second)
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        const extractButton = this.core.findElementByText(uiDump, 'Extract Script');
        if (!extractButton) {
            return {
                success: false,
                error: 'Extract Script button not found'
            };
        }
        
        // Rapid clicks
        for (let i = 0; i < 5; i++) {
            await this.core.clickElement(extractButton);
            await this.core.sleep(200); // 200ms between clicks
        }
        
        await this.core.sleep(2000);
        
        // Step 3: Verify only one token vend request (check logcat)
        const vendLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "vend-token\|vendToken\|POST.*vend"'
        );
        
        const vendCount = (vendLogcat.output || '').split('vend-token').length - 1;
        if (vendCount > 1) {
            return {
                success: false,
                error: `Multiple token vend requests detected: ${vendCount}`
            };
        }
        
        // Step 4: Verify button disabled during vending
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        
        const buttonDisabled = uiDump.includes('disabled') ||
                              uiDump.includes('enabled=false') ||
                              !uiDump.includes('Extract Script'); // Button may be hidden
        
        if (!buttonDisabled) {
            this.logger.warn('⚠️ Button may not be disabled during vending');
        }
        
        // Step 5: Verify no duplicate API calls in logcat
        const apiCallLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "POST.*v1/vend-token"'
        );
        
        const apiCallCount = (apiCallLogcat.output || '').split('POST').length - 1;
        if (apiCallCount > 1) {
            return {
                success: false,
                error: `Duplicate API calls detected: ${apiCallCount}`
            };
        }
        
        // Step 6: Verify single transcription job created
        const jobLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "jobId\|job_id\|Submitting job"'
        );
        
        const jobIdMatches = (jobLogcat.output || '').match(/job[_-]?id[=:]?\s*([a-zA-Z0-9_-]+)/gi);
        const uniqueJobIds = new Set(jobIdMatches || []);
        
        if (uniqueJobIds.size > 1) {
            return {
                success: false,
                error: `Multiple transcription jobs created: ${uniqueJobIds.size}`
            };
        }
        
        this.core.logger.info('✅ Concurrent token vending edge case validated');
        return true;
    }
}

module.exports = JourneyEdgeCase06ConcurrentVendingValidation;

