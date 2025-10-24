/**
 * Pluct-TechnicalDebt-11Performance-03Monitoring - Performance monitoring component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctTechnicalDebt11PerformanceMonitoring {
    constructor(core) {
        this.core = core;
        this.monitoringActive = false;
        this.metrics = new Map();
    }

    /**
     * Setup performance monitoring
     */
    async setupPerformanceMonitoring() {
        this.core.logger.info('📊 Setting up performance monitoring...');
        
        try {
            // Setup real-time monitoring
            await this.setupRealTimeMonitoring();
            
            // Setup metrics collection
            await this.setupMetricsCollection();
            
            // Setup alerting
            await this.setupAlerting();
            
            // Setup reporting
            await this.setupReporting();
            
            this.monitoringActive = true;
            this.core.logger.info('✅ Performance monitoring setup completed');
            return { success: true };
        } catch (error) {
            this.core.logger.error('❌ Performance monitoring setup failed:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Setup real-time monitoring
     */
    async setupRealTimeMonitoring() {
        this.core.logger.info('📈 Setting up real-time monitoring...');
        
        const monitoringConfig = {
            cpuThreshold: 80,
            memoryThreshold: 100000, // 100MB
            networkLatencyThreshold: 5000, // 5 seconds
            uiRenderThreshold: 16 // 60 FPS
        };
        
        this.monitoringConfig = monitoringConfig;
        this.core.logger.info('✅ Real-time monitoring configured');
    }

    /**
     * Setup metrics collection
     */
    async setupMetricsCollection() {
        this.core.logger.info('📊 Setting up metrics collection...');
        
        const metricsToCollect = [
            'app_startup_time',
            'ui_render_time',
            'api_response_time',
            'memory_usage',
            'cpu_usage',
            'network_latency',
            'battery_usage'
        ];
        
        for (const metric of metricsToCollect) {
            this.metrics.set(metric, []);
        }
        
        this.core.logger.info('✅ Metrics collection configured');
    }

    /**
     * Setup alerting
     */
    async setupAlerting() {
        this.core.logger.info('🚨 Setting up alerting...');
        
        const alertingRules = [
            {
                metric: 'cpu_usage',
                threshold: 80,
                severity: 'warning'
            },
            {
                metric: 'memory_usage',
                threshold: 100000,
                severity: 'critical'
            },
            {
                metric: 'api_response_time',
                threshold: 5000,
                severity: 'warning'
            }
        ];
        
        this.alertingRules = alertingRules;
        this.core.logger.info('✅ Alerting configured');
    }

    /**
     * Setup reporting
     */
    async setupReporting() {
        this.core.logger.info('📋 Setting up reporting...');
        
        const reportingConfig = {
            reportInterval: 60000, // 1 minute
            metricsRetention: 24 * 60 * 60 * 1000, // 24 hours
            reportFormats: ['json', 'csv'],
            exportPath: './performance-reports'
        };
        
        this.reportingConfig = reportingConfig;
        this.core.logger.info('✅ Reporting configured');
    }

    /**
     * Start monitoring
     */
    async startMonitoring() {
        if (this.monitoringActive) {
            this.core.logger.info('📊 Starting performance monitoring...');
            
            // Start metrics collection
            this.startMetricsCollection();
            
            // Start alerting
            this.startAlerting();
            
            // Start reporting
            this.startReporting();
            
            this.core.logger.info('✅ Performance monitoring started');
        }
    }

    /**
     * Start metrics collection
     */
    startMetricsCollection() {
        this.metricsInterval = setInterval(async () => {
            await this.collectMetrics();
        }, 5000); // Collect every 5 seconds
    }

    /**
     * Start alerting
     */
    startAlerting() {
        this.alertingInterval = setInterval(async () => {
            await this.checkAlerts();
        }, 10000); // Check every 10 seconds
    }

    /**
     * Start reporting
     */
    startReporting() {
        this.reportingInterval = setInterval(async () => {
            await this.generateReport();
        }, this.reportingConfig.reportInterval);
    }

    /**
     * Collect metrics
     */
    async collectMetrics() {
        try {
            // Collect CPU usage
            const cpuUsage = await this.getCPUUsage();
            this.addMetric('cpu_usage', cpuUsage);
            
            // Collect memory usage
            const memoryUsage = await this.getMemoryUsage();
            this.addMetric('memory_usage', memoryUsage);
            
            // Collect network latency
            const networkLatency = await this.getNetworkLatency();
            this.addMetric('network_latency', networkLatency);
            
        } catch (error) {
            this.core.logger.warn('⚠️ Error collecting metrics:', error.message);
        }
    }

    /**
     * Check alerts
     */
    async checkAlerts() {
        for (const rule of this.alertingRules) {
            const currentValue = this.getLatestMetric(rule.metric);
            if (currentValue && currentValue > rule.threshold) {
                this.core.logger.warn(`🚨 Alert: ${rule.metric} is ${currentValue} (threshold: ${rule.threshold})`);
            }
        }
    }

    /**
     * Generate report
     */
    async generateReport() {
        const report = {
            timestamp: Date.now(),
            metrics: Object.fromEntries(this.metrics),
            summary: this.getMetricsSummary()
        };
        
        this.core.logger.info('📋 Performance report generated');
        return report;
    }

    /**
     * Get CPU usage
     */
    async getCPUUsage() {
        try {
            const result = await this.core.executeCommand('adb shell top -n 1 | grep app.pluct');
            if (result.success) {
                return this.parseCPUUsage(result.output);
            }
        } catch (error) {
            this.core.logger.warn('⚠️ Could not get CPU usage:', error.message);
        }
        return 0;
    }

    /**
     * Get memory usage
     */
    async getMemoryUsage() {
        try {
            const result = await this.core.executeCommand('adb shell dumpsys meminfo app.pluct');
            if (result.success) {
                return this.parseMemoryUsage(result.output);
            }
        } catch (error) {
            this.core.logger.warn('⚠️ Could not get memory usage:', error.message);
        }
        return 0;
    }

    /**
     * Get network latency
     */
    async getNetworkLatency() {
        try {
            const startTime = Date.now();
            await this.core.executeCommand('adb shell ping -c 1 8.8.8.8');
            return Date.now() - startTime;
        } catch (error) {
            this.core.logger.warn('⚠️ Could not get network latency:', error.message);
            return 0;
        }
    }

    /**
     * Add metric
     */
    addMetric(metricName, value) {
        if (this.metrics.has(metricName)) {
            const metricArray = this.metrics.get(metricName);
            metricArray.push({
                value: value,
                timestamp: Date.now()
            });
            
            // Keep only last 100 values
            if (metricArray.length > 100) {
                metricArray.shift();
            }
        }
    }

    /**
     * Get latest metric
     */
    getLatestMetric(metricName) {
        if (this.metrics.has(metricName)) {
            const metricArray = this.metrics.get(metricName);
            if (metricArray.length > 0) {
                return metricArray[metricArray.length - 1].value;
            }
        }
        return null;
    }

    /**
     * Get metrics summary
     */
    getMetricsSummary() {
        const summary = {};
        for (const [metricName, values] of this.metrics) {
            if (values.length > 0) {
                const latest = values[values.length - 1];
                summary[metricName] = {
                    latest: latest.value,
                    timestamp: latest.timestamp,
                    count: values.length
                };
            }
        }
        return summary;
    }

    /**
     * Stop monitoring
     */
    stopMonitoring() {
        if (this.metricsInterval) {
            clearInterval(this.metricsInterval);
        }
        if (this.alertingInterval) {
            clearInterval(this.alertingInterval);
        }
        if (this.reportingInterval) {
            clearInterval(this.reportingInterval);
        }
        this.monitoringActive = false;
        this.core.logger.info('✅ Performance monitoring stopped');
    }
}

module.exports = PluctTechnicalDebt11PerformanceMonitoring;
