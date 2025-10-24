/**
 * Pluct-TechnicalDebt-10Architecture-00Orchestrator - Architecture technical debt orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctTechnicalDebt10ArchitectureAnalysis = require('./Pluct-TechnicalDebt-10Architecture-01Analysis');
const PluctTechnicalDebt10ArchitectureImplementation = require('./Pluct-TechnicalDebt-10Architecture-02Implementation');

class PluctTechnicalDebt10ArchitectureOrchestrator {
    constructor(core) {
        this.core = core;
        this.analysis = new PluctTechnicalDebt10ArchitectureAnalysis(core);
        this.implementation = new PluctTechnicalDebt10ArchitectureImplementation(core);
    }

    /**
     * Resolve architecture technical debt
     */
    async resolveArchitectureDebt() {
        this.core.logger.info('🏗️ Resolving architecture technical debt...');
        
        try {
            // Step 1: Analyze architecture
            this.core.logger.info('📊 Step 1: Analyzing architecture...');
            const analysisResult = await this.analysis.analyzeArchitecture();
            if (!analysisResult.success) {
                throw new Error(`Architecture analysis failed: ${analysisResult.error}`);
            }
            
            // Step 2: Implement design patterns
            this.core.logger.info('🎨 Step 2: Implementing design patterns...');
            const implementationResult = await this.implementation.implementDesignPatterns();
            if (!implementationResult.success) {
                throw new Error(`Design pattern implementation failed: ${implementationResult.error}`);
            }
            
            this.core.logger.info('✅ Architecture technical debt resolved');
            return { 
                success: true,
                analysis: this.analysis.getArchitectureAnalysis(),
                implementation: this.implementation.getImplementationSummary()
            };
        } catch (error) {
            this.core.logger.error('❌ Architecture debt resolution failed:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Get architecture status
     */
    getArchitectureStatus() {
        return {
            analysis: this.analysis.getArchitectureAnalysis(),
            implementation: this.implementation.getImplementationSummary()
        };
    }
}

module.exports = PluctTechnicalDebt10ArchitectureOrchestrator;
