const BaseCore = require('./Pluct-Core-01Foundation.js');

/**
 * Pluct-Core-UI-State-Tracker-02Capture - UI state capture module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles UI state capture and basic element extraction
 */
class PluctUIStateCapture extends BaseCore {
    constructor(core) {
        super();
        this.core = core;
    }

    /**
     * Capture current UI state with metadata
     */
    async captureCurrentState(label = 'unknown') {
        try {
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            const timestamp = new Date().toISOString();
            
            const state = {
                label,
                timestamp,
                uiDump,
                elementCount: this.countUIElements(uiDump),
                clickableElements: this.extractClickableElements(uiDump),
                textElements: this.extractTextElements(uiDump),
                focusedElement: this.extractFocusedElement(uiDump),
                enabledButtons: this.extractEnabledButtons(uiDump),
                videoListItems: this.extractVideoListItems(uiDump),
                creditBalance: this.extractCreditBalance(uiDump),
                processingStates: this.extractProcessingStates(uiDump)
            };

            return state;
        } catch (error) {
            this.core.logger.error(`UI state capture failed: ${error.message}`);
            throw error;
        }
    }

    /**
     * Count total UI elements
     */
    countUIElements(uiDump) {
        const elementMatches = uiDump.match(/<[^>]+>/g);
        return elementMatches ? elementMatches.length : 0;
    }

    /**
     * Extract clickable elements
     */
    extractClickableElements(uiDump) {
        const clickableRegex = /clickable="true"[^>]*>/g;
        const matches = uiDump.match(clickableRegex);
        return matches ? matches.length : 0;
    }

    /**
     * Extract text elements
     */
    extractTextElements(uiDump) {
        const textRegex = /text="[^"]*"/g;
        const matches = uiDump.match(textRegex);
        return matches ? matches.length : 0;
    }

    /**
     * Extract focused element
     */
    extractFocusedElement(uiDump) {
        const focusedMatch = uiDump.match(/focused="true"[^>]*>/);
        return focusedMatch ? focusedMatch[0] : null;
    }

    /**
     * Extract enabled buttons
     */
    extractEnabledButtons(uiDump) {
        const buttonRegex = /enabled="true"[^>]*class="[^"]*button[^"]*"/g;
        const matches = uiDump.match(buttonRegex);
        return matches ? matches.length : 0;
    }

    /**
     * Extract video list items
     */
    extractVideoListItems(uiDump) {
        const videoRegex = /class="[^"]*video[^"]*"/g;
        const matches = uiDump.match(videoRegex);
        return matches ? matches.length : 0;
    }

    /**
     * Extract credit balance information
     */
    extractCreditBalance(uiDump) {
        const creditMatch = uiDump.match(/💎\s*(\d+)/);
        return creditMatch ? parseInt(creditMatch[1]) : null;
    }

    /**
     * Extract processing states
     */
    extractProcessingStates(uiDump) {
        const processingKeywords = ['processing', 'loading', 'transcribing', 'analyzing'];
        const states = {};
        
        processingKeywords.forEach(keyword => {
            const regex = new RegExp(keyword, 'gi');
            const matches = uiDump.match(regex);
            states[keyword] = matches ? matches.length : 0;
        });
        
        return states;
    }
}

module.exports = PluctUIStateCapture;
