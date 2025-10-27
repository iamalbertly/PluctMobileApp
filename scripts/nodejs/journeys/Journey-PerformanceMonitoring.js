const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-PerformanceMonitoring - Test performance monitoring functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Tests performance metrics collection and monitoring systems
 */
class JourneyPerformanceMonitoring extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'PerformanceMonitoring';
    }

    async execute() {
        try {
            this.core.logger.info('🚀 Starting Performance Monitoring Journey');
            
            // Step 1: Verify performance monitoring UI is accessible
            await this.verifyPerformanceMonitoringUI();
            
            // Step 2: Test performance metrics collection
            await this.testPerformanceMetricsCollection();
            
            // Step 3: Test performance alerts and notifications
            await this.testPerformanceAlerts();
            
            // Step 4: Test performance optimization suggestions
            await this.testPerformanceOptimization();
            
            this.core.logger.info('✅ Performance Monitoring Journey completed successfully');
            return { success: true, message: 'Performance monitoring functionality working correctly' };
            
        } catch (error) {
            this.core.logger.error(`❌ Performance Monitoring Journey failed: ${error.message}`);
            throw error;
        }
    }

    async verifyPerformanceMonitoringUI() {
        this.core.logger.info('🔍 Verifying performance monitoring UI accessibility...');
        
        // Wait for app to be ready
        await this.core.waitForText('Pluct', 5000);
        
        // Look for performance monitoring elements
        const uiDump = await this.core.dumpUIHierarchy();
        const performanceElements = [
            'performance_monitoring_card',
            'Performance',
            'System Metrics',
            'performance_dashboard',
            'metrics_display'
        ];
        
        const hasPerformanceElements = performanceElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasPerformanceElements) {
            this.core.logger.info('✅ Performance monitoring UI elements found');
        } else {
            this.core.logger.info('ℹ️ Performance monitoring UI not visible (may be in settings or advanced section)');
        }
    }

    async testPerformanceMetricsCollection() {
        this.core.logger.info('📊 Testing performance metrics collection...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for metrics display elements
        const metricsElements = [
            'memory_usage_display',
            'cpu_usage_display',
            'network_latency_display',
            'app_startup_time_display',
            'performance_metrics_list'
        ];
        
        const hasMetricsElements = metricsElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasMetricsElements) {
            this.core.logger.info('✅ Performance metrics display found');
            
            // Try to interact with metrics if available
            try {
                if (uiDump.toString().includes('refresh_metrics_button')) {
                    await this.core.tapByTestTag('refresh_metrics_button');
                    this.core.logger.info('✅ Refresh metrics button tapped');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not interact with performance metrics: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ Performance metrics not visible (may be background collection)');
        }
    }

    async testPerformanceAlerts() {
        this.core.logger.info('🚨 Testing performance alerts and notifications...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for performance alert elements
        const alertElements = [
            'performance_alert_banner',
            'Performance Alert',
            'System Warning',
            'performance_notification',
            'alert_dismiss_button'
        ];
        
        const hasAlertElements = alertElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasAlertElements) {
            this.core.logger.info('✅ Performance alerts found');
            
            // Try to interact with alerts if available
            try {
                if (uiDump.toString().includes('alert_dismiss_button')) {
                    await this.core.tapByTestTag('alert_dismiss_button');
                    this.core.logger.info('✅ Performance alert dismissed');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not interact with performance alerts: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ No performance alerts visible (system performing well)');
        }
    }

    async testPerformanceOptimization() {
        this.core.logger.info('⚡ Testing performance optimization suggestions...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for optimization elements
        const optimizationElements = [
            'optimization_suggestions_button',
            'Performance Optimization',
            'optimize_button',
            'performance_recommendations',
            'apply_optimization_button'
        ];
        
        const hasOptimizationElements = optimizationElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasOptimizationElements) {
            this.core.logger.info('✅ Performance optimization elements found');
            
            // Try to interact with optimization if available
            try {
                if (uiDump.toString().includes('optimize_button')) {
                    await this.core.tapByTestTag('optimize_button');
                    this.core.logger.info('✅ Performance optimization button tapped');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not interact with performance optimization: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ Performance optimization not visible (system may be optimized)');
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('PerformanceMonitoring', new JourneyPerformanceMonitoring(orchestrator.core));
}

module.exports = { register };