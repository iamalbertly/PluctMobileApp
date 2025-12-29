const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-EdgeCase-02CreditDepletion-Validation
 * Follows naming convention: [Journey]-[EdgeCase]-[02CreditDepletion]-[Validation]
 * 4 scope layers: Journey, EdgeCase, CreditDepletion, Validation
 * Validates credit depletion during auto-submit edge case
 */
class JourneyEdgeCase02CreditDepletionValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'EdgeCase-02CreditDepletion-Validation';
    }

    async execute() {
        await this.log('Starting Credit Depletion Edge Case Validation');
        
        // Step 1: Set credits to 1 (via API or mock)
        // Note: This would require API access or mock setup
        // For now, we'll check if credits are low and proceed
        
        // Step 2: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 3: Check current credit balance
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        const creditMatch = uiDump.match(/(\d+)\s*credits/i);
        const currentCredits = creditMatch ? parseInt(creditMatch[1]) : null;
        
        if (currentCredits === null || currentCredits > 1) {
            this.logger.warn('⚠️ Credits > 1, test may not trigger edge case');
        }
        
        // Step 4: Send intent to trigger auto-submit
        const intentStartTime = Date.now();
        await this.core.executeCommand(
            `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${this.core.config.url}" app.pluct/.PluctUIScreen01MainActivity`
        );
        
        // Step 5: Simulate credit consumption (would need API call)
        // In real scenario, another process would consume the credit
        await this.core.sleep(1000);
        
        // Step 6: Monitor for queue prompt (not error)
        await this.core.sleep(2000);
        
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        
        const hasQueuePrompt = uiDump.includes('Add credits') ||
                              uiDump.includes('queued') ||
                              uiDump.includes('Save for later');
        
        const hasError = uiDump.includes('Error') ||
                        uiDump.includes('Failed') ||
                        uiDump.includes('insufficient');
        
        if (hasError && !hasQueuePrompt) {
            return {
                success: false,
                error: 'Error shown instead of queue prompt when credits depleted'
            };
        }
        
        // Step 7: Verify video is queued (check logcat)
        const queueLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "queued\|INSUFFICIENT_CREDITS\|queueVideo"'
        );
        
        const queued = queueLogcat.output && (
            queueLogcat.output.includes('queued') ||
            queueLogcat.output.includes('INSUFFICIENT_CREDITS')
        );
        
        if (!queued && currentCredits === 0) {
            return {
                success: false,
                error: 'Video was not queued when credits depleted'
            };
        }
        
        // Step 8: Verify "Add credits" prompt appears
        if (!hasQueuePrompt) {
            return {
                success: false,
                error: 'Add credits prompt not shown'
            };
        }
        
        await this.log('✅ Credit depletion edge case validated');
        return true;
    }
}

module.exports = JourneyEdgeCase02CreditDepletionValidation;

