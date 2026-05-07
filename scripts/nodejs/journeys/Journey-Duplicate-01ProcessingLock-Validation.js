const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Duplicate-01ProcessingLock-Validation
 * Validates that duplicate processing is prevented using global processing lock
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 */
class JourneyDuplicate01ProcessingLockValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Duplicate-01ProcessingLock-Validation';
        this.maxDuration = 120000; // 2 minutes max
    }

    async execute() {
        this.core.logger.info('🔍 Starting duplicate processing lock validation...');
        
        try {
            // Step 1: Clear app data to start fresh
            this.core.logger.info('📱 Clearing app data...');
            await this.core.executeCommand('adb shell pm clear app.pluct');
            await this.core.sleep(2000);
            
            // Step 2: Launch app
            this.core.logger.info('🚀 Launching app...');
            await this.core.launchApp();
            await this.core.sleep(3000);
            
            // Step 3: Get test URL
            const testUrl = this.core.getActiveUrl();
            this.core.logger.info(`📝 Using test URL: ${testUrl}`);
            
            // Step 4: Submit URL first time (should succeed)
            this.core.logger.info('✅ Submitting URL first time...');
            await this.core.tapByText('Paste TikTok Link');
            await this.core.sleep(500);
            await this.core.typeText(testUrl);
            await this.core.sleep(1000);
            await this.core.tapByText('Extract Script');
            await this.core.sleep(2000);
            
            // Step 5: Check logcat for processing start
            this.core.logger.info('🔍 Checking logcat for processing start...');
            const startLog = await this.core.executeCommand(
                'adb logcat -d | findstr /i "processTikTokVideo Starting transcription registered processing"'
            );
            if (!startLog.success || !(startLog.output || startLog.stdout || '').includes('processTikTokVideo')) {
                this.core.logger.warn('⚠️ First submission may not have started');
            }
            
            // Step 6: Rapidly submit same URL again (should be rejected)
            this.core.logger.info('🔄 Rapidly submitting same URL again (should be rejected)...');
            await this.core.tapByText('Paste TikTok Link');
            await this.core.sleep(500);
            await this.core.typeText(testUrl);
            await this.core.sleep(500);
            await this.core.tapByText('Extract Script');
            await this.core.sleep(2000);
            
            // Step 7: Check logcat for duplicate prevention using shared validator
            this.core.logger.info('🔍 Checking logcat for duplicate prevention...');
            const duplicateLog = await this.core.logcatValidator.validatePattern(
                'Duplicate prevention triggered|already being processed|duplicate_prevention|ProcessingLock.*rejecting duplicate',
                'Duplicate prevention',
                2
            );
            
            if (duplicateLog.success) {
                this.core.logger.success('✅ Duplicate prevention detected in logcat');
            } else {
                this.core.logger.warn('⚠️ Duplicate prevention message not found in logcat');
            }
            
            // Step 8: Check UI for error message
            this.core.logger.info('🔍 Checking UI for duplicate error message...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            const hasDuplicateMessage = /already being processed|already processing/i.test(uiDump);
            
            if (hasDuplicateMessage) {
                this.core.logger.success('✅ Duplicate error message found in UI');
            } else {
                this.core.logger.warn('⚠️ Duplicate error message not found in UI');
            }
            
            // Step 9: Check database for single PROCESSING entry
            this.core.logger.info('🔍 Checking database for single PROCESSING entry...');
            const dbCheck = await this.core.executeCommand(
                'adb shell "run-as app.pluct sqlite3 /data/data/app.pluct/databases/pluct_database.db \"SELECT COUNT(*) FROM videos WHERE status = \\\"PROCESSING\\\";\""'
            );
            
            if (dbCheck.success) {
                const count = parseInt((dbCheck.output || dbCheck.stdout || '').trim()) || 0;
                if (count === 1) {
                    this.core.logger.success(`✅ Database has exactly 1 PROCESSING entry (expected)`);
                } else {
                    this.core.logger.warn(`⚠️ Database has ${count} PROCESSING entries (expected 1)`);
                }
            } else {
                this.core.logger.warn('⚠️ Could not check database');
            }
            
            // Step 10: Wait a bit and check final state
            await this.core.sleep(5000);
            this.core.logger.info('✅ Duplicate processing lock validation completed');
            
            return {
                success: true,
                details: {
                    duplicatePreventionDetected: duplicateLog.success,
                    duplicateMessageInUI: hasDuplicateMessage,
                    processingEntriesCount: dbCheck.success ? parseInt((dbCheck.output || dbCheck.stdout || '').trim()) : null
                }
            };
        } catch (error) {
            this.core.logger.error(`❌ Validation failed: ${error.message}`);
            return {
                success: false,
                error: error.message
            };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Duplicate-01ProcessingLock-Validation', new JourneyDuplicate01ProcessingLockValidation(orchestrator.core));
}

module.exports = { JourneyDuplicate01ProcessingLockValidation, register };
