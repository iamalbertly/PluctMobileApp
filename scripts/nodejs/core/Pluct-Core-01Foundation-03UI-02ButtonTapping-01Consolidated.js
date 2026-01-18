/**
 * Pluct-Core-01Foundation-03UI-02ButtonTapping-01Consolidated.js
 * Consolidated button tapping logic with comprehensive fallback strategies
 * Follows naming: [Project]-[Core]-[01Foundation]-[03UI]-[02ButtonTapping]-[01Consolidated]
 */

class PluctCoreFoundationUIButtonTappingConsolidated {
    constructor(uiModule, logger) {
        this.ui = uiModule;
        this.logger = logger;
    }

    /**
     * Tap button with comprehensive fallback strategy
     * Tries: testTag -> text -> contentDesc -> coordinates
     */
    async tapButtonWithFallback(options) {
        const { testTag, text, contentDesc, coordinates, buttonName } = options;
        
        this.logger.info(`🔍 Attempting to tap: ${buttonName || testTag || text || 'button'}`);
        
        // Strategy 1: Test tag (most reliable)
        if (testTag) {
            const result = await this.ui.tapByTestTag(testTag);
            if (result.success) {
                this.logger.info(`✅ Tapped by test tag: ${testTag}`);
                return result;
            }
        }
        
        // Strategy 2: Text matching
        if (text) {
            const texts = Array.isArray(text) ? text : [text];
            for (const t of texts) {
                const result = await this.ui.tapByText(t);
                if (result.success) {
                    this.logger.info(`✅ Tapped by text: ${t}`);
                    return result;
                }
            }
        }
        
        // Strategy 3: Content description
        if (contentDesc) {
            const result = await this.ui.tapByContentDesc(contentDesc);
            if (result.success) {
                this.logger.info(`✅ Tapped by content description: ${contentDesc}`);
                return result;
            }
        }
        
        // Strategy 4: Coordinates (last resort)
        if (coordinates) {
            const coords = Array.isArray(coordinates[0]) ? coordinates : [coordinates];
            for (const [x, y] of coords) {
                const result = await this.ui.tapByCoordinates(x, y);
                if (result.success) {
                    this.logger.info(`✅ Tapped by coordinates: (${x}, ${y})`);
                    return result;
                }
            }
        }
        
        this.logger.error(`❌ Failed to tap button after all strategies`);
        return { success: false, error: `Button not found: ${buttonName || testTag || text}` };
    }

    /**
     * Tap Extract Script button with all known variations
     */
    async tapExtractScriptButton() {
        return await this.tapButtonWithFallback({
            testTag: 'extract_script_button',
            text: ['Extract Script', 'FREE', '📄 Extract Script'],
            contentDesc: 'Extract Script',
            coordinates: [[360, 700], [360, 750], [360, 800], [206, 769]],
            buttonName: 'Extract Script'
        });
    }

    /**
     * Tap URL input field with all known variations
     */
    async tapURLInputField() {
        return await this.tapButtonWithFallback({
            testTag: 'url_input_field',
            text: ['TikTok URL', 'Paste Video Link', 'Paste TikTok Link'],
            contentDesc: 'Video URL input field',
            buttonName: 'URL Input Field'
        });
    }

    /**
     * Tap Settings button with all known variations
     */
    async tapSettingsButton() {
        return await this.tapButtonWithFallback({
            testTag: 'settings_button',
            text: ['Settings'],
            contentDesc: 'Settings button',
            buttonName: 'Settings'
        });
    }
}

module.exports = PluctCoreFoundationUIButtonTappingConsolidated;
