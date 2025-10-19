/**
 * Pluct-Core-06Monitoring - Advanced monitoring and analytics
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[next stage increment to the childscope][CoreResponsibility]
 * Consolidated from Pluct-Core-02Foundation.js to maintain 300-line limit
 */

class PluctCore06Monitoring {
    constructor() {
        this.monitoringData = new Map();
        this.startTime = Date.now();
    }

    /**
     * Monitor Business Engine health with detailed metrics
     */
    async monitorBusinessEngineHealth() {
        console.log('🔍 Monitoring Business Engine health with detailed metrics...');
        
        try {
            const result = await this.executeCommand('curl -s https://business-engine.pluct.app/health');
            if (result.success) {
                const healthData = JSON.parse(result.output);
                
                console.log(`✅ D1: ${healthData.connectivity?.d1 || 'unknown'}`);
                console.log(`✅ KV: ${healthData.connectivity?.kv || 'unknown'}`);
                console.log(`✅ TTT: ${healthData.connectivity?.ttt || 'unknown'}`);
                console.log(`✅ Business Engine uptime: ${Math.floor(healthData.uptimeSeconds / 3600)} hours`);
                console.log(`✅ Business Engine version: ${healthData.version}`);
                
                return {
                    success: true,
                    health: healthData,
                    uptime: healthData.uptimeSeconds,
                    version: healthData.version,
                    connectivity: healthData.connectivity
                };
            }
            
            return { success: false, reason: 'Health check failed' };
        } catch (error) {
            console.error('❌ Business Engine health monitoring failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Validate token vending system with detailed transaction tracking
     */
    async validateTokenVendingSystem() {
        console.log('🎫 Validating token vending system with detailed transaction tracking...');
        
        try {
            const startTime = Date.now();
            const result = await this.executeCommand('curl -s -w "%{http_code}" -o /dev/null https://business-engine.pluct.app/v1/vend-token');
            const endTime = Date.now();
            const responseTime = (endTime - startTime) / 1000;
            
            const statusCode = result.output.trim();
            console.log(`✅ Token vending endpoint response code: ${statusCode}`);
            console.log(`✅ Token vending endpoint response time: ${responseTime}s`);
            
            if (statusCode === '400' || statusCode === '401') {
                console.warn('⚠️ Token vending system validation inconclusive');
                return { success: false, reason: 'Token vending system validation failed' };
            }
            
            return {
                success: true,
                statusCode,
                responseTime,
                endpoint: 'https://business-engine.pluct.app/v1/vend-token'
            };
        } catch (error) {
            console.error('❌ Token vending system validation failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Monitor TTTranscribe connectivity
     */
    async monitorTTTranscribeConnectivity() {
        console.log('🔍 Monitoring TTTranscribe connectivity...');
        
        try {
            const result = await this.executeCommand('curl -s -w "%{http_code}" -o /dev/null https://ttt.pluct.app/health');
            const statusCode = result.output.trim();
            
            if (statusCode === '200') {
                console.log('✅ TTTranscribe API is reachable');
                return { success: true, statusCode, endpoint: 'https://ttt.pluct.app/health' };
            } else {
                console.warn(`⚠️ TTTranscribe API returned status: ${statusCode}`);
                return { success: false, statusCode, reason: 'TTTranscribe API not reachable' };
            }
        } catch (error) {
            console.error('❌ TTTranscribe connectivity monitoring failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Validate end-to-end transaction flow
     */
    async validateEndToEndTransactionFlow() {
        console.log('🔄 Validating end-to-end transaction flow...');
        
        try {
            // Step 1: Validate Business Engine health
            const healthResult = await this.monitorBusinessEngineHealth();
            if (!healthResult.success) {
                return { success: false, reason: 'Business Engine health check failed' };
            }
            
            // Step 2: Validate token vending
            const tokenResult = await this.validateTokenVendingSystem();
            if (!tokenResult.success) {
                return { success: false, reason: 'Token vending validation failed' };
            }
            
            // Step 3: Validate TTTranscribe connectivity
            const tttResult = await this.monitorTTTranscribeConnectivity();
            if (!tttResult.success) {
                return { success: false, reason: 'TTTranscribe connectivity failed' };
            }
            
            console.log('✅ End-to-end transaction flow validation successful');
            return {
                success: true,
                businessEngine: healthResult,
                tokenVending: tokenResult,
                ttTranscribe: tttResult
            };
        } catch (error) {
            console.error('❌ End-to-end transaction flow validation failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Record monitoring data
     */
    recordMonitoringData(key, data) {
        this.monitoringData.set(key, {
            data,
            timestamp: Date.now()
        });
    }

    /**
     * Get monitoring summary
     */
    getMonitoringSummary() {
        const currentTime = Date.now();
        const totalTime = currentTime - this.startTime;
        
        return {
            totalTime,
            dataPoints: this.monitoringData.size,
            monitoringData: Array.from(this.monitoringData.entries()),
            timestamp: new Date().toISOString()
        };
    }

    /**
     * Execute command helper
     */
    async executeCommand(command, timeout = 15000) {
        const { exec } = require('child_process');
        return new Promise((resolve, reject) => {
            const child = exec(command, { timeout }, (error, stdout, stderr) => {
                if (error) {
                    reject({ success: false, error: error.message, stderr });
                } else {
                    resolve({ success: true, output: stdout, stderr });
                }
            });
        });
    }

    /**
     * Clear monitoring data
     */
    clearMonitoringData() {
        this.monitoringData.clear();
        this.startTime = Date.now();
        console.log('🧹 Monitoring data cleared');
    }
}

module.exports = PluctCore06Monitoring;
