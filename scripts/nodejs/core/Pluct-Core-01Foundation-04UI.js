/**
 * Pluct-Core-01Foundation-04UI - UI interaction functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctCoreFoundationUI {
    constructor() {
        this.logger = new PluctLogger();
        this.uiState = {
            currentScreen: null,
            lastInteraction: null,
            uiHierarchy: null
        };
    }

    /**
     * Dump UI hierarchy
     */
    async dumpUIHierarchy() {
        try {
            this.logger.info('📱 Dumping UI hierarchy...');
            
            const result = await this.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (!result.success) {
                throw new Error('Failed to dump UI hierarchy');
            }
            
            const pullResult = await this.executeCommand('adb pull /sdcard/ui_dump.xml ./ui_dump.xml');
            if (!pullResult.success) {
                throw new Error('Failed to pull UI dump');
            }
            
            const fs = require('fs');
            const uiDump = fs.readFileSync('./ui_dump.xml', 'utf8');
            
            this.uiState.uiHierarchy = uiDump;
            this.logger.info('✅ UI hierarchy dumped successfully');
            
            return { success: true, uiDump };
        } catch (error) {
            this.logger.error('❌ Failed to dump UI hierarchy:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Read last UI dump
     */
    readLastUIDump() {
        try {
            const fs = require('fs');
            if (fs.existsSync('./ui_dump.xml')) {
                return fs.readFileSync('./ui_dump.xml', 'utf8');
            }
            return this.uiState.uiHierarchy || '';
        } catch (error) {
            this.logger.warn('⚠️ Failed to read UI dump:', error.message);
            return '';
        }
    }

    /**
     * Tap by text
     */
    async tapByText(text, timeout = 5000) {
        try {
            this.logger.info(`🔍 Tapping by text: "${text}"`);
            
            // First dump UI to find the element
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            const element = this.findElementByText(uiDump, text);
            if (!element) {
                throw new Error(`Element with text "${text}" not found`);
            }
            
            const result = await this.tapElement(element);
            if (!result.success) {
                throw new Error('Failed to tap element');
            }
            
            this.uiState.lastInteraction = { type: 'tapByText', text, timestamp: Date.now() };
            this.logger.info(`✅ Tapped by text: "${text}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Failed to tap by text "${text}":`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by test tag
     */
    async tapByTestTag(testTag, timeout = 5000) {
        try {
            this.logger.info(`🔍 Tapping by test tag: "${testTag}"`);
            
            // First dump UI to find the element
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            const element = this.findElementByTestTag(uiDump, testTag);
            if (!element) {
                throw new Error(`Element with test tag "${testTag}" not found`);
            }
            
            const result = await this.tapElement(element);
            if (!result.success) {
                throw new Error('Failed to tap element');
            }
            
            this.uiState.lastInteraction = { type: 'tapByTestTag', testTag, timestamp: Date.now() };
            this.logger.info(`✅ Tapped by test tag: "${testTag}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Failed to tap by test tag "${testTag}":`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap first edit text
     */
    async tapFirstEditText() {
        try {
            this.logger.info('🔍 Tapping first edit text...');
            
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            const element = this.findFirstEditText(uiDump);
            if (!element) {
                throw new Error('No edit text found');
            }
            
            const result = await this.tapElement(element);
            if (!result.success) {
                throw new Error('Failed to tap edit text');
            }
            
            this.uiState.lastInteraction = { type: 'tapFirstEditText', timestamp: Date.now() };
            this.logger.info('✅ Tapped first edit text');
            
            return { success: true };
        } catch (error) {
            this.logger.error('❌ Failed to tap first edit text:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Input text
     */
    async inputText(text) {
        try {
            this.logger.info(`📝 Inputting text: "${text}"`);
            
            const result = await this.executeCommand(`adb shell input text "${text}"`);
            if (!result.success) {
                throw new Error('Failed to input text');
            }
            
            this.uiState.lastInteraction = { type: 'inputText', text, timestamp: Date.now() };
            this.logger.info(`✅ Text inputted: "${text}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Failed to input text:`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Wait for text
     */
    async waitForText(text, timeout = 5000) {
        const startTime = Date.now();
        
        while (Date.now() - startTime < timeout) {
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            if (uiDump.includes(text)) {
                this.logger.info(`✅ Found text: "${text}"`);
                return { success: true };
            }
            
            await this.sleep(500);
        }
        
        this.logger.warn(`⚠️ Text not found within timeout: "${text}"`);
        return { success: false, error: 'Text not found' };
    }

    /**
     * Find element by test tag
     */
    findElementByTestTag(uiDump, testTag) {
        const regex = new RegExp(`test-tag="${testTag}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
        const match = uiDump.match(regex);
        
        if (match) {
            const [, x1, y1, x2, y2] = match;
            return {
                testTag,
                bounds: {
                    x1: parseInt(x1),
                    y1: parseInt(y1),
                    x2: parseInt(x2),
                    y2: parseInt(y2)
                }
            };
        }
        
        return null;
    }

    /**
     * Find element by text
     */
    findElementByText(uiDump, text) {
        const regex = new RegExp(`text="${text}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
        const match = uiDump.match(regex);
        
        if (match) {
            const [, x1, y1, x2, y2] = match;
            return {
                text,
                bounds: {
                    x1: parseInt(x1),
                    y1: parseInt(y1),
                    x2: parseInt(x2),
                    y2: parseInt(y2)
                }
            };
        }
        
        return null;
    }

    /**
     * Find first edit text
     */
    findFirstEditText(uiDump) {
        const regex = /class="android\.widget\.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/i;
        const match = uiDump.match(regex);
        
        if (match) {
            const [, x1, y1, x2, y2] = match;
            return {
                type: 'EditText',
                bounds: {
                    x1: parseInt(x1),
                    y1: parseInt(y1),
                    x2: parseInt(x2),
                    y2: parseInt(y2)
                }
            };
        }
        
        return null;
    }

    /**
     * Tap element
     */
    async tapElement(element) {
        const centerX = Math.floor((element.bounds.x1 + element.bounds.x2) / 2);
        const centerY = Math.floor((element.bounds.y1 + element.bounds.y2) / 2);
        
        this.logger.info(`📍 Tapping at coordinates: ${centerX}, ${centerY}`);
        
        return this.executeCommand(`adb shell input tap ${centerX} ${centerY}`);
    }

    /**
     * Get UI state
     */
    getUIState() {
        return this.uiState;
    }

    /**
     * Execute command
     */
    async executeCommand(command, timeout = 5000) {
        try {
            const { exec } = require('child_process');
            const { promisify } = require('util');
            const execAsync = promisify(exec);
            
            const { stdout, stderr } = await execAsync(command, { timeout });
            
            return { 
                success: true, 
                output: stdout, 
                error: stderr,
                fullOutput: stdout + stderr
            };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

module.exports = PluctCoreFoundationUI;
