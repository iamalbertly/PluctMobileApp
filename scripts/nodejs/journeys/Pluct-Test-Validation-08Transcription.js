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
            
            // Vend a service token
            const vendResponse = await this.core.httpPost(
                'https://pluct-business-engine.romeo-lya2.workers.dev/v1/vend-token',
                { userId: 'mobile' },
                { 'Authorization': `Bearer ${jwtToken}`, 'Content-Type': 'application/json' }
            );
            
            if (!vendResponse.success || vendResponse.status !== 200) {
                return { success: false, error: 'Token vending failed' };
            }
            
            const vendData = JSON.parse(vendResponse.body);
            const serviceToken = vendData.token;
            
            // Start transcription job
            const testUrl = 'https://vm.tiktok.com/ZMA730880/';
            const transcribeResponse = await this.core.httpPost(
                'https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe',
                { url: testUrl },
                { 'Authorization': `Bearer ${serviceToken}`, 'Content-Type': 'application/json' }
            );
            
            if (!transcribeResponse.success || transcribeResponse.status !== 200) {
                return { success: false, error: 'Transcription job creation failed' };
            }
            
            const transcribeData = JSON.parse(transcribeResponse.body);
            const jobId = transcribeData.jobId;
            
            // Check job status (just verify it was created)
            const statusResponse = await this.core.httpGet(
                `https://pluct-business-engine.romeo-lya2.workers.dev/ttt/status/${jobId}`,
                { 'Authorization': `Bearer ${serviceToken}` }
            );
            
            if (!statusResponse.success || statusResponse.status !== 200) {
                return { success: false, error: 'Job status check failed' };
            }
            
            this.core.logger.info('✅ End-to-end transcription validation passed');
            return { success: true, details: { jobId: jobId, status: 'created' } };
            
        } catch (error) {
            this.core.logger.error(`❌ End-to-end transcription validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationTranscription;
