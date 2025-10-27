/**
 * Pluct-Core-01Foundation-09PerformanceOptimizer - Test execution performance optimizer
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctLogger = require('./Logger');

class PluctCoreFoundationPerformanceOptimizer {
    constructor() {
        this.logger = PluctLogger;
        this.performanceMetrics = {
            testStartTime: null,
            testEndTime: null,
            operations: [],
            bottlenecks: []
        };
        this.optimizationStrategies = new Map();
    }

    /**
     * Start test performance tracking
     */
    startTestPerformanceTracking() {
        this.performanceMetrics.testStartTime = Date.now();
        this.performanceMetrics.operations = [];
        this.performanceMetrics.bottlenecks = [];
        this.logger.info('📊 Test performance tracking started');
    }

    /**
     * End test performance tracking
     */
    endTestPerformanceTracking() {
        this.performanceMetrics.testEndTime = Date.now();
        const totalDuration = this.performanceMetrics.testEndTime - this.performanceMetrics.testStartTime;
        
        this.logger.info(`📊 Test performance tracking ended (${totalDuration}ms)`);
        
        // Analyze performance
        const analysis = this.analyzePerformance();
        this.logger.info('📊 Performance analysis:', analysis);
        
        return {
            totalDuration,
            analysis,
            recommendations: this.generateRecommendations(analysis)
        };
    }

    /**
     * Track operation performance
     */
    trackOperation(operationName, startTime, endTime, metadata = {}) {
        const duration = endTime - startTime;
        const operation = {
            name: operationName,
            duration,
            timestamp: new Date().toISOString(),
            metadata
        };
        
        this.performanceMetrics.operations.push(operation);
        
        // Check for bottlenecks
        if (duration > 5000) { // 5 seconds threshold
            this.performanceMetrics.bottlenecks.push({
                operation: operationName,
                duration,
                severity: duration > 10000 ? 'HIGH' : 'MEDIUM',
                metadata
            });
            this.logger.warn(`⚠️ Performance bottleneck detected: ${operationName} (${duration}ms)`);
        }
        
        this.logger.info(`📊 Operation ${operationName}: ${duration}ms`);
    }

    /**
     * Analyze performance
     */
    analyzePerformance() {
        const operations = this.performanceMetrics.operations;
        const totalDuration = this.performanceMetrics.testEndTime - this.performanceMetrics.testStartTime;
        
        // Calculate statistics
        const durations = operations.map(op => op.duration);
        const avgDuration = durations.reduce((a, b) => a + b, 0) / durations.length;
        const maxDuration = Math.max(...durations);
        const minDuration = Math.min(...durations);
        
        // Find slowest operations
        const slowestOperations = operations
            .sort((a, b) => b.duration - a.duration)
            .slice(0, 5);
        
        // Group by operation type
        const operationGroups = {};
        operations.forEach(op => {
            if (!operationGroups[op.name]) {
                operationGroups[op.name] = [];
            }
            operationGroups[op.name].push(op.duration);
        });
        
        // Calculate group statistics
        const groupStats = {};
        Object.keys(operationGroups).forEach(group => {
            const durations = operationGroups[group];
            groupStats[group] = {
                count: durations.length,
                total: durations.reduce((a, b) => a + b, 0),
                average: durations.reduce((a, b) => a + b, 0) / durations.length,
                max: Math.max(...durations),
                min: Math.min(...durations)
            };
        });
        
        return {
            totalDuration,
            operationCount: operations.length,
            averageDuration: avgDuration,
            maxDuration,
            minDuration,
            slowestOperations,
            groupStats,
            bottlenecks: this.performanceMetrics.bottlenecks
        };
    }

    /**
     * Generate performance recommendations
     */
    generateRecommendations(analysis) {
        const recommendations = [];
        
        // Check for slow operations
        if (analysis.maxDuration > 10000) {
            recommendations.push({
                type: 'CRITICAL',
                message: 'Very slow operations detected (>10s)',
                suggestion: 'Consider breaking down large operations into smaller chunks'
            });
        }
        
        // Check for bottlenecks
        if (analysis.bottlenecks.length > 0) {
            recommendations.push({
                type: 'WARNING',
                message: `${analysis.bottlenecks.length} performance bottlenecks detected`,
                suggestion: 'Review and optimize slow operations'
            });
        }
        
        // Check for repeated slow operations
        const repeatedSlowOps = Object.keys(analysis.groupStats)
            .filter(group => analysis.groupStats[group].average > 2000);
        
        if (repeatedSlowOps.length > 0) {
            recommendations.push({
                type: 'INFO',
                message: `Repeated slow operations: ${repeatedSlowOps.join(', ')}`,
                suggestion: 'Consider caching or optimizing these operations'
            });
        }
        
        // Check total duration
        if (analysis.totalDuration > 300000) { // 5 minutes
            recommendations.push({
                type: 'WARNING',
                message: 'Total test duration is very long',
                suggestion: 'Consider parallelizing operations or reducing test scope'
            });
        }
        
        return recommendations;
    }

    /**
     * Optimize test execution
     */
    async optimizeTestExecution(testOperations) {
        this.logger.info('🚀 Optimizing test execution...');
        
        // Group operations by type
        const operationGroups = this.groupOperationsByType(testOperations);
        
        // Apply optimization strategies
        const optimizedOperations = [];
        
        for (const [type, operations] of Object.entries(operationGroups)) {
            const strategy = this.optimizationStrategies.get(type);
            if (strategy) {
                this.logger.info(`🔧 Applying optimization strategy for: ${type}`);
                const optimized = await strategy.optimize(operations);
                optimizedOperations.push(...optimized);
            } else {
                optimizedOperations.push(...operations);
            }
        }
        
        this.logger.info(`✅ Optimized ${testOperations.length} operations to ${optimizedOperations.length}`);
        return optimizedOperations;
    }

    /**
     * Group operations by type
     */
    groupOperationsByType(operations) {
        const groups = {};
        operations.forEach(op => {
            const type = op.type || 'default';
            if (!groups[type]) {
                groups[type] = [];
            }
            groups[type].push(op);
        });
        return groups;
    }

    /**
     * Register optimization strategy
     */
    registerOptimizationStrategy(type, strategy) {
        this.optimizationStrategies.set(type, strategy);
        this.logger.info(`🔧 Registered optimization strategy for: ${type}`);
    }

    /**
     * Get performance metrics
     */
    getPerformanceMetrics() {
        return {
            ...this.performanceMetrics,
            isTracking: this.performanceMetrics.testStartTime !== null
        };
    }

    /**
     * Reset performance metrics
     */
    resetPerformanceMetrics() {
        this.performanceMetrics = {
            testStartTime: null,
            testEndTime: null,
            operations: [],
            bottlenecks: []
        };
        this.logger.info('🧹 Performance metrics reset');
    }
}

module.exports = PluctCoreFoundationPerformanceOptimizer;
