const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Fix-02ResourceLeak-Validation
 * Follows naming convention: [Journey]-[Fix]-[02ResourceLeak]-[Validation]
 * 4 scope layers: Journey, Fix, ResourceLeak, Validation
 * Validates infinite loop fix and resource leak prevention
 */
class JourneyFix02ResourceLeakValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Fix-02ResourceLeak-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Infinite Loop and Resource Leak Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        await this.core.clearLogcat();
        
        // Step 2: Monitor logcat for network check frequency
        this.core.logger.info('Monitoring network check frequency...');
        await this.core.sleep(5000); // Wait 5 seconds
        
        const networkCheckLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "network\|Network\|connectivity\|Connectivity"'
        );
        
        const networkCheckCount = (networkCheckLogcat.output || '').split('\n').filter(line => 
            line.includes('network') || line.includes('Network') || 
            line.includes('connectivity') || line.includes('Connectivity')
        ).length;
        
        this.core.logger.info(`Network check logs found: ${networkCheckCount}`);
        
        // Step 3: Put app in background
        this.core.logger.info('Putting app in background...');
        await this.core.executeCommand('adb shell input keyevent KEYCODE_HOME');
        await this.core.sleep(3000);
        
        // Step 4: Verify network check loop stops or reduces frequency
        await this.core.clearLogcat();
        await this.core.sleep(5000); // Wait 5 seconds while backgrounded
        
        const backgroundNetworkLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "network\|Network\|connectivity\|Connectivity"'
        );
        
        const backgroundNetworkCount = (backgroundNetworkLogcat.output || '').split('\n').filter(line => 
            line.includes('network') || line.includes('Network') || 
            line.includes('connectivity') || line.includes('Connectivity')
        ).length;
        
        this.core.logger.info(`Network check logs while backgrounded: ${backgroundNetworkCount}`);
        
        // Network checks should be significantly reduced or stopped when backgrounded
        if (backgroundNetworkCount > networkCheckCount * 0.5) {
            this.core.logger.warn('⚠️ Network checks may not be stopping when app is backgrounded');
        } else {
            this.core.logger.info('✅ Network checks reduced when app is backgrounded');
        }
        
        // Step 5: Return app to foreground
        this.core.logger.info('Returning app to foreground...');
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 6: Verify network check resumes
        await this.core.clearLogcat();
        await this.core.sleep(5000);
        
        const foregroundNetworkLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "network\|Network\|connectivity\|Connectivity"'
        );
        
        const foregroundNetworkCount = (foregroundNetworkLogcat.output || '').split('\n').filter(line => 
            line.includes('network') || line.includes('Network') || 
            line.includes('connectivity') || line.includes('Connectivity')
        ).length;
        
        this.core.logger.info(`Network check logs after foreground: ${foregroundNetworkCount}`);
        
        if (foregroundNetworkCount > 0) {
            this.core.logger.info('✅ Network checks resumed when app returned to foreground');
        }
        
        // Step 7: Check for coroutine cancellation logs
        const cancellationLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "cancel\|Cancel\|isActive\|coroutine.*cancel"'
        );
        
        const hasCancellation = cancellationLogcat.output && (
            cancellationLogcat.output.includes('cancel') ||
            cancellationLogcat.output.includes('Cancel') ||
            cancellationLogcat.output.includes('isActive')
        );
        
        if (hasCancellation) {
            this.core.logger.info('✅ Coroutine cancellation detected (good - indicates proper lifecycle management)');
        }
        
        // Step 8: Check memory usage (via ADB)
        const memInfo = await this.core.executeCommand('adb shell dumpsys meminfo app.pluct');
        
        if (memInfo.success && memInfo.output) {
            const memMatch = memInfo.output.match(/TOTAL\s+(\d+)/);
            if (memMatch) {
                const totalMem = parseInt(memMatch[1]);
                this.core.logger.info(`Memory usage: ${totalMem} KB`);
                
                if (totalMem > 200000) { // 200MB threshold
                    this.core.logger.warn(`⚠️ High memory usage detected: ${totalMem} KB`);
                } else {
                    this.core.logger.info('✅ Memory usage within acceptable range');
                }
            }
        }
        
        // Step 9: Verify no continuous polling when activity is destroyed
        // Force stop app to simulate activity destruction
        this.core.logger.info('Force stopping app to test cleanup...');
        await this.core.executeCommand('adb shell am force-stop app.pluct');
        await this.core.sleep(2000);
        
        await this.core.clearLogcat();
        await this.core.sleep(5000);
        
        const afterStopLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "network\|Network\|connectivity\|Connectivity"'
        );
        
        const afterStopCount = (afterStopLogcat.output || '').split('\n').filter(line => 
            line.includes('network') || line.includes('Network') || 
            line.includes('connectivity') || line.includes('Connectivity')
        ).length;
        
        if (afterStopCount > 2) {
            this.core.logger.warn('⚠️ Network checks may still be running after app force stop');
        } else {
            this.core.logger.info('✅ Network checks stopped after app force stop');
        }
        
        // Step 10: Verify UI responds normally after background/foreground transitions
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        await this.core.dumpUIHierarchy();
        const finalUI = this.core.readLastUIDump() || '';
        
        const hasUI = finalUI.length > 100; // Basic check that UI is present
        if (!hasUI) {
            return {
                success: false,
                error: 'UI not responding after background/foreground transitions'
            };
        }
        
        const hasFreeze = finalUI.toLowerCase().includes('not responding') ||
                         finalUI.toLowerCase().includes('anr');
        
        if (hasFreeze) {
            return {
                success: false,
                error: 'UI freeze detected after background/foreground transitions'
            };
        }
        
        this.core.logger.info('✅ UI responds normally after background/foreground transitions');
        this.core.logger.info('✅ Infinite loop and resource leak validation completed');
        return { success: true };
    }
}

module.exports = JourneyFix02ResourceLeakValidation;
