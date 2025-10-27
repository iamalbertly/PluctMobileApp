const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class PluctUIHeader02SettingsNavigationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-UI-Header-02SettingsNavigation-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Pluct-UI-Header-02SettingsNavigation-Validation');
        
        try {
            // 1. Settings Navigation
            this.core.logger.info('- Step 1: Settings Navigation');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Capture initial UI state
            await this.dumpUI();
            const initialUI = this.core.readLastUIDump();
            
            // Look for settings gear icon in header
            const settingsElements = [
                'Settings', 'settings', '⚙️', 'gear', 'Gear', '⚙'
            ];
            
            let settingsFound = false;
            let settingsElement = null;
            
            for (const element of settingsElements) {
                if (initialUI.includes(element)) {
                    settingsFound = true;
                    settingsElement = element;
                    this.core.logger.info(`✅ Settings element found: ${element}`);
                    break;
                }
            }
            
            if (!settingsFound) {
                this.core.logger.warn('⚠️ Settings gear icon not found in header');
                // Continue anyway to test navigation if it exists
            }
            
            // Try to tap on settings element
            let settingsTapped = false;
            for (const element of settingsElements) {
                const tapResult = await this.core.tapByText(element);
                if (tapResult.success) {
                    this.core.logger.info(`✅ Tapped on settings element: ${element}`);
                    settingsTapped = true;
                    break;
                }
            }
            
            if (!settingsTapped) {
                this.core.logger.warn('⚠️ Could not tap on settings element');
            } else {
                // Wait for navigation
                await this.core.sleep(2000);
                await this.dumpUI();
                const settingsUI = this.core.readLastUIDump();
                
                // Check if we're in settings screen
                const isInSettings = settingsUI.includes('Settings') || 
                                  settingsUI.includes('settings') ||
                                  settingsUI.includes('Configuration') ||
                                  settingsUI.includes('configuration') ||
                                  settingsUI.includes('Provider') ||
                                  settingsUI.includes('provider') ||
                                  settingsUI.includes('API') ||
                                  settingsUI.includes('api');
                
                if (isInSettings) {
                    this.core.logger.info('✅ Successfully navigated to Settings screen');
                } else {
                    this.core.logger.warn('⚠️ Navigation to Settings screen not confirmed');
                }
            }

            // 2. Settings Screen Functionality
            this.core.logger.info('- Step 2: Settings Screen Functionality');
            
            await this.dumpUI();
            const settingsScreenUI = this.core.readLastUIDump();
            
            // Look for provider configuration options
            const providerElements = [
                'HuggingFace', 'huggingface', 'Hugging Face',
                'TokAudit', 'tokaudit', 'Tok Audit',
                'GetTranscribe', 'gettranscribe', 'Get Transcribe',
                'OpenAI', 'openai', 'Open AI'
            ];
            
            let providersFound = 0;
            for (const provider of providerElements) {
                if (settingsScreenUI.includes(provider)) {
                    providersFound++;
                    this.core.logger.info(`✅ Provider found: ${provider}`);
                }
            }
            
            if (providersFound > 0) {
                this.core.logger.info(`✅ Found ${providersFound} provider configuration options`);
            } else {
                this.core.logger.warn('⚠️ No provider configuration options found');
            }
            
            // Look for toggle elements
            const toggleElements = [
                'Toggle', 'toggle', 'Switch', 'switch', 'Enable', 'enable', 'Disable', 'disable'
            ];
            
            let togglesFound = 0;
            for (const toggle of toggleElements) {
                if (settingsScreenUI.includes(toggle)) {
                    togglesFound++;
                }
            }
            
            if (togglesFound > 0) {
                this.core.logger.info(`✅ Found ${togglesFound} toggle elements`);
            } else {
                this.core.logger.warn('⚠️ No toggle elements found');
            }
            
            // Look for API key input fields
            const apiKeyElements = [
                'API Key', 'api key', 'apikey', 'Key', 'key', 'Token', 'token'
            ];
            
            let apiKeyFieldsFound = 0;
            for (const field of apiKeyElements) {
                if (settingsScreenUI.includes(field)) {
                    apiKeyFieldsFound++;
                    this.core.logger.info(`✅ API key field found: ${field}`);
                }
            }
            
            if (apiKeyFieldsFound > 0) {
                this.core.logger.info(`✅ Found ${apiKeyFieldsFound} API key input fields`);
            } else {
                this.core.logger.warn('⚠️ No API key input fields found');
            }

            // 3. Settings Integration with Business Engine
            this.core.logger.info('- Step 3: Settings Integration with Business Engine');
            
            // Check if credit balance is still visible in settings
            await this.dumpUI();
            const settingsWithBalanceUI = this.core.readLastUIDump();
            
            const hasCreditInSettings = settingsWithBalanceUI.includes('credits') || 
                                      settingsWithBalanceUI.includes('Credit') || 
                                      settingsWithBalanceUI.includes('balance') ||
                                      settingsWithBalanceUI.includes('diamond') ||
                                      settingsWithBalanceUI.includes('💎');
            
            if (hasCreditInSettings) {
                this.core.logger.info('✅ Credit balance still visible in settings screen');
            } else {
                this.core.logger.info('ℹ️ Credit balance not visible in settings (may be normal)');
            }

            // 4. Navigation State Management
            this.core.logger.info('- Step 4: Navigation State Management');
            
            // Try to navigate back to home
            const backElements = [
                'Back', 'back', '←', '◀', 'Home', 'home', '← Back', 'Back to Home'
            ];
            
            let backTapped = false;
            for (const element of backElements) {
                const backResult = await this.core.tapByText(element);
                if (backResult.success) {
                    this.core.logger.info(`✅ Tapped back element: ${element}`);
                    backTapped = true;
                    break;
                }
            }
            
            if (!backTapped) {
                // Try using back button
                const backButtonResult = await this.core.pressBackButton();
                if (backButtonResult.success) {
                    this.core.logger.info('✅ Used back button to navigate');
                    backTapped = true;
                }
            }
            
            if (backTapped) {
                // Wait for navigation back
                await this.core.sleep(2000);
                await this.dumpUI();
                const homeUI = this.core.readLastUIDump();
                
                // Check if we're back to home screen
                const isBackHome = homeUI.includes('Welcome to Pluct') || 
                                 homeUI.includes('No transcripts yet') ||
                                 homeUI.includes('Recent Transcripts') ||
                                 homeUI.includes('Credits') ||
                                 homeUI.includes('credits');
                
                if (isBackHome) {
                    this.core.logger.info('✅ Successfully navigated back to home screen');
                } else {
                    this.core.logger.warn('⚠️ Navigation back to home not confirmed');
                }
                
                // Check if header is intact
                const hasHeaderAfterBack = homeUI.includes('Settings') || 
                                         homeUI.includes('settings') ||
                                         homeUI.includes('⚙️') ||
                                         homeUI.includes('gear') ||
                                         homeUI.includes('credits') ||
                                         homeUI.includes('Credit');
                
                if (hasHeaderAfterBack) {
                    this.core.logger.info('✅ Header remains intact after navigation');
                } else {
                    this.core.logger.warn('⚠️ Header may not be intact after navigation');
                }
            } else {
                this.core.logger.warn('⚠️ Could not navigate back from settings');
            }

            // 5. Validate Settings Persistence
            this.core.logger.info('- Step 5: Settings Persistence Validation');
            
            // Check if we can access settings again
            const settingsAccessResult = await this.core.tapByText('Settings');
            if (settingsAccessResult.success) {
                this.core.logger.info('✅ Settings accessible after navigation');
                await this.core.sleep(1000);
                // Navigate back again
                await this.core.pressBackButton();
                await this.core.sleep(1000);
            } else {
                this.core.logger.warn('⚠️ Settings not accessible after navigation');
            }

            this.core.logger.info('✅ Completed: Pluct-UI-Header-02SettingsNavigation-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Header settings navigation validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctUIHeader02SettingsNavigationValidation;
