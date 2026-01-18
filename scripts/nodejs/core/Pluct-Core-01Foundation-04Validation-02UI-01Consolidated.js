/**
 * Pluct-Core-01Foundation-04Validation-02UI-01Consolidated.js
 * Consolidated UI validation patterns
 * Follows naming: [Project]-[Core]-[01Foundation]-[04Validation]-[02UI]-[01Consolidated]
 */

class PluctCoreFoundationValidationUIConsolidated {
    constructor(uiModule, logger) {
        this.ui = uiModule;
        this.logger = logger;
    }

    /**
     * Validate UI element visibility with retry
     */
    async validateElementVisible(options) {
        const { testTag, text, contentDesc, timeout = 5000, pollInterval = 500 } = options;
        const startTime = Date.now();
        
        while (Date.now() - startTime < timeout) {
            await this.ui.dumpUIHierarchy();
            const uiDump = this.ui.readLastUIDump();
            
            let found = false;
            
            if (testTag && uiDump.includes(`test-tag="${testTag}"`)) {
                found = true;
            } else if (text) {
                const texts = Array.isArray(text) ? text : [text];
                found = texts.some(t => uiDump.includes(t));
            } else if (contentDesc && uiDump.includes(`content-desc="${contentDesc}"`)) {
                found = true;
            }
            
            if (found) {
                this.logger.info(`✅ Element found: ${testTag || text || contentDesc}`);
                return { success: true };
            }
            
            await this.ui.sleep(pollInterval);
        }
        
        this.logger.error(`❌ Element not found: ${testTag || text || contentDesc}`);
        return { success: false, error: 'Element not visible' };
    }

    /**
     * Validate text appears in UI
     */
    async validateTextVisible(text, timeout = 5000) {
        return await this.validateElementVisible({ text, timeout });
    }

    /**
     * Validate test tag appears in UI
     */
    async validateTestTagVisible(testTag, timeout = 5000) {
        return await this.validateElementVisible({ testTag, timeout });
    }

    /**
     * Validate processing state
     */
    async validateProcessingState() {
        return await this.validateElementVisible({
            testTag: 'processing_indicator',
            text: ['Processing', 'Progress'],
            timeout: 10000
        });
    }

    /**
     * Validate error state
     */
    async validateErrorState() {
        return await this.validateElementVisible({
            testTag: 'error_message_text',
            text: ['Error', 'Failed'],
            timeout: 5000
        });
    }

    /**
     * Validate transcript display
     */
    async validateTranscriptDisplay() {
        return await this.validateElementVisible({
            text: ['transcript', 'Transcript', 'Completed'],
            timeout: 160000
        });
    }
}

module.exports = PluctCoreFoundationValidationUIConsolidated;
