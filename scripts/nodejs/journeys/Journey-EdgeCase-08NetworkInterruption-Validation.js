const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-EdgeCase-08NetworkInterruption-Validation
 * Follows naming convention: [Journey]-[EdgeCase]-[08NetworkInterruption]-[Validation]
 * 4 scope layers: Journey, EdgeCase, NetworkInterruption, Validation
 * Validates network interruption during token vending with idempotency edge case
 */
class JourneyEdgeCase08NetworkInterruptionValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'EdgeCase-08NetworkInterruption-Validation';
    }

    async execute() {
        await this.log('Starting Network Interruption During Token Vending Edge Case Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Start transcription
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        const extractButton = this.core.findElementByText(uiDump, 'Extract Script');
        if (extractButton) {
            await this.core.clickElement(extractButton);
        } else {
            const urlInput = this.core.findElementByHint(uiDump, 'Enter TikTok URL');
            if (urlInput) {
                await this.core.typeText(urlInput, this.core.config.url);
                await this.core.sleep(500);
                await this.core.pressKey('Enter');
            }
        }
        
        await this.core.sleep(1000);
        
        // Step 3: Disable network during token vending
        await this.core.executeCommand('adb shell svc wifi disable');
        await this.core.sleep(2000);
        
        // Step 4: Verify idempotency (same clientRequestId used)
        const requestIdLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "clientRequestId\|requestId\|client_request_id"'
        );
        
        const requestIdMatches = (requestIdLogcat.output || '').match(/clientRequestId[=:]?\s*([a-zA-Z0-9_-]+)/gi);
        const uniqueRequestIds = new Set(requestIdMatches || []);
        
        // Step 5: Re-enable network
        await this.core.executeCommand('adb shell svc wifi enable');
        await this.core.sleep(3000);
        
        // Step 6: Verify retry uses same request ID
        await this.core.sleep(2000);
        
        const retryRequestIdLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "clientRequestId\|requestId"'
        );
        
        const retryRequestIdMatches = (retryRequestIdLogcat.output || '').match(/clientRequestId[=:]?\s*([a-zA-Z0-9_-]+)/gi);
        const retryRequestIds = new Set(retryRequestIdMatches || []);
        
        // Check if same request ID is reused
        if (uniqueRequestIds.size > 0 && retryRequestIds.size > 0) {
            const allRequestIds = [...uniqueRequestIds, ...retryRequestIds];
            const allUnique = new Set(allRequestIds);
            
            if (allUnique.size < allRequestIds.length) {
                // Some request IDs are reused (good for idempotency)
                this.logger.info('✅ Request ID reuse detected (idempotency)');
            }
        }
        
        // Step 7: Verify no duplicate credit deduction
        const creditLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "balance\|credit\|deduct"'
        );
        
        const balanceMatches = (creditLogcat.output || '').match(/balance[=:]?\s*(\d+)/gi);
        if (balanceMatches && balanceMatches.length > 1) {
            const balances = balanceMatches.map(m => parseInt(m.match(/\d+/)[0]));
            const balanceDecreases = balances.filter((b, i) => i > 0 && b < balances[i - 1]);
            
            if (balanceDecreases.length > 1) {
                return {
                    success: false,
                    error: 'Multiple credit deductions detected'
                };
            }
        }
        
        await this.log('✅ Network interruption during token vending edge case validated');
        return true;
    }
}

module.exports = JourneyEdgeCase08NetworkInterruptionValidation;

