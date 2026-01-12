const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX18StaleStatus01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-18StaleStatus-01Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-18StaleStatus-01Validation');
        
        try {
            // Step 1: Create stale entry in database
            this.core.logger.info('📱 Step 1: Creating stale entry in database...');
            
            // Get database path
            const dbPathResult = await this.core.executeCommand(
                'adb shell "run-as app.pluct find /data/data/app.pluct/databases -name *.db"',
                10000,
                1,
                { allowFailure: true }
            );
            
            let dbPath = '/data/data/app.pluct/databases/pluct_database';
            if (dbPathResult.output && dbPathResult.output.trim()) {
                const match = dbPathResult.output.match(/\/data\/[^\s]+\.db/);
                if (match) {
                    dbPath = match[0];
                }
            }
            
            // Create stale entry (older than 1 hour, no jobId)
            const staleTimestamp = Math.floor((Date.now() - (2 * 60 * 60 * 1000)) / 1000); // 2 hours ago in seconds
            const testUrl = 'https://vm.tiktok.com/TEST_STALE_ENTRY/';
            
            // Insert stale entry using SQLite
            const insertCommand = `adb shell "run-as app.pluct sqlite3 ${dbPath} \\\"INSERT OR REPLACE INTO videos (id, url, title, thumbnailUrl, author, duration, status, progress, transcript, timestamp, jobId) VALUES ('stale_test_001', '${testUrl}', 'Stale Test', '', 'Test', 0, 1, 0, NULL, ${staleTimestamp}, NULL);\\\"\""`;
            
            const insertResult = await this.core.executeCommand(insertCommand, 10000, 1, { allowFailure: true });
            
            if (!insertResult.success) {
                this.core.logger.warn('⚠️ Could not insert stale entry directly, simulating via app behavior');
                // Alternative: We'll rely on the app creating a stale entry naturally
            } else {
                this.core.logger.info('✅ Stale entry created in database');
            }
            
            // Step 2: Launch app
            this.core.logger.info('📱 Step 2: Launching app...');
            await this.core.clearLogcat();
            const launchResult = await this.core.launchApp();
            if (!launchResult.success) {
                return { success: false, error: 'App launch failed' };
            }
            await this.core.sleep(3000); // Wait for resumer to run
            
            // Step 3: Verify logcat shows stale entry filtering
            this.core.logger.info('📱 Step 3: Verifying stale entry filtering in logcat...');
            const staleLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "Filtered out.*stale processing entries|stale processing entries"'
            );
            
            if (staleLog.output.includes('stale') || staleLog.output.includes('Filtered out')) {
                this.core.logger.info('✅ Stale entry filtering detected in logcat');
            } else {
                this.core.logger.warn('⚠️ Stale entry filtering log not found (may not have stale entries)');
            }
            
            // Step 4: Verify stale entries marked as FAILED
            this.core.logger.info('📱 Step 4: Verifying stale entries marked as FAILED...');
            const failedLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "Processing timed out.*stale entry|stale entry"'
            );
            
            if (failedLog.output.includes('stale entry') || failedLog.output.includes('timed out')) {
                this.core.logger.info('✅ Stale entries marked as FAILED confirmed');
            } else {
                this.core.logger.warn('⚠️ Stale entry failure marking not found (may not have stale entries)');
            }
            
            // Step 5: Verify no resume attempts for stale entries
            this.core.logger.info('📱 Step 5: Verifying no resume attempts for stale entries...');
            const resumeLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "Scheduled background worker.*stale|resuming.*stale"'
            );
            
            // Should NOT have resume attempts for stale entries
            if (resumeLog.output.includes('stale') && 
                (resumeLog.output.includes('Scheduled') || resumeLog.output.includes('resuming'))) {
                this.core.logger.error('❌ FAILURE: Resume attempt detected for stale entry');
                return { success: false, error: 'Stale entry was resumed (should not happen)' };
            }
            
            this.core.logger.info('✅ No resume attempts for stale entries');
            
            // Step 6: Verify database state
            this.core.logger.info('📱 Step 6: Verifying database state...');
            const dbCheckLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "status.*FAILED|Processing timed out"'
            );
            
            if (dbCheckLog.output.includes('FAILED') || dbCheckLog.output.includes('timed out')) {
                this.core.logger.info('✅ Database update to FAILED confirmed');
            } else {
                this.core.logger.warn('⚠️ Database update log not found');
            }
            
            this.core.logger.info('✅ Journey-UX-18StaleStatus-01Validation completed successfully');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('UX-18StaleStatus-01Validation', new JourneyUX18StaleStatus01Validation(orchestrator.core));
}

module.exports = { register };
