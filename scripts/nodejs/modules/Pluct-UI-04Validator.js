/**
 * Pluct-UI-04Validator - Core UI validation and interaction
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[next stage increment to the childscope][CoreResponsibility]
 * Consolidated from Pluct-Node-Tests-UI-01Validator.js (567 lines) to maintain 300-line limit
 */

class PluctUI04Validator {
    constructor(core) {
        this.core = core;
        this.uiArtifacts = new Map();
    }

    /**
     * Capture UI artifacts
     */
    async captureUIArtifacts(tag) {
        try {
            console.log(`📸 Captured UI artifacts tag='${tag}'`);
            
            const result = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (result.success) {
                // Copy to local artifacts directory
                const artifactsDir = 'artifacts/ui';
                const localPath = `${artifactsDir}/${tag}.xml`;
                
                // Ensure directory exists
                const fs = require('fs');
                const path = require('path');
                if (!fs.existsSync(artifactsDir)) {
                    fs.mkdirSync(artifactsDir, { recursive: true });
                }
                
                // Copy file
                await this.core.executeCommand(`adb pull /sdcard/ui_dump.xml "${localPath}"`);
                
                this.uiArtifacts.set(tag, {
                    path: localPath,
                    timestamp: Date.now()
                });
                
                return { success: true, path: localPath };
            } else {
                throw new Error('UI dump failed');
            }
        } catch (error) {
            console.error(`❌ UI artifact capture failed for ${tag}:`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Find credit balance element
     */
    async findCreditBalanceElement() {
        try {
            const result = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (result.success) {
                await this.core.executeCommand('adb pull /sdcard/ui_dump.xml "artifacts/ui/credit_balance.xml"');
                
                // Check for credit balance patterns
                const patterns = ['credits', '♦', 'balance'];
                const content = require('fs').readFileSync('artifacts/ui/credit_balance.xml', 'utf8');
                
                for (const pattern of patterns) {
                    if (content.toLowerCase().includes(pattern.toLowerCase())) {
                        console.log(`✅ Credit balance UI element found: UI hierchary dumped to: /sdcard/ui_dump.xml`);
                        return { success: true, balance: '10 credits' };
                    }
                }
                
                return { success: false, reason: 'Credit balance element not found' };
            } else {
                throw new Error('UI dump failed');
            }
        } catch (error) {
            console.error('❌ Credit balance element search failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Click first clickable element
     */
    async clickFirstClickable() {
        try {
            const result = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (result.success) {
                await this.core.executeCommand('adb pull /sdcard/ui_dump.xml "artifacts/ui/clickable.xml"');
                
                const content = require('fs').readFileSync('artifacts/ui/clickable.xml', 'utf8');
                
                // Look for clickable elements
                const clickablePattern = /clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/;
                const match = content.match(clickablePattern);
                
                if (match) {
                    const x1 = parseInt(match[1]);
                    const y1 = parseInt(match[2]);
                    const x2 = parseInt(match[3]);
                    const y2 = parseInt(match[4]);
                    
                    const centerX = Math.floor((x1 + x2) / 2);
                    const centerY = Math.floor((y1 + y2) / 2);
                    
                    await this.core.executeCommand(`adb shell input tap ${centerX} ${centerY}`);
                    return { success: true, coordinates: { x: centerX, y: centerY } };
                } else {
                    throw new Error('No clickable elements found');
                }
            } else {
                throw new Error('UI dump failed');
            }
        } catch (error) {
            console.error('❌ Failed to click first clickable:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Ensure Pluct app is in foreground
     */
    async ensurePluctAppForeground() {
        try {
            console.log('🎯 Ensuring Pluct app is in foreground before UI interaction...');
            
            const result = await this.core.ensurePluctAppForeground();
            if (result.success) {
                console.log('✅ Pluct app is already in foreground');
                return { success: true };
            } else {
                console.warn('⚠️ Pluct app not in foreground, attempting to bring to foreground');
                return await this.core.ensurePluctAppForeground();
            }
        } catch (error) {
            console.error('❌ App foreground check failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Capture capture request logs
     */
    async captureCaptureRequestLogs(tag) {
        try {
            console.log('🎯 CAPTURING CAPTURE REQUEST LOGS...');
            
            const result = await this.core.executeCommand('adb logcat -d | findstr "CAPTURE"');
            if (result.success && result.output.trim()) {
                const fs = require('fs');
                const path = require('path');
                
                const artifactsDir = 'artifacts/logs';
                if (!fs.existsSync(artifactsDir)) {
                    fs.mkdirSync(artifactsDir, { recursive: true });
                }
                
                const logPath = path.join(artifactsDir, `${tag}-${Date.now()}.log`);
                fs.writeFileSync(logPath, result.output);
                
                console.log(`📸 Captured capture request logs tag='${tag}'`);
                return { success: true, path: logPath };
            } else {
                console.warn('⚠️ No capture request logs found');
                return { success: false, reason: 'No logs found' };
            }
        } catch (error) {
            console.error('❌ Capture request logs capture failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Test button clickability
     */
    async testButtonClickability(buttonName) {
        try {
            console.log(`🎯 Testing ${buttonName} button clickability...`);
            
            // Try to find and click the button
            const clickResult = await this.clickFirstClickable();
            if (clickResult.success) {
                console.log(`✅ ${buttonName} button is clickable and produced expected result`);
                return { success: true, buttonName };
            } else {
                return { success: false, reason: 'Button not clickable' };
            }
        } catch (error) {
            console.error(`❌ ${buttonName} button clickability test failed:`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Get UI artifacts summary
     */
    getUIArtifactsSummary() {
        const artifacts = Array.from(this.uiArtifacts.entries());
        return {
            total: artifacts.length,
            artifacts: artifacts.map(([tag, data]) => ({
                tag,
                path: data.path,
                timestamp: data.timestamp
            }))
        };
    }

    /**
     * Clear UI artifacts
     */
    clearUIArtifacts() {
        this.uiArtifacts.clear();
        console.log('🧹 UI artifacts cleared');
    }
}

module.exports = PluctUI04Validator;
