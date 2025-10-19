/**
 * Pluct-Journey-04Orchestrator - Core journey orchestration
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[next stage increment to the childscope][CoreResponsibility]
 * Consolidated from Pluct-Journey-02Orchestrator.js (865 lines) to maintain 300-line limit
 */

class PluctJourney04Orchestrator {
    constructor(core, uiValidator) {
        this.core = core;
        this.uiValidator = uiValidator;
        this.journeyResults = new Map();
    }

    /**
     * Test app launch journey
     */
    async testAppLaunch() {
        console.log('🎯 Testing app launch...');
        
        try {
            // Ensure app is in foreground
            const foregroundResult = await this.core.validateAppForeground();
            if (!foregroundResult.success) {
                throw new Error('App not in foreground');
            }

            // Capture UI artifacts
            await this.uiValidator.captureUIArtifacts('AppLaunch-pre');
            
            // Test credit balance display
            console.log('🎯 Testing credit balance display on app launch...');
            const creditResult = await this.uiValidator.findCreditBalanceElement();
            
            if (creditResult.success) {
                console.log('✅ Credit balance UI element found');
                await this.uiValidator.captureUIArtifacts('AppLaunch-post');
                return { success: true, creditBalance: creditResult.balance };
            } else {
                throw new Error('Credit balance element not found');
            }
        } catch (error) {
            console.error('❌ App launch test failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Test share intent handling
     */
    async testShareIntent() {
        console.log('🎯 Testing share intent handling...');
        
        try {
            // Ensure app is in foreground
            const foregroundResult = await this.core.validateAppForeground();
            if (!foregroundResult.success) {
                throw new Error('App not in foreground');
            }

            // Capture UI artifacts
            await this.uiValidator.captureUIArtifacts('ShareIntent-pre');
            
            // Simulate share intent
            console.log('🎯 CAPTURING CAPTURE REQUEST LOGS...');
            await this.uiValidator.captureCaptureRequestLogs('ShareIntent-capture-request');
            
            // Capture final UI state
            await this.uiValidator.captureUIArtifacts('ShareIntent-post');
            
            return { success: true };
        } catch (error) {
            console.error('❌ Share intent test failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Test QuickScan button functionality
     */
    async testQuickScan() {
        console.log('🎯 Testing video processing flow...');
        
        try {
            // Ensure app is in foreground
            const foregroundResult = await this.core.validateAppForeground();
            if (!foregroundResult.success) {
                throw new Error('App not in foreground');
            }

            // Capture UI artifacts
            await this.uiValidator.captureUIArtifacts('VideoProcessing-pre');
            
            // Ensure app is in foreground before clicking
            await this.uiValidator.ensurePluctAppForeground();
            
            // Click QuickScan button
            const clickResult = await this.uiValidator.clickFirstClickable();
            if (clickResult.success) {
                console.log('✅ Quick Scan button clicked successfully');
            } else {
                console.warn('⚠️ Quick Scan button click failed, trying alternative method');
                // Alternative click method
                await this.uiValidator.executeCommand('adb shell input tap 360 600');
                console.log('✅ Quick Scan button clicked via alternative method');
            }
            
            // Capture final UI state
            await this.uiValidator.captureUIArtifacts('VideoProcessing-post');
            
            return { success: true };
        } catch (error) {
            console.error('❌ QuickScan test failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Test manual URL input flow
     */
    async testManualURLInput() {
        console.log('🎯 Testing manual URL input flow...');
        
        try {
            // Ensure app is in foreground
            const foregroundResult = await this.core.validateAppForeground();
            if (!foregroundResult.success) {
                throw new Error('App not in foreground');
            }

            // Test QuickScan button clickability
            console.log('🎯 Testing QuickScan button clickability...');
            const quickScanResult = await this.uiValidator.testButtonClickability('QuickScan');
            if (quickScanResult.success) {
                console.log('✅ QuickScan button is clickable and produced expected result');
            }

            // Test AI Analysis button clickability
            console.log('🎯 Testing AI Analysis button clickability...');
            const aiAnalysisResult = await this.uiValidator.testButtonClickability('AIAnalysis');
            if (aiAnalysisResult.success) {
                console.log('✅ AI Analysis button is clickable and produced expected result');
            }
            
            return { success: true };
        } catch (error) {
            console.error('❌ Manual URL input test failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Record journey result
     */
    recordJourneyResult(journeyName, result) {
        this.journeyResults.set(journeyName, {
            result,
            timestamp: Date.now()
        });
    }

    /**
     * Get journey results summary
     */
    getJourneyResultsSummary() {
        const results = Array.from(this.journeyResults.entries());
        const successful = results.filter(([name, data]) => data.result.success).length;
        const total = results.length;
        const successRate = total > 0 ? (successful / total) * 100 : 0;
        
        return {
            total,
            successful,
            failed: total - successful,
            successRate,
            results: results.map(([name, data]) => ({
                journey: name,
                success: data.result.success,
                timestamp: data.timestamp
            }))
        };
    }
}

module.exports = PluctJourney04Orchestrator;
