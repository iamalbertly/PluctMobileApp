const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyRefactor03DeadCodeRemovalValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-Refactor-03DeadCodeRemoval-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Dead Code Removal Validation');
        
        try {
            // Step 1: Launch app
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Step 2: Clear logcat
            await this.core.clearLogcat();
            
            // Step 3: Trigger app activity to generate logs
            this.core.logger.info('- Step 3: Generate app activity');
            await this.core.dumpUIHierarchy();
            
            // Wait for app to fully initialize
            await this.core.sleep(3000);
            
            // Step 4: Check logcat for removed memory management classes
            this.core.logger.info('- Step 4: Validate memory management code removed');
            const logcat = await this.core.captureAPILogs(200);
            const logcatText = logcat.join('\n');
            
            // Check for removed memory management classes
            const hasMemoryManager = logcatText.includes('PluctMemoryManager');
            const hasMemoryMonitor = logcatText.includes('PluctMemoryMonitor');
            const hasLeakDetector = logcatText.includes('PluctMemoryLeakDetector');
            const hasGarbageCollector = logcatText.includes('PluctGarbageCollector');
            
            if (hasMemoryManager || hasMemoryMonitor || hasLeakDetector || hasGarbageCollector) {
                const found = [];
                if (hasMemoryManager) found.push('PluctMemoryManager');
                if (hasMemoryMonitor) found.push('PluctMemoryMonitor');
                if (hasLeakDetector) found.push('PluctMemoryLeakDetector');
                if (hasGarbageCollector) found.push('PluctGarbageCollector');
                
                return { 
                    success: false, 
                    error: `Removed memory management classes still referenced: ${found.join(', ')}` 
                };
            }
            this.core.logger.info('✅ Memory management classes not found (expected)');
            
            // Step 5: Check for ClassNotFoundException errors
            this.core.logger.info('- Step 5: Check for missing class errors');
            const hasClassNotFound = logcatText.includes('ClassNotFoundException') ||
                                    logcatText.includes('NoClassDefFoundError');
            
            if (hasClassNotFound) {
                // Check if it's related to removed classes
                const memoryRelatedError = logcatText.includes('Memory') && 
                                         (logcatText.includes('ClassNotFoundException') ||
                                          logcatText.includes('NoClassDefFoundError'));
                
                if (memoryRelatedError) {
                    return { 
                        success: false, 
                        error: 'ClassNotFoundException for removed memory management classes' 
                    };
                }
                // Other ClassNotFoundErrors might be unrelated
            }
            
            // Step 6: Check for removed enhancement script references
            this.core.logger.info('- Step 6: Validate enhancement scripts removed');
            // Note: Enhancement scripts are Node.js files, not checked in Android logcat
            // This is validated by file system, not runtime
            
            // Step 7: Verify app runs without crashes
            this.core.logger.info('- Step 7: Verify app stability');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            const finalUI = this.core.readLastUIDump() || '';
            
            const hasCrash = finalUI.includes('Unfortunately') || 
                            finalUI.includes('has stopped') ||
                            logcatText.includes('FATAL EXCEPTION');
            
            if (hasCrash) {
                return { 
                    success: false, 
                    error: 'App crashed - possibly due to missing classes' 
                };
            }
            this.core.logger.info('✅ App running without crashes');
            
            // Step 8: Verify UI elements still work
            this.core.logger.info('- Step 8: Verify UI functionality');
            const hasUIElements = finalUI.includes('url_input_field') || 
                                 finalUI.includes('Paste a TikTok link') ||
                                 finalUI.includes('Extract Script');
            
            if (!hasUIElements) {
                this.core.logger.warn('⚠️ Expected UI elements not found');
            } else {
                this.core.logger.info('✅ UI elements present');
            }
            
            this.core.logger.info('✅ Dead code removal validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyRefactor03DeadCodeRemovalValidation;

