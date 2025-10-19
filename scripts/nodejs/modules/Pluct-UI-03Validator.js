// Pluct-UI-03Validator.js
// Consolidated UI validator - Single source of truth
// Format: [Project]-[ParentScope]-[ChildScope]-[increment][CoreResponsibility]

const { execOk, execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const path = require('path');
const fs = require('fs');

class PluctUIValidator {
    constructor() {
        this.artifactsDir = path.join(__dirname, '../../artifacts/ui');
        this.ensureArtifactsDir();
    }

    ensureArtifactsDir() {
        try {
            if (!fs.existsSync(this.artifactsDir)) {
                fs.mkdirSync(this.artifactsDir, { recursive: true });
            }
        } catch (error) {
            logWarn(`Failed to create artifacts directory: ${error.message}`, 'UIValidator');
        }
    }

    // Unified execution methods
    async executeCommand(command, timeout = 30000) {
        try {
            const result = execOut(command);
            return { success: true, output: result, error: null };
        } catch (error) {
            return { success: false, output: null, error: error.message };
        }
    }

    // Unified logging methods
    logInfo(message, component = 'UIValidator') {
        logInfo(message, component);
    }

    logSuccess(message, component = 'UIValidator') {
        logSuccess(message, component);
    }

    logWarn(message, component = 'UIValidator') {
        logWarn(message, component);
    }

    logError(message, component = 'UIValidator') {
        logError(message, component);
    }

    // UI hierarchy dumping
    async dumpHierarchy(deviceId = 'emulator-5554', outputDir = 'artifacts/ui', tag = 'dump') {
        this.logInfo(`Dumping UI hierarchy with tag: ${tag}`, 'UIValidator');
        
        try {
            const timestamp = Date.now();
            const xmlFile = `${tag}-${timestamp}.xml`;
            const xmlPath = path.join(outputDir, xmlFile);
            
            const result = await this.executeCommand(`adb shell uiautomator dump /sdcard/ui_dump.xml`);
            if (!result.success) {
                return { success: false, error: 'Failed to dump UI hierarchy' };
            }

            const pullResult = await this.executeCommand(`adb pull /sdcard/ui_dump.xml "${xmlPath}"`);
            if (!pullResult.success) {
                return { success: false, error: 'Failed to pull UI dump' };
            }

            const xml = fs.readFileSync(xmlPath, 'utf8');
            this.logSuccess(`UI hierarchy dumped to: ${xmlPath}`, 'UIValidator');
            
            return { success: true, xml: xml, path: xmlPath };
        } catch (error) {
            this.logError(`UI hierarchy dumping failed: ${error.message}`, 'UIValidator');
            return { success: false, error: error.message };
        }
    }

    // Parse UI hierarchy XML
    parseUIHierarchy(xml) {
        try {
            const nodes = [];
            const nodeRegex = /<node[^>]*>/g;
            let match;
            
            while ((match = nodeRegex.exec(xml)) !== null) {
                const nodeXml = match[0];
                const node = this.parseNode(nodeXml);
                if (node) {
                    nodes.push(node);
                }
            }
            
            return nodes;
        } catch (error) {
            this.logError(`UI hierarchy parsing failed: ${error.message}`, 'UIValidator');
            return [];
        }
    }

    // Parse individual node
    parseNode(nodeXml) {
        try {
            const node = {};
            
            // Extract attributes using robust regex
            const attrRegex = /(\w+)="([^"]*)"/g;
            let match;
            
            while ((match = attrRegex.exec(nodeXml)) !== null) {
                const [, key, value] = match;
                node[key] = value;
            }
            
            // Parse bounds
            if (node.bounds) {
                const boundsMatch = node.bounds.match(/\[(\d+),(\d+)\]\[(\d+),(\d+)\]/);
                if (boundsMatch) {
                    node.x1 = parseInt(boundsMatch[1]);
                    node.y1 = parseInt(boundsMatch[2]);
                    node.x2 = parseInt(boundsMatch[3]);
                    node.y2 = parseInt(boundsMatch[4]);
                }
            }
            
            // Parse clickable
            node.clickable = node.clickable === 'true';
            node.enabled = node.enabled === 'true';
            
            return node;
        } catch (error) {
            this.logWarn(`Failed to parse node: ${error.message}`, 'UIValidator');
            return null;
        }
    }

    // Capture UI artifacts
    async captureUiArtifacts(tag) {
        this.logInfo(`Captured UI artifacts tag='${tag}'`, 'UIValidator');
        
        try {
            const { success, xml } = await this.dumpHierarchy('emulator-5554', this.artifactsDir, tag);
            if (success) {
                return { success: true, xml: xml };
            }
            return { success: false, error: 'Failed to capture UI artifacts' };
        } catch (error) {
            this.logError(`UI artifact capture failed: ${error.message}`, 'UIValidator');
            return { success: false, error: error.message };
        }
    }

    // Click first clickable element
    async clickFirstClickable() {
        this.logInfo('Clicking first clickable element...', 'UIValidator');
        
        try {
            const { success, xml } = await this.dumpHierarchy('emulator-5554', this.artifactsDir, 'clickable');
            if (!success) {
                return { success: false, error: 'Failed to dump UI hierarchy' };
            }

            const nodes = this.parseUIHierarchy(xml);
            const clickableNodes = nodes.filter(node => node.clickable && node.enabled);
            
            if (clickableNodes.length === 0) {
                return { success: false, error: 'No clickable elements found' };
            }

            // Try to find Quick Scan button first
            const quickScanNode = clickableNodes.find(node => 
                node.contentDescription === 'quick_scan' || 
                node.text === 'Quick Scan' ||
                (node.contentDescription && node.contentDescription.includes('Quick Scan'))
            );

            if (quickScanNode && quickScanNode.x1 !== undefined) {
                const centerX = Math.floor((quickScanNode.x1 + quickScanNode.x2) / 2);
                const centerY = Math.floor((quickScanNode.y1 + quickScanNode.y2) / 2);
                
                const clickResult = await this.executeCommand(`adb shell input tap ${centerX} ${centerY}`);
                if (clickResult.success) {
                    this.logSuccess('Quick Scan button clicked successfully', 'UIValidator');
                    return { success: true };
                }
            }

            // Fallback to first clickable element
            const firstClickable = clickableNodes[0];
            if (firstClickable.x1 !== undefined) {
                const centerX = Math.floor((firstClickable.x1 + firstClickable.x2) / 2);
                const centerY = Math.floor((firstClickable.y1 + firstClickable.y2) / 2);
                
                const clickResult = await this.executeCommand(`adb shell input tap ${centerX} ${centerY}`);
                if (clickResult.success) {
                    this.logSuccess('First clickable element clicked successfully', 'UIValidator');
                    return { success: true };
                }
            }

            return { success: false, error: 'Failed to click any element' };
        } catch (error) {
            this.logError(`Failed to click first clickable: ${error.message}`, 'UIValidator');
            return { success: false, error: error.message };
        }
    }

    // Compare UI counts
    async compareUiCounts(beforeTag, afterTag) {
        this.logInfo(`Comparing UI counts: ${beforeTag} vs ${afterTag}`, 'UIValidator');
        
        try {
            const beforePath = path.join(this.artifactsDir, `${beforeTag}.xml`);
            const afterPath = path.join(this.artifactsDir, `${afterTag}.xml`);

            if (!fs.existsSync(beforePath) || !fs.existsSync(afterPath)) {
                this.logWarn('UI artifact files not found for comparison', 'UIValidator');
                return { success: false, error: 'UI artifact files not found' };
            }

            const beforeXml = fs.readFileSync(beforePath, 'utf8');
            const afterXml = fs.readFileSync(afterPath, 'utf8');

            const beforeNodes = this.parseUIHierarchy(beforeXml);
            const afterNodes = this.parseUIHierarchy(afterXml);

            const delta = afterNodes.length - beforeNodes.length;
            this.logInfo(`UI count delta: ${delta} (${beforeNodes.length} → ${afterNodes.length})`, 'UIValidator');

            return { success: true, delta: delta, before: beforeNodes.length, after: afterNodes.length };
        } catch (error) {
            this.logError(`UI count comparison failed: ${error.message}`, 'UIValidator');
            return { success: false, error: error.message };
        }
    }

    // Wait for element
    async waitForElement(type, value, timeoutSeconds = 10) {
        this.logInfo(`Waiting for element: ${type}="${value}"`, 'UIValidator');
        
        const startTime = Date.now();
        const timeout = timeoutSeconds * 1000;
        
        while (Date.now() - startTime < timeout) {
            const { success, xml } = await this.dumpHierarchy('emulator-5554', this.artifactsDir, 'wait');
            if (success) {
                const nodes = this.parseUIHierarchy(xml);
                const element = nodes.find(node => {
                    if (type === 'text') return node.text && node.text.includes(value);
                    if (type === 'desc') return node.contentDescription && node.contentDescription.includes(value);
                    if (type === 'testTag') return node.resourceId && node.resourceId.includes(value);
                    return false;
                });
                
                if (element) {
                    this.logSuccess(`Element found: ${type}="${value}"`, 'UIValidator');
                    return { found: true, element: element };
                }
            }
            
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
        
        this.logWarn(`Element not found within timeout: ${type}="${value}"`, 'UIValidator');
        return { found: false, element: null };
    }
}

module.exports = PluctUIValidator;
