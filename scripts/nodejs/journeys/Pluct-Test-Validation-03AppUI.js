const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Pluct-Test-Validation-03AppUI - App UI validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates app launch, UI elements, and accessibility features
 */
class PluctTestValidationAppUI extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-03AppUI';
        this.maxDuration = 30000; // 30 seconds max
    }

    async execute() {
        try {
            this.core.logger.info('🔍 Validating app UI...');
            
            // Launch app
            const launchResult = await this.core.launchApp();
            if (!launchResult.success) {
                return { success: false, error: 'App launch failed' };
            }
            
            await this.core.sleep(3000);
            
            // Check for modern UI elements
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // Validate header presence
            const hasHeader = uiDump.includes('App header with refreshable credit balance and settings');
            if (!hasHeader) {
                return { success: false, error: 'Modern header not found' };
            }
            
            // Validate credit balance display
            const hasCreditDisplay = uiDump.includes('Credit balance display showing') || 
                                   uiDump.includes('💎') || 
                                   uiDump.includes('Error');
            if (!hasCreditDisplay) {
                return { success: false, error: 'Credit balance display not found' };
            }
            
            // Validate floating action button
            const hasFAB = uiDump.includes('Start transcription button');
            if (!hasFAB) {
                return { success: false, error: 'Floating action button not found' };
            }
            
            // Validate content descriptions
            const hasContentDescriptions = uiDump.includes('content-desc=') && 
                                         uiDump.split('content-desc=').length > 5;
            if (!hasContentDescriptions) {
                return { success: false, error: 'Insufficient content descriptions found' };
            }
            
            this.core.logger.info('✅ App UI validation passed');
            return { success: true, details: { header: hasHeader, creditDisplay: hasCreditDisplay, fab: hasFAB, contentDescriptions: hasContentDescriptions } };
            
        } catch (error) {
            this.core.logger.error(`❌ App UI validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationAppUI;
