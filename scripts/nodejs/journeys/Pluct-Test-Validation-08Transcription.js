const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Pluct-Test-Validation-08Transcription - Transcription validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates end-to-end transcription functionality
 */
class PluctTestValidationTranscription extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-08Transcription';
        this.maxDuration = 120000; // 2 minutes max
    }

    async execute() {
        try {
            this.core.logger.info('🔍 Validating end-to-end transcription...');
            
            // Generate test JWT token
            const jwtToken = this.core.generateTestJWT('mobile');
            
            // Vend a service token (use SSOT base URL)
            const vendClientRequestId = `req_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
            const vendResponse = await this.core.httpPost(
                `${this.core.config.businessEngineUrl}/v1/vend-token`,
                { userId: 'mobile', clientRequestId: vendClientRequestId },
                { 'Authorization': `Bearer ${jwtToken}`, 'Content-Type': 'application/json' }
            );
            
            if (!vendResponse.success || vendResponse.status !== 200) {
                return { success: false, error: 'Token vending failed' };
            }
            
            const vendData = JSON.parse(vendResponse.body);
            const serviceToken = vendData.token || vendData.serviceToken || vendData.pollingToken;
            if (!serviceToken) {
                return { success: false, error: 'Token vending returned empty service token' };
            }

            const urlsToTest = this.core.getTestUrls();
            for (const testUrl of urlsToTest) {
                const clientRequestId = `transcribe_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
                const transcribeResponse = await this.core.httpPost(
                    `${this.core.config.businessEngineUrl}/ttt/transcribe`,
                    { url: testUrl, clientRequestId },
                    { 'Authorization': `Bearer ${serviceToken}`, 'Content-Type': 'application/json' }
                );

                if (!transcribeResponse.success || transcribeResponse.status !== 200) {
                    return { success: false, error: `Transcription job creation failed for ${testUrl}` };
                }

                const transcribeData = JSON.parse(transcribeResponse.body);
                const jobId = transcribeData.jobId;

                const statusResponse = await this.core.httpGet(
                    `${this.core.config.businessEngineUrl}/ttt/status/${jobId}`,
                    { 'Authorization': `Bearer ${serviceToken}` }
                );

                if (!statusResponse.success || statusResponse.status !== 200) {
                    return { success: false, error: `Job status check failed for ${testUrl}` };
                }
            }

            this.core.logger.info('? End-to-end transcription validation passed');
            return { success: true };

        } catch (error) {
            this.core.logger.error(`❌ End-to-end transcription validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationTranscription;
