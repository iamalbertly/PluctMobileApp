/**
 * Pluct-TechnicalDebt-11Performance-00Orchestrator - Performance technical debt orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctTechnicalDebt11PerformanceAnalysis = require('./Pluct-TechnicalDebt-11Performance-01Analysis');
const PluctTechnicalDebt11PerformanceOptimization = require('./Pluct-TechnicalDebt-11Performance-02Optimization');
const PluctTechnicalDebt11PerformanceMonitoring = require('./Pluct-TechnicalDebt-11Performance-03Monitoring');

class PluctTechnicalDebt11PerformanceOrchestrator {
    constructor(core) {
        this.core = core;
        this.analysis = new PluctTechnicalDebt11PerformanceAnalysis(core);
        this.optimization = new PluctTechnicalDebt11PerformanceOptimization(core);
        this.monitoring = new PluctTechnicalDebt11PerformanceMonitoring(core);
    }

    /**
     * Resolve performance technical debt
     */
    async resolvePerformanceDebt() {
        this.core.logger.info('⚡ Resolving performance technical debt...');
        
        try {
            // Step 1: Analyze performance
            this.core.logger.info('📊 Step 1: Analyzing performance...');
            const analysisResult = await this.analysis.analyzePerformance();
            if (!analysisResult.success) {
                throw new Error(`Performance analysis failed: ${analysisResult.error}`);
            }
            
            // Step 2: Implement optimizations
            this.core.logger.info('🔧 Step 2: Implementing optimizations...');
            const optimizationResult = await this.optimization.implementPerformanceOptimizations();
            if (!optimizationResult.success) {
                throw new Error(`Performance optimization failed: ${optimizationResult.error}`);
            }
            
            // Step 3: Setup monitoring
            this.core.logger.info('📈 Step 3: Setting up monitoring...');
            const monitoringResult = await this.monitoring.setupPerformanceMonitoring();
            if (!monitoringResult.success) {
                throw new Error(`Performance monitoring setup failed: ${monitoringResult.error}`);
            }
            
            // Step 4: Start monitoring
            this.core.logger.info('🚀 Step 4: Starting monitoring...');
            await this.monitoring.startMonitoring();
            
            this.core.logger.info('✅ Performance technical debt resolved');
            return { 
                success: true,
                analysis: this.analysis.getPerformanceMetrics(),
                optimization: this.optimization.getOptimizationSummary(),
                monitoring: this.monitoring.getMetricsSummary()
            };
        } catch (error) {
            this.core.logger.error('❌ Performance debt resolution failed:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Get performance status
     */
    getPerformanceStatus() {
        return {
            analysis: this.analysis.getPerformanceMetrics(),
            optimization: this.optimization.getOptimizationSummary(),
            monitoring: this.monitoring.getMetricsSummary(),
            isMonitoringActive: this.monitoring.monitoringActive
        };
    }

    /**
     * Stop performance monitoring
     */
    stopPerformanceMonitoring() {
        this.monitoring.stopMonitoring();
        this.core.logger.info('✅ Performance monitoring stopped');
    }
}

module.exports = PluctTechnicalDebt11PerformanceOrchestrator;
