const BaseCore = require('./Pluct-Core-01Foundation.js');

/**
 * Pluct-Core-UI-State-Tracker-04ChangeDetection - UI state change detection module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles UI state change detection and monitoring
 */
class PluctUIStateChangeDetection extends BaseCore {
    constructor(core) {
        super();
        this.core = core;
        this.changeDetectionTimeout = 5000; // 5 seconds
        this.criticalErrorThreshold = 3; // Max retries before critical error
    }

    /**
     * Detect changes between UI states
     */
    detectChanges(currentState, previousState) {
        try {
            if (!previousState) {
                return { success: true, changes: [], isFirstState: true };
            }

            const changes = {
                elementCountChange: currentState.elementCount - previousState.elementCount,
                clickableElementsChange: currentState.clickableElements - previousState.clickableElements,
                textElementsChange: currentState.textElements - previousState.textElements,
                enabledButtonsChange: currentState.enabledButtons - previousState.enabledButtons,
                videoListItemsChange: currentState.videoListItems - previousState.videoListItems,
                creditBalanceChange: this.compareCreditBalance(currentState.creditBalance, previousState.creditBalance),
                processingStatesChange: this.compareProcessingStates(currentState.processingStates, previousState.processingStates),
                focusedElementChange: this.compareFocusedElement(currentState.focusedElement, previousState.focusedElement)
            };

            const significantChanges = this.identifySignificantChanges(changes);
            
            return {
                success: true,
                changes: changes,
                significantChanges: significantChanges,
                hasChanges: significantChanges.length > 0
            };
        } catch (error) {
            this.core.logger.error(`Change detection failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Wait for specific UI state change
     */
    async waitForChange(expectedChange, timeoutMs = 10000) {
        try {
            const startTime = Date.now();
            let retryCount = 0;
            let previousDump = '';

            while (Date.now() - startTime < timeoutMs) {
                // Use core's dumpUIHierarchy instead of captureUIState
                await this.core.dumpUIHierarchy();
                const currentDump = this.core.readLastUIDump();
                
                // Simple change detection - check if UI dump changed
                if (previousDump && currentDump !== previousDump) {
                    // UI changed - check if it matches expected change
                    if (!expectedChange || this.matchesExpectedChangeInDump(currentDump, previousDump, expectedChange)) {
                        return { success: true, changes: { uiDumpChanged: true }, retryCount };
                    }
                }
                
                previousDump = currentDump;
                retryCount++;
                
                if (retryCount >= this.criticalErrorThreshold) {
                    return { 
                        success: false, 
                        error: 'Critical error threshold reached', 
                        retryCount 
                    };
                }

                await this.core.sleep(1000);
            }

            return { 
                success: false, 
                error: 'Timeout waiting for UI state change', 
                timeoutMs, 
                retryCount 
            };
        } catch (error) {
            this.core.logger.error(`Wait for change failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Check if UI dump changes match expected change
     */
    matchesExpectedChangeInDump(currentDump, previousDump, expectedChange) {
        if (!expectedChange) return true;
        
        // Check for specific text/element changes
        if (expectedChange.type === 'element_count') {
            return currentDump.length !== previousDump.length;
        }
        
        if (expectedChange.type === 'clickable_elements') {
            const currentClickable = (currentDump.match(/clickable="true"/g) || []).length;
            const previousClickable = (previousDump.match(/clickable="true"/g) || []).length;
            return currentClickable !== previousClickable;
        }
        
        if (expectedChange.type === 'text_elements') {
            const currentText = (currentDump.match(/text="[^"]+"/g) || []).length;
            const previousText = (previousDump.match(/text="[^"]+"/g) || []).length;
            return currentText !== previousText;
        }
        
        // Default: any change is acceptable
        return true;
    }

    /**
     * Compare credit balance between states
     */
    compareCreditBalance(current, previous) {
        if (current === null && previous === null) return 0;
        if (current === null || previous === null) return 'unknown';
        return current - previous;
    }

    /**
     * Compare processing states between states
     */
    compareProcessingStates(current, previous) {
        const changes = {};
        
        Object.keys(current).forEach(key => {
            const currentValue = current[key] || 0;
            const previousValue = previous[key] || 0;
            changes[key] = currentValue - previousValue;
        });
        
        return changes;
    }

    /**
     * Compare focused element between states
     */
    compareFocusedElement(current, previous) {
        if (current === previous) return 'unchanged';
        if (current === null) return 'lost_focus';
        if (previous === null) return 'gained_focus';
        return 'changed';
    }

    /**
     * Identify significant changes
     */
    identifySignificantChanges(changes) {
        const significantChanges = [];
        
        if (Math.abs(changes.elementCountChange) > 5) {
            significantChanges.push('element_count');
        }
        
        if (Math.abs(changes.clickableElementsChange) > 2) {
            significantChanges.push('clickable_elements');
        }
        
        if (Math.abs(changes.textElementsChange) > 3) {
            significantChanges.push('text_elements');
        }
        
        if (Math.abs(changes.enabledButtonsChange) > 1) {
            significantChanges.push('enabled_buttons');
        }
        
        if (Math.abs(changes.videoListItemsChange) > 0) {
            significantChanges.push('video_list_items');
        }
        
        if (changes.creditBalanceChange !== 0 && changes.creditBalanceChange !== 'unknown') {
            significantChanges.push('credit_balance');
        }
        
        if (changes.focusedElementChange !== 'unchanged') {
            significantChanges.push('focused_element');
        }
        
        return significantChanges;
    }

    /**
     * Check if changes match expected change
     */
    matchesExpectedChange(actualChanges, expectedChange) {
        if (!expectedChange) return true;
        
        // Check specific change types
        if (expectedChange.type === 'element_count' && Math.abs(actualChanges.elementCountChange) > 0) {
            return true;
        }
        
        if (expectedChange.type === 'clickable_elements' && Math.abs(actualChanges.clickableElementsChange) > 0) {
            return true;
        }
        
        if (expectedChange.type === 'credit_balance' && actualChanges.creditBalanceChange !== 0) {
            return true;
        }
        
        if (expectedChange.type === 'focused_element' && actualChanges.focusedElementChange !== 'unchanged') {
            return true;
        }
        
        return false;
    }
}

module.exports = PluctUIStateChangeDetection;
