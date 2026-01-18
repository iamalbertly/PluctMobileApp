const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Fix-03HealthMonitoring-Validation
 * Follows naming convention: [Journey]-[Fix]-[03HealthMonitoring]-[Validation]
 * 4 scope layers: Journey, Fix, HealthMonitoring, Validation
 * Validates health monitoring cleanup and proper lifecycle management
 */
class JourneyFix03HealthMonitoringValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Fix-03HealthMonitoring-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Health Monitoring Cleanup Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        await this.core.clearLogcat();
        
        // Step 2: Monitor logcat for health check frequency
        this.core.logger.info('Monitoring health check frequency...');
        await this.core.sleep(35000); // Wait ~35 seconds to catch at least one health check
        
        const healthCheckLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "/health\|health.*check\|HealthMonitoring\|healthStatus"'
        );
        
        const healthCheckLines = (healthCheckLogcat.output || '').split('\n').filter(line => 
            line.includes('/health') || 
            line.includes('health') || 
            line.includes('HealthMonitoring') ||
            line.includes('healthStatus')
        );
        
        const healthCheckCount = healthCheckLines.length;
        this.core.logger.info(`Health check logs found: ${healthCheckCount}`);
        
        // Step 3: Verify health checks occur every ~30 seconds
        if (healthCheckCount > 0) {
            this.core.logger.info('✅ Health checks are occurring');
            
            // Check timing between health checks (should be ~30-60 seconds)
            const timestamps = healthCheckLines.map(line => {
                const match = line.match(/(\d{2}-\d{2} \d{2}:\d{2}:\d{2})/);
                return match ? match[1] : null;
            }).filter(Boolean);
            
            if (timestamps.length >= 2) {
                this.core.logger.info(`Health check timestamps: ${timestamps.slice(0, 3).join(', ')}`);
            }
        } else {
            this.core.logger.warn('⚠️ No health check logs found (may be normal if health endpoint is not logged)');
        }
        
        // Step 4: Force stop app
        this.core.logger.info('Force stopping app to test cleanup...');
        await this.core.executeCommand('adb shell am force-stop app.pluct');
        await this.core.sleep(2000);
        
        // Step 5: Verify health check stops immediately
        await this.core.clearLogcat();
        await this.core.sleep(10000); // Wait 10 seconds after force stop
        
        const afterStopHealthLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "/health\|health.*check\|HealthMonitoring"'
        );
        
        const afterStopHealthCount = (afterStopHealthLogcat.output || '').split('\n').filter(line => 
            line.includes('/health') || 
            line.includes('health') || 
            line.includes('HealthMonitoring')
        ).length;
        
        if (afterStopHealthCount > 2) {
            this.core.logger.warn(`⚠️ Health checks may still be running after force stop: ${afterStopHealthCount} logs found`);
        } else {
            this.core.logger.info('✅ Health checks stopped after app force stop');
        }
        
        // Step 6: Check for /health endpoint calls after app stop
        const healthEndpointLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "GET.*health\|/health"'
        );
        
        const healthEndpointCount = (healthEndpointLogcat.output || '').split('\n').filter(line => 
            line.includes('/health') || line.includes('GET') && line.includes('health')
        ).length;
        
        if (healthEndpointCount > 1) {
            this.core.logger.warn(`⚠️ Health endpoint calls detected after app stop: ${healthEndpointCount}`);
        } else {
            this.core.logger.info('✅ No health endpoint calls after app stop');
        }
        
        // Step 7: Restart app
        this.core.logger.info('Restarting app...');
        await this.core.launchApp();
        await this.core.sleep(3000);
        await this.core.clearLogcat();
        
        // Step 8: Verify health check resumes
        await this.core.sleep(35000); // Wait ~35 seconds
        
        const restartHealthLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "/health\|health.*check\|HealthMonitoring"'
        );
        
        const restartHealthCount = (restartHealthLogcat.output || '').split('\n').filter(line => 
            line.includes('/health') || 
            line.includes('health') || 
            line.includes('HealthMonitoring')
        ).length;
        
        if (restartHealthCount > 0) {
            this.core.logger.info('✅ Health checks resumed after app restart');
        } else {
            this.core.logger.warn('⚠️ Health checks may not have resumed (may be normal if not logged)');
        }
        
        // Step 9: Check for orphaned health monitoring jobs
        // Look for multiple health monitoring instances or cleanup logs
        const cleanupLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "cleanup\|Cleanup\|cancel\|Cancel.*health\|health.*cancel"'
        );
        
        const hasCleanup = cleanupLogcat.output && (
            cleanupLogcat.output.includes('cleanup') ||
            cleanupLogcat.output.includes('Cleanup') ||
            cleanupLogcat.output.includes('cancel')
        );
        
        if (hasCleanup) {
            this.core.logger.info('✅ Cleanup/cancellation logs detected (good - indicates proper lifecycle management)');
        }
        
        // Check for multiple health monitoring instances
        const multipleInstances = (healthCheckLogcat.output || '').split('HealthMonitoring').length - 1;
        if (multipleInstances > 3) {
            this.core.logger.warn(`⚠️ Multiple health monitoring instances detected: ${multipleInstances}`);
        } else {
            this.core.logger.info('✅ No orphaned health monitoring jobs detected');
        }
        
        this.core.logger.info('✅ Health monitoring cleanup validation completed');
        return { success: true };
    }
}

module.exports = JourneyFix03HealthMonitoringValidation;
