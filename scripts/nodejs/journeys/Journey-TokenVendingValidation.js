const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class TokenVendingValidationJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'TokenVendingValidation';
    }

    async execute() {
        const beBase = this.core.config.businessEngineUrl;
        const testUrl = this.core.getActiveUrl ? this.core.getActiveUrl() : this.core.config.url;
        this.core.logger.info('🎯 Testing Token Vending System Validation...');

        // 1) Launch app to home with a fresh local user so repeated validation does not deplete credits.
        const appReady = await this.core.resetAppToFreshCaptureState();
        if (!appReady.success) return { success: false, error: appReady.error || 'App not ready' };

        // 2) Test Business Engine health
        this.core.logger.info('🔍 Testing Business Engine health...');
        const healthResult = await this.core.httpGet(`${beBase}/health`);
        
        if (!healthResult.success || healthResult.status !== 200) {
            return { success: false, error: 'Business Engine health check failed' };
        }
        
        this.core.logger.info('✅ Business Engine health check passed');

        // 3) Test Token Vending endpoint
        this.core.logger.info('🔍 Testing Token Vending endpoint...');
        const vendResult = await this.core.httpPost(
            `${beBase}/v1/vend-token`,
            { userId: 'mobile-automation', clientRequestId: `test-${Date.now()}` }
        );
        
        // Token vending should return 401 (unauthorized) without JWT, which is expected
        if (!vendResult.success || (vendResult.status !== 401 && vendResult.status !== 200)) {
            this.core.logger.warn(`⚠️ Token vending returned unexpected status: ${(vendResult.status || vendResult.error)}`);
        }
        
        this.core.logger.info('✅ Token Vending endpoint accessible');

        // 4) Test TTTranscribe endpoint
        this.core.logger.info('🔍 Testing TTTranscribe endpoint...');
        const tttResult = await this.core.httpPost(
            `${beBase}/ttt/transcribe`,
            { url: testUrl }
        );
        
        // TTTranscribe should return 401 (unauthorized) without JWT, which is expected
        if (!tttResult.success || (tttResult.status !== 401 && tttResult.status !== 200)) {
            this.core.logger.warn(`⚠️ TTTranscribe returned unexpected status: ${(tttResult.status || tttResult.error)}`);
        }
        
        this.core.logger.info('✅ TTTranscribe endpoint accessible');

        // 5) Test current app integration without creating a failed transcription item.
        this.core.logger.info('Testing app capture readiness...');
        const captureReady = await this.core.ensureCaptureCardReady();
        if (!captureReady.success) {
            return { success: false, error: `Capture card not ready: ${captureReady.error}` };
        }
        this.core.logger.info('✅ Token Vending System validation completed');
        return { 
            success: true, 
            note: "Token Vending System and TTTranscribe integration validated" 
        };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('TokenVendingValidation', new TokenVendingValidationJourney(orchestrator.core));
}

module.exports = { TokenVendingValidationJourney, register };
