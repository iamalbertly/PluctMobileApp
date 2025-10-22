/**
 * Pluct-UI-Validator - Consolidated UI validation
 * Single source of truth for UI validation functionality
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctUIValidator {
    constructor(core) {
        this.core = core;
        this.uiElements = new Map();
        this.validationResults = [];
    }

    /**
     * Validate UI components
     */
    async validateComponents(tag, expectedElements) {
        this.core.logger.info(`🔍 Validating UI components for ${tag}...`);
        
        try {
            // Dump UI hierarchy
            const dumpResult = await this.core.dumpUIHierarchy();
            if (!dumpResult.success) {
                return { success: false, error: 'Failed to dump UI hierarchy' };
            }

            // Parse UI elements
            const elements = await this.parseUIElements();
            
            // Validate expected elements
            const validation = this.validateExpectedElements(elements, expectedElements);
            
            this.validationResults.push({
                tag,
                timestamp: Date.now(),
                success: validation.success,
                found: validation.found,
                missing: validation.missing
            });

            return validation;
        } catch (error) {
            this.core.logger.error(`UI validation failed for ${tag}:`, error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Parse UI elements from dump
     */
    async parseUIElements() {
        try {
            const fs = require('fs');
            const xmlContent = fs.readFileSync('artifacts/ui/ui_dump.xml', 'utf8');
            
            // Simple XML parsing for UI elements
            const elements = [];
            const elementRegex = /<node[^>]*resource-id="([^"]*)"[^>]*class="([^"]*)"[^>]*text="([^"]*)"[^>]*>/g;
            let match;
            
            while ((match = elementRegex.exec(xmlContent)) !== null) {
                elements.push({
                    resourceId: match[1],
                    className: match[2],
                    text: match[3]
                });
            }
            
            return elements;
        } catch (error) {
            this.core.logger.error('Failed to parse UI elements:', error);
            return [];
        }
    }

    /**
     * Validate expected elements
     */
    validateExpectedElements(elements, expectedElements) {
        const found = [];
        const missing = [];
        
        for (const expected of expectedElements) {
            const foundElement = elements.find(element => 
                element.resourceId.includes(expected) || 
                element.text.includes(expected) ||
                element.className.includes(expected)
            );
            
            if (foundElement) {
                found.push(expected);
            } else {
                missing.push(expected);
            }
        }
        
        return {
            success: missing.length === 0,
            found,
            missing,
            total: expectedElements.length,
            foundCount: found.length,
            missingCount: missing.length
        };
    }

    /**
     * Capture UI artifacts
     */
    async captureUIArtifacts(tag) {
        return await this.core.captureUIArtifacts(tag);
    }

    /**
     * Print UI inventory
     */
    async printUIInventory(tag) {
        try {
            const elements = await this.parseUIElements();
            this.core.logger.info(`📱 UI Inventory for ${tag}:`);
            this.core.logger.info(`   Total elements: ${elements.length}`);
            
            // Group elements by type
            const grouped = this.groupElementsByType(elements);
            for (const [type, count] of Object.entries(grouped)) {
                this.core.logger.info(`   ${type}: ${count}`);
            }
        } catch (error) {
            this.core.logger.error('Failed to print UI inventory:', error);
        }
    }

    /**
     * Group elements by type
     */
    groupElementsByType(elements) {
        const groups = {};
        
        for (const element of elements) {
            const type = element.className.split('.').pop() || 'Unknown';
            groups[type] = (groups[type] || 0) + 1;
        }
        
        return groups;
    }

    /**
     * Clear UI artifacts
     */
    clearUIArtifacts() {
        try {
            const fs = require('fs');
            const path = require('path');
            
            const artifactsDir = 'artifacts/ui';
            if (fs.existsSync(artifactsDir)) {
                const files = fs.readdirSync(artifactsDir);
                for (const file of files) {
                    fs.unlinkSync(path.join(artifactsDir, file));
                }
            }
            
            this.core.logger.info('🧹 UI artifacts cleared');
        } catch (error) {
            this.core.logger.error('Failed to clear UI artifacts:', error);
        }
    }

    /**
     * Get validation statistics
     */
    getValidationStatistics() {
        const total = this.validationResults.length;
        const successful = this.validationResults.filter(r => r.success).length;
        const failed = total - successful;
        
        return {
            total,
            successful,
            failed,
            successRate: total > 0 ? (successful / total) * 100 : 0,
            results: this.validationResults
        };
    }
}

/**
 * UI Interaction utilities
 */
class PluctUIInteraction {
    constructor(core) {
        this.core = core;
        this.interactionHistory = [];
    }

    /**
     * Click element
     */
    async clickElement(x, y) {
        try {
            const result = await this.core.executeCommand(`adb shell input tap ${x} ${y}`);
            this.interactionHistory.push({
                action: 'click',
                coordinates: { x, y },
                timestamp: Date.now(),
                success: result.success
            });
            return result;
        } catch (error) {
            this.core.logger.error('Click failed:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Input text
     */
    async inputText(text) {
        try {
            const result = await this.core.executeCommand(`adb shell input text "${text}"`);
            this.interactionHistory.push({
                action: 'input',
                text,
                timestamp: Date.now(),
                success: result.success
            });
            return result;
        } catch (error) {
            this.core.logger.error('Input failed:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Clear interaction history
     */
    clearInteractionHistory() {
        this.interactionHistory = [];
        this.core.logger.info('🧹 Interaction history cleared');
    }
}

/**
 * Real-time status utilities
 */
class PluctRealTimeStatus {
    constructor(core) {
        this.core = core;
        this.statusUpdates = [];
    }

    /**
     * Update video status
     */
    updateVideoStatus(videoId, status, message = '') {
        const update = {
            videoId,
            status,
            message,
            timestamp: Date.now()
        };
        
        this.statusUpdates.push(update);
        this.core.logger.info(`📊 Video ${videoId} status: ${status} - ${message}`);
    }

    /**
     * Show toast notification
     */
    showToast(message, type = 'info', duration = 3000) {
        this.core.logger.info(`🍞 Toast: ${type.toUpperCase()} - ${message}`);
    }

    /**
     * Update progress
     */
    updateProgress(message, progress = 0) {
        this.core.logger.info(`📊 Progress: ${progress}% - ${message}`);
    }

    /**
     * Hide progress
     */
    hideProgress() {
        this.core.logger.info('📊 Progress indicator hidden');
    }

    /**
     * Show dialog
     */
    showDialog(title, message, actions = []) {
        this.core.logger.info(`💬 Dialog: ${title} - ${message}`);
    }

    /**
     * Update status
     */
    updateStatus(status, details = '') {
        this.core.logger.info(`📋 Status: ${status} - ${details}`);
    }
}

module.exports = { PluctUIValidator, PluctUIInteraction, PluctRealTimeStatus };
