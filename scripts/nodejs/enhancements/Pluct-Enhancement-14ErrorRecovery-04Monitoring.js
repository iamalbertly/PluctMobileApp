/**
 * Pluct-Enhancement-14ErrorRecovery-04Monitoring - Error monitoring module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles error monitoring, statistics, and history tracking
 */
class PluctEnhancementErrorMonitoring {
    constructor(core) {
        this.core = core;
        this.errorHistory = [];
        this.errorStatistics = {
            total: 0,
            byType: new Map(),
            bySeverity: new Map(),
            recoverySuccess: 0,
            recoveryFailure: 0
        };
    }

    /**
     * Set up error monitoring
     */
    async setupErrorMonitoring() {
        try {
            this.core.logger.info('Setting up error monitoring...');
            
            // Initialize statistics
            this.errorStatistics = {
                total: 0,
                byType: new Map(),
                bySeverity: new Map(),
                recoverySuccess: 0,
                recoveryFailure: 0
            };
            
            this.core.logger.info('✅ Error monitoring setup completed');
            return { success: true };
        } catch (error) {
            this.core.logger.error(`Error monitoring setup failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Record error recovery
     */
    async recordErrorRecovery(error, classification, strategy, recoveryResult) {
        try {
            const record = {
                timestamp: new Date().toISOString(),
                error: {
                    message: error.message,
                    stack: error.stack
                },
                classification: classification,
                strategy: strategy,
                recoveryResult: recoveryResult
            };
            
            this.errorHistory.push(record);
            
            // Update statistics
            this.updateStatistics(classification, recoveryResult);
            
            // Log recovery attempt
            if (recoveryResult.success) {
                this.core.logger.info(`✅ Error recovery successful: ${classification.type}`);
            } else {
                this.core.logger.warn(`⚠️ Error recovery failed: ${classification.type}`);
            }
            
            return { success: true };
        } catch (error) {
            this.core.logger.error(`Error recovery recording failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Update error statistics
     */
    updateStatistics(classification, recoveryResult) {
        try {
            // Update total count
            this.errorStatistics.total++;
            
            // Update by type
            const typeCount = this.errorStatistics.byType.get(classification.type) || 0;
            this.errorStatistics.byType.set(classification.type, typeCount + 1);
            
            // Update by severity
            const severityCount = this.errorStatistics.bySeverity.get(classification.severity) || 0;
            this.errorStatistics.bySeverity.set(classification.severity, severityCount + 1);
            
            // Update recovery statistics
            if (recoveryResult.success) {
                this.errorStatistics.recoverySuccess++;
            } else {
                this.errorStatistics.recoveryFailure++;
            }
        } catch (error) {
            this.core.logger.error(`Statistics update failed: ${error.message}`);
        }
    }

    /**
     * Get error statistics
     */
    getErrorStatistics() {
        try {
            return {
                total: this.errorStatistics.total,
                byType: Object.fromEntries(this.errorStatistics.byType),
                bySeverity: Object.fromEntries(this.errorStatistics.bySeverity),
                recoverySuccess: this.errorStatistics.recoverySuccess,
                recoveryFailure: this.errorStatistics.recoveryFailure,
                recoverySuccessRate: this.calculateSuccessRate()
            };
        } catch (error) {
            this.core.logger.error(`Statistics retrieval failed: ${error.message}`);
            return null;
        }
    }

    /**
     * Calculate recovery success rate
     */
    calculateSuccessRate() {
        try {
            const totalRecoveries = this.errorStatistics.recoverySuccess + this.errorStatistics.recoveryFailure;
            if (totalRecoveries === 0) return 0;
            return (this.errorStatistics.recoverySuccess / totalRecoveries) * 100;
        } catch (error) {
            return 0;
        }
    }

    /**
     * Get error history
     */
    getErrorHistory(limit = 50) {
        try {
            return this.errorHistory.slice(-limit);
        } catch (error) {
            this.core.logger.error(`Error history retrieval failed: ${error.message}`);
            return [];
        }
    }

    /**
     * Clear error history
     */
    clearErrorHistory() {
        try {
            this.errorHistory = [];
            this.errorStatistics = {
                total: 0,
                byType: new Map(),
                bySeverity: new Map(),
                recoverySuccess: 0,
                recoveryFailure: 0
            };
            this.core.logger.info('Error history cleared');
        } catch (error) {
            this.core.logger.error(`Error history clear failed: ${error.message}`);
        }
    }

    /**
     * Export error data
     */
    exportErrorData() {
        try {
            return {
                statistics: this.getErrorStatistics(),
                history: this.getErrorHistory(),
                exportTimestamp: new Date().toISOString()
            };
        } catch (error) {
            this.core.logger.error(`Error data export failed: ${error.message}`);
            return null;
        }
    }

    /**
     * Get error trends
     */
    getErrorTrends(timeWindow = 3600000) { // 1 hour default
        try {
            const cutoffTime = new Date(Date.now() - timeWindow);
            const recentErrors = this.errorHistory.filter(record => 
                new Date(record.timestamp) > cutoffTime
            );
            
            return {
                recentCount: recentErrors.length,
                timeWindow: timeWindow,
                trends: this.analyzeTrends(recentErrors)
            };
        } catch (error) {
            this.core.logger.error(`Error trends analysis failed: ${error.message}`);
            return null;
        }
    }

    /**
     * Analyze error trends
     */
    analyzeTrends(errors) {
        try {
            const trends = {
                mostCommonType: null,
                mostCommonSeverity: null,
                averageRecoveryTime: 0
            };
            
            if (errors.length === 0) return trends;
            
            // Find most common type
            const typeCounts = new Map();
            errors.forEach(error => {
                const type = error.classification.type;
                typeCounts.set(type, (typeCounts.get(type) || 0) + 1);
            });
            
            let maxCount = 0;
            for (const [type, count] of typeCounts) {
                if (count > maxCount) {
                    maxCount = count;
                    trends.mostCommonType = type;
                }
            }
            
            return trends;
        } catch (error) {
            return {};
        }
    }
}

module.exports = PluctEnhancementErrorMonitoring;
