/**
 * Pluct-Enhancement-14ErrorRecovery-02Classification - Error classification module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles error classification and categorization
 */
class PluctEnhancementErrorClassification {
    constructor(core) {
        this.core = core;
        this.errorTypes = new Map();
    }

    /**
     * Set up error classification
     */
    async setupErrorClassification() {
        try {
            // Define error types
            this.errorTypes.set('network', {
                patterns: ['network', 'connection', 'timeout', 'unreachable'],
                severity: 'medium',
                recoverable: true
            });
            
            this.errorTypes.set('authentication', {
                patterns: ['unauthorized', 'forbidden', 'token', 'auth'],
                severity: 'high',
                recoverable: true
            });
            
            this.errorTypes.set('validation', {
                patterns: ['invalid', 'validation', 'format', 'malformed'],
                severity: 'low',
                recoverable: true
            });
            
            this.errorTypes.set('server', {
                patterns: ['server', 'internal', '500', '502', '503', '504'],
                severity: 'high',
                recoverable: true
            });
            
            this.errorTypes.set('ui', {
                patterns: ['element', 'not found', 'clickable', 'visible'],
                severity: 'medium',
                recoverable: true
            });
            
            this.core.logger.info('✅ Error classification setup completed');
            return { success: true };
        } catch (error) {
            this.core.logger.error(`Error classification setup failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Classify error
     */
    async classifyError(error, context = {}) {
        try {
            const errorMessage = error.message || error.toString();
            const errorLower = errorMessage.toLowerCase();
            
            // Find matching error type
            for (const [type, config] of this.errorTypes) {
                if (config.patterns.some(pattern => errorLower.includes(pattern))) {
                    return {
                        type: type,
                        severity: config.severity,
                        recoverable: config.recoverable,
                        message: errorMessage,
                        context: context,
                        timestamp: new Date().toISOString()
                    };
                }
            }
            
            // Default classification
            return {
                type: 'unknown',
                severity: 'medium',
                recoverable: true,
                message: errorMessage,
                context: context,
                timestamp: new Date().toISOString()
            };
        } catch (classificationError) {
            this.core.logger.error(`Error classification failed: ${classificationError.message}`);
            return {
                type: 'classification_error',
                severity: 'high',
                recoverable: false,
                message: error.message,
                context: context,
                timestamp: new Date().toISOString()
            };
        }
    }

    /**
     * Get error type configuration
     */
    getErrorTypeConfig(type) {
        return this.errorTypes.get(type);
    }

    /**
     * Add custom error type
     */
    addErrorType(type, config) {
        this.errorTypes.set(type, config);
    }
}

module.exports = PluctEnhancementErrorClassification;
