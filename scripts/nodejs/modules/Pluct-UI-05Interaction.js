/**
 * Pluct-UI-05Interaction - Advanced UI interaction and validation
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[next stage increment to the childscope][CoreResponsibility]
 * Consolidated from Pluct-Node-Tests-UI-01Validator.js to maintain 300-line limit
 */

class PluctUI05Interaction {
    constructor(core) {
        this.core = core;
        this.interactionHistory = [];
    }

    /**
     * Find element with reliability
     */
    async findElementWithReliability(selector, maxAttempts = 3) {
        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                console.log(`🔍 Finding element with selector: ${selector} (attempt ${attempt}/${maxAttempts})`);
                
                const result = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
                if (result.success) {
                    const content = require('fs').readFileSync('artifacts/ui/current.xml', 'utf8');
                    
                    // Look for element with various patterns
                    const patterns = [
                        new RegExp(`content-desc="${selector}"`, 'i'),
                        new RegExp(`text="${selector}"`, 'i'),
                        new RegExp(`resource-id="[^"]*${selector}[^"]*"`, 'i')
                    ];
                    
                    for (const pattern of patterns) {
                        const match = content.match(pattern);
                        if (match) {
                            console.log(`✅ Element found with pattern: ${pattern}`);
                            return { success: true, element: match[0], attempt };
                        }
                    }
                }
                
                if (attempt < maxAttempts) {
                    await this.core.sleep(1000);
                }
            } catch (error) {
                console.warn(`⚠️ Attempt ${attempt} failed:`, error.message);
            }
        }
        
        return { success: false, reason: 'Element not found after all attempts' };
    }

    /**
     * Optimized click with multiple strategies
     */
    async optimizedClick(selector, strategies = ['content-desc', 'text', 'coordinates']) {
        for (const strategy of strategies) {
            try {
                console.log(`🎯 Attempting click with strategy: ${strategy}`);
                
                let result;
                switch (strategy) {
                    case 'content-desc':
                        result = await this.clickByContentDesc(selector);
                        break;
                    case 'text':
                        result = await this.clickByText(selector);
                        break;
                    case 'coordinates':
                        result = await this.clickByCoordinates(selector);
                        break;
                    default:
                        continue;
                }
                
                if (result.success) {
                    console.log(`✅ Click successful with strategy: ${strategy}`);
                    return result;
                }
            } catch (error) {
                console.warn(`⚠️ Strategy ${strategy} failed:`, error.message);
            }
        }
        
        return { success: false, reason: 'All click strategies failed' };
    }

    /**
     * Click by content description
     */
    async clickByContentDesc(selector) {
        try {
            const result = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (result.success) {
                const content = require('fs').readFileSync('artifacts/ui/current.xml', 'utf8');
                const pattern = new RegExp(`content-desc="${selector}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
                const match = content.match(pattern);
                
                if (match) {
                    const x1 = parseInt(match[1]);
                    const y1 = parseInt(match[2]);
                    const x2 = parseInt(match[3]);
                    const y2 = parseInt(match[4]);
                    
                    const centerX = Math.floor((x1 + x2) / 2);
                    const centerY = Math.floor((y1 + y2) / 2);
                    
                    await this.core.executeCommand(`adb shell input tap ${centerX} ${centerY}`);
                    return { success: true, coordinates: { x: centerX, y: centerY }, strategy: 'content-desc' };
                }
            }
            
            return { success: false, reason: 'Content description not found' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Click by text
     */
    async clickByText(selector) {
        try {
            const result = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (result.success) {
                const content = require('fs').readFileSync('artifacts/ui/current.xml', 'utf8');
                const pattern = new RegExp(`text="${selector}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
                const match = content.match(pattern);
                
                if (match) {
                    const x1 = parseInt(match[1]);
                    const y1 = parseInt(match[2]);
                    const x2 = parseInt(match[3]);
                    const y2 = parseInt(match[4]);
                    
                    const centerX = Math.floor((x1 + x2) / 2);
                    const centerY = Math.floor((y1 + y2) / 2);
                    
                    await this.core.executeCommand(`adb shell input tap ${centerX} ${centerY}`);
                    return { success: true, coordinates: { x: centerX, y: centerY }, strategy: 'text' };
                }
            }
            
            return { success: false, reason: 'Text not found' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Click by coordinates
     */
    async clickByCoordinates(selector) {
        try {
            // Use known coordinates for specific elements
            const knownCoordinates = {
                'quick_scan': { x: 360, y: 600 },
                'ai_analysis': { x: 360, y: 700 },
                'add_video': { x: 632, y: 1192 }
            };
            
            const coords = knownCoordinates[selector.toLowerCase()];
            if (coords) {
                await this.core.executeCommand(`adb shell input tap ${coords.x} ${coords.y}`);
                return { success: true, coordinates: coords, strategy: 'coordinates' };
            }
            
            return { success: false, reason: 'Coordinates not found' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Validate UI state changes
     */
    async validateUIStateChanges(beforeTag, afterTag) {
        try {
            console.log(`🔍 Validating UI state changes from ${beforeTag} to ${afterTag}`);
            
            const beforeResult = await this.core.executeCommand(`adb shell uiautomator dump /sdcard/ui_dump_${beforeTag}.xml`);
            const afterResult = await this.core.executeCommand(`adb shell uiautomator dump /sdcard/ui_dump_${afterTag}.xml`);
            
            if (beforeResult.success && afterResult.success) {
                // Compare UI states
                const beforeContent = require('fs').readFileSync(`artifacts/ui/${beforeTag}.xml`, 'utf8');
                const afterContent = require('fs').readFileSync(`artifacts/ui/${afterTag}.xml`, 'utf8');
                
                const changes = this.compareUIStates(beforeContent, afterContent);
                
                if (changes.length > 0) {
                    console.log(`✅ UI state changes detected: ${changes.length} changes`);
                    return { success: true, changes };
                } else {
                    console.warn('⚠️ No UI state changes detected');
                    return { success: false, reason: 'No changes detected' };
                }
            }
            
            return { success: false, reason: 'UI dump comparison failed' };
        } catch (error) {
            console.error('❌ UI state validation failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Compare UI states
     */
    compareUIStates(before, after) {
        const changes = [];
        
        // Simple comparison - in a real implementation, this would be more sophisticated
        if (before !== after) {
            changes.push({
                type: 'content_change',
                description: 'UI content has changed'
            });
        }
        
        return changes;
    }

    /**
     * Monitor real-time UI updates
     */
    async monitorRealTimeUIUpdates(duration = 5000) {
        try {
            console.log(`🔍 Monitoring real-time UI updates for ${duration}ms`);
            
            const startTime = Date.now();
            const updates = [];
            
            while (Date.now() - startTime < duration) {
                const result = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump_current.xml');
                if (result.success) {
                    updates.push({
                        timestamp: Date.now(),
                        success: true
                    });
                }
                
                await this.core.sleep(500);
            }
            
            console.log(`✅ Real-time UI monitoring completed: ${updates.length} updates captured`);
            return { success: true, updates };
        } catch (error) {
            console.error('❌ Real-time UI monitoring failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Detect element with state
     */
    async detectElementWithState(selector, expectedState) {
        try {
            console.log(`🔍 Detecting element ${selector} with state: ${expectedState}`);
            
            const result = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (result.success) {
                const content = require('fs').readFileSync('artifacts/ui/current.xml', 'utf8');
                
                // Look for element with specific state
                const statePattern = new RegExp(`${expectedState}="true"`, 'i');
                const elementPattern = new RegExp(`content-desc="${selector}"[^>]*${statePattern.source}`, 'i');
                
                if (content.match(elementPattern)) {
                    console.log(`✅ Element ${selector} found with state ${expectedState}`);
                    return { success: true, element: selector, state: expectedState };
                } else {
                    return { success: false, reason: `Element not found with state ${expectedState}` };
                }
            }
            
            return { success: false, reason: 'UI dump failed' };
        } catch (error) {
            console.error('❌ Element state detection failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Get interaction history
     */
    getInteractionHistory() {
        return {
            total: this.interactionHistory.length,
            interactions: this.interactionHistory
        };
    }

    /**
     * Clear interaction history
     */
    clearInteractionHistory() {
        this.interactionHistory = [];
        console.log('🧹 Interaction history cleared');
    }
}

module.exports = PluctUI05Interaction;
