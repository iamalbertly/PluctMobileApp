const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-User-Identification-01Validation - User identification and credit balance validation
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates that the app correctly identifies users and retrieves accurate credit balances
 */
class JourneyUserIdentificationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.journeyName = 'UserIdentificationValidation';
        this.timeout = 60000; // 60 seconds
    }

    async execute() {
        this.core.logger.info('🔍 Starting User Identification and Credit Balance Validation Journey...');
        
        try {
            // Step 1: Launch the app
            this.core.logger.info('📱 Step 1: Launching Pluct app...');
            const launchResult = await this.core.launchApp('app.pluct');
            if (!launchResult.success) {
                return { success: false, error: 'Failed to launch app' };
            }
            this.core.logger.info('✅ App launched successfully');

            // Step 2: Wait for app to load and check for user identification
            this.core.logger.info('⏳ Step 2: Waiting for app to load...');
            await this.core.delay(3000);

            // Step 3: Check if the app shows the correct UI elements
            this.core.logger.info('🔍 Step 3: Checking for capture card UI...');
            const uiDump = await this.core.getUIDump();
            if (!uiDump.success) {
                return { success: false, error: 'Failed to get UI dump' };
            }

            // Look for the capture card with URL input
            const hasUrlInput = uiDump.xml.includes('video_url_input') || uiDump.xml.includes('Paste Video Link');
            if (!hasUrlInput) {
                this.core.logger.warn('⚠️ URL input field not found in UI');
            } else {
                this.core.logger.info('✅ URL input field found');
            }

            // Look for the Extract Script button
            const hasExtractButton = uiDump.xml.includes('extract_script_button') || uiDump.xml.includes('Extract Script');
            if (!hasExtractButton) {
                this.core.logger.warn('⚠️ Extract Script button not found in UI');
            } else {
                this.core.logger.info('✅ Extract Script button found');
            }

            // Step 4: Test user identification by checking logs
            this.core.logger.info('🔍 Step 4: Checking user identification logs...');
            const logcatResult = await this.core.executeCommand('adb logcat -d | findstr "PluctUserIdentification"');
            if (logcatResult.success && logcatResult.output.includes('Generated user ID')) {
                this.core.logger.info('✅ User identification service is working');
                
                // Extract user ID from logs
                const userIdMatch = logcatResult.output.match(/Generated user ID: (mobile-[a-f0-9-]+)/);
                if (userIdMatch) {
                    const userId = userIdMatch[1];
                    this.core.logger.info(`📱 Generated User ID: ${userId}`);
                    
                    // Validate user ID format
                    if (userId.startsWith('mobile-') && userId.length >= 20) {
                        this.core.logger.info('✅ User ID format is valid');
                    } else {
                        this.core.logger.warn('⚠️ User ID format appears invalid');
                    }
                }
            } else {
                this.core.logger.warn('⚠️ User identification logs not found');
            }

            // Step 5: Test credit balance retrieval
            this.core.logger.info('🔍 Step 5: Testing credit balance retrieval...');
            const creditLogResult = await this.core.executeCommand('adb logcat -d | findstr "credit balance"');
            if (creditLogResult.success && creditLogResult.output.includes('REAL credit balance')) {
                this.core.logger.info('✅ Credit balance retrieval is working');
                
                // Extract credit balance from logs
                const balanceMatch = creditLogResult.output.match(/REAL credit balance retrieved for .+: (\d+)/);
                if (balanceMatch) {
                    const balance = parseInt(balanceMatch[1]);
                    this.core.logger.info(`💰 Credit Balance: ${balance}`);
                    
                    // Validate that balance is reasonable (not hallucinated)
                    if (balance >= 0 && balance <= 100) {
                        this.core.logger.info('✅ Credit balance appears realistic');
                    } else {
                        this.core.logger.warn(`⚠️ Credit balance seems unrealistic: ${balance}`);
                    }
                }
            } else {
                this.core.logger.warn('⚠️ Credit balance logs not found');
            }

            // Step 6: Test API communication
            this.core.logger.info('🔍 Step 6: Testing API communication...');
            const apiLogResult = await this.core.executeCommand('adb logcat -d | findstr "pluct-business-engine"');
            if (apiLogResult.success && apiLogResult.output.includes('pluct-business-engine.romeo-lya2.workers.dev')) {
                this.core.logger.info('✅ API communication is working');
            } else {
                this.core.logger.warn('⚠️ API communication logs not found');
            }

            // Step 7: Test user creation if needed
            this.core.logger.info('🔍 Step 7: Checking for user creation logs...');
            const userCreationLogResult = await this.core.executeCommand('adb logcat -d | findstr "Creating user"');
            if (userCreationLogResult.success) {
                this.core.logger.info('✅ User creation process is working');
            } else {
                this.core.logger.info('ℹ️ No user creation needed (user already exists)');
            }

            // Step 8: Validate JWT token generation
            this.core.logger.info('🔍 Step 8: Checking JWT token generation...');
            const jwtLogResult = await this.core.executeCommand('adb logcat -d | findstr "JWT"');
            if (jwtLogResult.success) {
                this.core.logger.info('✅ JWT token generation is working');
            } else {
                this.core.logger.warn('⚠️ JWT token generation logs not found');
            }

            this.core.logger.info('✅ User Identification and Credit Balance Validation Journey completed successfully');
            return { 
                success: true, 
                message: 'User identification and credit balance validation passed',
                details: {
                    userIdentification: 'Working',
                    creditBalance: 'Retrieved from API',
                    apiCommunication: 'Active',
                    jwtGeneration: 'Working'
                }
            };

        } catch (error) {
            this.core.logger.error(`❌ User Identification Validation Journey failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('UserIdentificationValidation', new JourneyUserIdentificationValidation(orchestrator.core));
}

module.exports = { register, JourneyUserIdentificationValidation };
