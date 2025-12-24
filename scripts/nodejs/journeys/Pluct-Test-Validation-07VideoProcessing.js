const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Pluct-Test-Validation-07VideoProcessing - Video processing validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates video processing pipeline functionality
 */
class PluctTestValidationVideoProcessing extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-07VideoProcessing';
        this.maxDuration = 30000; // 30 seconds max
    }

    async execute() {
        try {
            this.core.logger.info('🔍 Validating video processing pipeline...');
            
            // Test metadata endpoint
            let lastMeta = null;
            const urlsToTest = this.core.getTestUrls();
            for (const testUrl of urlsToTest) {
                const metaResponse = await this.core.httpGet(
                    `${this.core.config.businessEngineUrl}/meta?url=${encodeURIComponent(testUrl)}`
                );

                if (!metaResponse.success || metaResponse.status !== 200) {
                    return { success: false, error: `Metadata endpoint request failed for ${testUrl}` };
                }

                const metaData = JSON.parse(metaResponse.body);
                lastMeta = metaData;
                if (!metaData.title || !metaData.author) {
                    return { success: false, error: `Invalid metadata response format for ${testUrl}` };
                }
            }

            this.core.logger.info('✅ Video processing pipeline validation passed');
            return { success: true, details: { metadataReceived: true, title: lastMeta ? lastMeta.title : null } };
            
        } catch (error) {
            this.core.logger.error(`❌ Video processing pipeline validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationVideoProcessing;
