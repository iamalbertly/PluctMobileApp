const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');
const PluctTestValidationSystemHealth = require('./Pluct-Test-Validation-02SystemHealth');
const PluctTestValidationAppUI = require('./Pluct-Test-Validation-03AppUI');
const PluctTestValidationAPIConnectivity = require('./Pluct-Test-Validation-04APIConnectivity');
const PluctTestValidationCreditBalance = require('./Pluct-Test-Validation-05CreditBalance');
const PluctTestValidationTokenVending = require('./Pluct-Test-Validation-06TokenVending');
const PluctTestValidationVideoProcessing = require('./Pluct-Test-Validation-07VideoProcessing');
const PluctTestValidationTranscription = require('./Pluct-Test-Validation-08Transcription');
const PluctTestValidationUIState = require('./Pluct-Test-Validation-09UIState');
const PluctTestValidationErrorHandling = require('./Pluct-Test-Validation-10ErrorHandling');
const PluctTestValidationPerformance = require('./Pluct-Test-Validation-11Performance');

/**
 * Pluct-Test-Validation-01Orchestrator - Main orchestrator for comprehensive validation
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Orchestrates all validation modules for complete end-to-end testing
 */
class PluctTestValidationOrchestrator extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-01Orchestrator';
        this.maxDuration = 300000; // 5 minutes max
        
        // Initialize validation modules
        this.validators = {
            systemHealth: new PluctTestValidationSystemHealth(core),
            appUI: new PluctTestValidationAppUI(core),
            apiConnectivity: new PluctTestValidationAPIConnectivity(core),
            creditBalance: new PluctTestValidationCreditBalance(core),
            tokenVending: new PluctTestValidationTokenVending(core),
            videoProcessing: new PluctTestValidationVideoProcessing(core),
            transcription: new PluctTestValidationTranscription(core),
            uiState: new PluctTestValidationUIState(core),
            errorHandling: new PluctTestValidationErrorHandling(core),
            performance: new PluctTestValidationPerformance(core)
        };
    }

    async execute() {
        this.core.logger.info('🎯 Starting Comprehensive End-to-End Test Validation...');
        const startTime = Date.now();
        
        try {
            const validationSteps = [
                { name: 'System Health', validator: 'systemHealth' },
                { name: 'App UI', validator: 'appUI' },
                { name: 'API Connectivity', validator: 'apiConnectivity' },
                { name: 'Credit Balance', validator: 'creditBalance' },
                { name: 'Token Vending', validator: 'tokenVending' },
                { name: 'Video Processing', validator: 'videoProcessing' },
                { name: 'Transcription', validator: 'transcription' },
                { name: 'UI State Management', validator: 'uiState' },
                { name: 'Error Handling', validator: 'errorHandling' },
                { name: 'Performance', validator: 'performance' }
            ];

            const results = {};
            
            for (const step of validationSteps) {
                this.core.logger.info(`📱 Step: ${step.name} Validation`);
                const result = await this.validators[step.validator].execute();
                
                if (!result.success) {
                    return { 
                        success: false, 
                        error: `${step.name} validation failed: ${result.error}`,
                        failedStep: step.name,
                        results: results
                    };
                }
                
                results[step.validator] = result;
            }
            
            const totalTime = Date.now() - startTime;
            this.core.logger.info(`✅ Comprehensive End-to-End Test Validation completed successfully in ${totalTime}ms`);
            
            return { 
                success: true, 
                duration: totalTime,
                results: results
            };
            
        } catch (error) {
            const totalTime = Date.now() - startTime;
            this.core.logger.error(`❌ Comprehensive validation failed after ${totalTime}ms: ${error.message}`);
            return { 
                success: false, 
                error: error.message,
                duration: totalTime
            };
        }
    }
}

module.exports = PluctTestValidationOrchestrator;
