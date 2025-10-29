const PluctEnhancementErrorClassification = require('./Pluct-Enhancement-14ErrorRecovery-02Classification');
const PluctEnhancementRecoveryStrategies = require('./Pluct-Enhancement-14ErrorRecovery-03Strategies');
const PluctEnhancementErrorMonitoring = require('./Pluct-Enhancement-14ErrorRecovery-04Monitoring');

/**
 * Pluct-Enhancement-14ErrorRecovery - Intelligent error recovery orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Orchestrates error recovery, classification, and monitoring
 */
class PluctEnhancement14ErrorRecovery {
    constructor(core) {
        this.core = core;
        this.errorTypes = new Map();
        this.recoveryStrategies = new Map();
        this.errorHistory = [];
        
        // Initialize specialized modules
        this.classification = new PluctEnhancementErrorClassification(core);
        this.strategies = new PluctEnhancementRecoveryStrategies(core);
        this.monitoring = new PluctEnhancementErrorMonitoring(core);
    }

    /**
     * ENHANCEMENT 14: Add intelligent error recovery and resilience mechanisms
     */
    async implementIntelligentErrorRecovery() {
        this.core.logger.info('🛡️ Implementing intelligent error recovery...');
        
        try {
            // Set up error classification
            await this.classification.setupErrorClassification();
            
            // Implement recovery strategies
            await this.strategies.implementRecoveryStrategies();
            
            // Set up error monitoring
            await this.monitoring.setupErrorMonitoring();
            
            this.core.logger.info('✅ Intelligent error recovery implemented successfully');
            return { success: true };
        } catch (error) {
            this.core.logger.error(`❌ Error recovery implementation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Handle error with intelligent recovery
     */
    async handleError(error, context = {}) {
        try {
            // Classify error
            const classification = await this.classification.classifyError(error, context);
            
            // Get recovery strategy
            const strategy = await this.strategies.getRecoveryStrategy(classification);
            
            // Execute recovery
            const recoveryResult = await this.strategies.executeRecovery(strategy, error, context);
            
            // Monitor recovery
            await this.monitoring.recordErrorRecovery(error, classification, strategy, recoveryResult);
            
            return recoveryResult;
        } catch (recoveryError) {
            this.core.logger.error(`Error recovery failed: ${recoveryError.message}`);
            return { success: false, error: recoveryError.message };
        }
    }

    /**
     * Get error statistics
     */
    getErrorStatistics() {
        return this.monitoring.getErrorStatistics();
    }

    /**
     * Clear error history
     */
    clearErrorHistory() {
        this.errorHistory = [];
        this.monitoring.clearErrorHistory();
    }
}

module.exports = PluctEnhancement14ErrorRecovery;