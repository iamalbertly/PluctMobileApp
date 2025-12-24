const PluctCoreFoundation = require('../core/Pluct-Core-01Foundation');
const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class TokenVendingValidationJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'TokenVendingValidation-Enhanced';
    }

    async execute() {
        try {
            this.core.logger.info('🎫 [TokenVendingValidation] Starting token vend count validation...');
            
            // 1. Launch app first (this will trigger app launch vend)
            const launchResult = await this.core.launchApp();
            if (!launchResult.success) {
                return { success: false, error: 'Failed to launch app' };
            }
            await this.core.sleep(3000); // Wait for app launch vend to complete
            
            // 2. Clear logcat AFTER app launch to exclude app launch vends
            await this.core.executeCommand('adb logcat -c');
            this.core.logger.info('✅ Logcat cleared (after app launch)');
            
            // 3. Enter test URL
            const testUrl = 'https://vm.tiktok.com/ZMDRUGT2P/';
            this.core.logger.info(`📝 Entering test URL: ${testUrl}`);
            
            // Try multiple methods to input URL
            let inputSuccess = false;
            let tapResult = await this.core.tapByTestTag('url_input_field');
            if (!tapResult.success) {
                tapResult = await this.core.tapByText('Paste a TikTok link');
            }
            if (!tapResult.success) {
                tapResult = await this.core.tapFirstEditText();
            }
            
            if (tapResult.success) {
                await this.core.sleep(500);
                await this.core.inputText(testUrl);
                await this.core.sleep(1000);
                inputSuccess = true;
            }
            
            if (!inputSuccess) {
                return { success: false, error: 'Failed to enter URL' };
            }
            
            // 4. Tap Extract Script button
            this.core.logger.info('👆 Tapping Extract Script button...');
            let extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                extractTap = await this.core.tapByText('Extract Script');
            }
            if (!extractTap.success) {
                return { success: false, error: 'Failed to tap Extract Script button' };
            }
            
            // 5. Monitor logcat for token vend calls
            this.core.logger.info('⏳ Monitoring logcat for token vend calls...');
            const startTime = Date.now();
            let vendCount = 0;
            const maxWait = 120000; // 2 minutes
            
            while (Date.now() - startTime < maxWait) {
                const logcat = await this.core.executeCommand(
                    'adb logcat -d | findstr /i "vend-token vendToken TOKEN_MANAGEMENT token_vend token_reuse"'
                );
                
                // Count vend-token API calls (look for actual API calls, not reuse logs)
                const vendMatches = (logcat.output || '').match(/vend-token|vendToken|token_vend/g);
                if (vendMatches) {
                    vendCount = vendMatches.length;
                }
                
                // Check if transcription completed
                const completed = logcat.output.includes('Transcription completed') ||
                                 logcat.output.includes('COMPLETED') ||
                                 logcat.output.includes('processVideo_complete');
                
                if (completed) {
                    this.core.logger.info('✅ Transcription completed');
                    break;
                }
                
                await this.core.sleep(3000);
            }
            
            // 6. Validate vend count
            this.core.logger.info(`📊 Token vend count: ${vendCount}`);
            if (vendCount > 1) {
                return {
                    success: false,
                    error: `Expected 1 token vend, but found ${vendCount} vends`,
                    vendCount
                };
            }
            
            if (vendCount === 0) {
                return {
                    success: false,
                    error: 'No token vend calls found in logcat (may indicate caching or error)',
                    vendCount
                };
            }
            
            return { success: true, vendCount };
        } catch (err) {
            return { success: false, error: err.message };
        }
    }
}

function register(orchestrator) {
    const core = new PluctCoreFoundation();
    orchestrator.registerJourney('TokenVendingValidation-Enhanced', new TokenVendingValidationJourney(core));
}

module.exports = { TokenVendingValidationJourney, register };

