/**
 * Pluct-Core-01Foundation-08ErrorRecovery - Comprehensive error recovery system
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctCoreFoundationErrorRecovery {
    constructor() {
        this.logger = new PluctLogger();
        this.errorHistory = [];
        this.recoveryStrategies = new Map();
        this.circuitBreaker = {
            failures: 0,
            lastFailureTime: null,
            state: 'CLOSED' // CLOSED, OPEN, HALF_OPEN
        };
        this.maxFailures = 5;
        this.timeoutMs = 60000; // 1 minute
    }

    /**
     * Register recovery strategy
     */
    registerRecoveryStrategy(errorType, strategy) {
        this.recoveryStrategies.set(errorType, strategy);
        this.logger.info(`🛡️ Registered recovery strategy for: ${errorType}`);
    }

    /**
     * Handle error with recovery
     */
    async handleErrorWithRecovery(error, context = {}) {
        this.logger.error(`❌ Error occurred: ${error.message}`, error);
        
        // Record error
        this.recordError(error, context);
        
        // Check circuit breaker
        if (this.isCircuitBreakerOpen()) {
            this.logger.warn('🔒 Circuit breaker is OPEN, skipping recovery');
            return { success: false, error: 'Circuit breaker is open' };
        }
        
        // Find recovery strategy
        const strategy = this.findRecoveryStrategy(error);
        if (!strategy) {
            this.logger.warn('⚠️ No recovery strategy found for error type');
            return { success: false, error: 'No recovery strategy available' };
        }
        
        // Execute recovery
        try {
            this.logger.info(`🔄 Executing recovery strategy: ${strategy.name}`);
            const result = await strategy.execute(error, context);
            
            if (result.success) {
                this.logger.info('✅ Recovery strategy succeeded');
                this.resetCircuitBreaker();
                return result;
            } else {
                this.logger.error('❌ Recovery strategy failed');
                this.recordFailure();
                return result;
            }
        } catch (recoveryError) {
            this.logger.error('❌ Recovery strategy threw error', recoveryError);
            this.recordFailure();
            return { success: false, error: recoveryError.message };
        }
    }

    /**
     * Record error
     */
    recordError(error, context) {
        const errorRecord = {
            timestamp: new Date().toISOString(),
            message: error.message,
            stack: error.stack,
            type: error.constructor.name,
            context,
            id: this.generateErrorId()
        };
        
        this.errorHistory.push(errorRecord);
        
        // Keep only last 100 errors
        if (this.errorHistory.length > 100) {
            this.errorHistory.shift();
        }
        
        this.logger.info(`📝 Recorded error: ${errorRecord.id}`);
    }

    /**
     * Find recovery strategy
     */
    findRecoveryStrategy(error) {
        const errorType = error.constructor.name;
        
        // Direct match
        if (this.recoveryStrategies.has(errorType)) {
            return this.recoveryStrategies.get(errorType);
        }
        
        // Pattern matching
        for (const [pattern, strategy] of this.recoveryStrategies) {
            if (error.message.includes(pattern) || errorType.includes(pattern)) {
                return strategy;
            }
        }
        
        return null;
    }

    /**
     * Check if circuit breaker is open
     */
    isCircuitBreakerOpen() {
        if (this.circuitBreaker.state === 'OPEN') {
            const timeSinceLastFailure = Date.now() - this.circuitBreaker.lastFailureTime;
            if (timeSinceLastFailure > this.timeoutMs) {
                this.circuitBreaker.state = 'HALF_OPEN';
                this.logger.info('🔄 Circuit breaker moved to HALF_OPEN');
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Record failure
     */
    recordFailure() {
        this.circuitBreaker.failures++;
        this.circuitBreaker.lastFailureTime = Date.now();
        
        if (this.circuitBreaker.failures >= this.maxFailures) {
            this.circuitBreaker.state = 'OPEN';
            this.logger.warn('🔒 Circuit breaker opened due to failures');
        }
    }

    /**
     * Reset circuit breaker
     */
    resetCircuitBreaker() {
        this.circuitBreaker.failures = 0;
        this.circuitBreaker.state = 'CLOSED';
        this.logger.info('🔄 Circuit breaker reset to CLOSED');
    }

    /**
     * Generate error ID
     */
    generateErrorId() {
        return `err_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`;
    }

    /**
     * Get error statistics
     */
    getErrorStatistics() {
        const now = Date.now();
        const last24h = now - (24 * 60 * 60 * 1000);
        
        const recentErrors = this.errorHistory.filter(error => 
            new Date(error.timestamp).getTime() > last24h
        );
        
        const errorTypes = {};
        recentErrors.forEach(error => {
            errorTypes[error.type] = (errorTypes[error.type] || 0) + 1;
        });
        
        return {
            totalErrors: this.errorHistory.length,
            recentErrors: recentErrors.length,
            errorTypes,
            circuitBreakerState: this.circuitBreaker.state,
            failures: this.circuitBreaker.failures
        };
    }

    /**
     * Clear error history
     */
    clearErrorHistory() {
        this.errorHistory = [];
        this.logger.info('🧹 Error history cleared');
    }

    /**
     * Get recovery strategies
     */
    getRecoveryStrategies() {
        return Array.from(this.recoveryStrategies.keys());
    }

    /**
     * Remove recovery strategy
     */
    removeRecoveryStrategy(errorType) {
        if (this.recoveryStrategies.has(errorType)) {
            this.recoveryStrategies.delete(errorType);
            this.logger.info(`🗑️ Removed recovery strategy for: ${errorType}`);
        }
    }
}

module.exports = PluctCoreFoundationErrorRecovery;
