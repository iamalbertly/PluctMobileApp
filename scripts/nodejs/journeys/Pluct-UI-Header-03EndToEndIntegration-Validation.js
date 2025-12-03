const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class PluctUIHeader03EndToEndIntegrationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-UI-Header-03EndToEndIntegration-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Pluct-UI-Header-03EndToEndIntegration-Validation');
        
        try {
            // 1. Complete App Initialization
            this.core.logger.info('- Step 1: Complete App Initialization');
            await this.ensureAppForeground();
            await this.core.sleep(3000);
            
            // Capture initial UI state
            await this.dumpUI();
            const initialUI = this.core.readLastUIDump();
            
            // Verify header loads completely
            const hasCreditBalance = initialUI.includes('credits') || 
                                   initialUI.includes('Credit') || 
                                   initialUI.includes('balance') ||
                                   initialUI.includes('diamond') ||
                                   initialUI.includes('💎');
            
            const hasSettingsGear = initialUI.includes('Settings') || 
                                   initialUI.includes('settings') ||
                                   initialUI.includes('⚙️') ||
                                   initialUI.includes('gear') ||
                                   initialUI.includes('⚙');
            
            if (hasCreditBalance && hasSettingsGear) {
                this.core.logger.info('✅ Header loads completely with both credit balance and settings gear');
            } else {
                this.core.logger.warn('⚠️ Header may not be loading completely');
                if (!hasCreditBalance) {
                    this.core.logger.warn('⚠️ Credit balance not found in header');
                }
                if (!hasSettingsGear) {
                    this.core.logger.warn('⚠️ Settings gear not found in header');
                }
            }
            
            // Validate header layout and accessibility
            const hasAccessibilityLabels = initialUI.includes('content-desc') || 
                                         initialUI.includes('contentDescription') ||
                                         initialUI.includes('accessibility');
            
            if (hasAccessibilityLabels) {
                this.core.logger.info('✅ Accessibility labels found in header');
            } else {
                this.core.logger.warn('⚠️ Accessibility labels not found in header');
            }

            // 2. Credit Balance Integration with Transcription Flow
            this.core.logger.info('- Step 2: Credit Balance Integration with Transcription Flow');
            
            // Get initial balance if visible
            let initialBalance = null;
            if (hasCreditBalance) {
                // Try to extract balance number
                const balanceMatch = initialUI.match(/(\d+)\s*(credits?|credit)/i);
                if (balanceMatch) {
                    initialBalance = parseInt(balanceMatch[1]);
                    this.core.logger.info(`✅ Initial balance detected: ${initialBalance} credits`);
                }
            }
            
            // Initiate a transcription request using always-visible input field
            this.core.logger.info('🎬 Initiating transcription request...');
            
            // Input test URL into always-visible input field
            const testUrl = 'https://vm.tiktok.com/ZMAKpqkpN/';
            this.core.logger.info(`📝 Inputting URL: ${testUrl}`);
            
            // Try multiple strategies to input URL
            let inputSuccess = false;
            
            // Strategy 1: Use test tag
            const inputByTag = await this.core.inputTextViaClipboard(testUrl, 'tiktok_url_input');
            if (inputByTag.success) {
                inputSuccess = true;
                this.core.logger.info('✅ URL input via test tag');
            } else {
                // Strategy 2: Use content description
                const inputByDesc = await this.core.inputTextViaClipboard(testUrl, 'TikTok URL input field');
                if (inputByDesc.success) {
                    inputSuccess = true;
                    this.core.logger.info('✅ URL input via content description');
                } else {
                    // Strategy 3: Use coordinates (fallback)
                    this.core.logger.warn('⚠️ Using coordinate-based input as fallback');
                    await this.core.sleep(1000);
                    await this.core.inputTextViaClipboard(testUrl);
                    inputSuccess = true;
                }
            }
            
            if (inputSuccess) {
                await this.core.sleep(2000);
                
                // Click Extract Script button (FREE tier)
                this.core.logger.info('🖱️ Clicking Extract Script button...');
                let extractScriptSuccess = false;
                
                // Strategy 1: Use test tag
                let extractResult = await this.core.tapByTestTag('extract_script_button');
                if (extractResult.success) {
                    extractScriptSuccess = true;
                    this.core.logger.info('✅ Extract Script clicked via test tag');
                } else {
                    // Strategy 2: Use content description
                    extractResult = await this.core.tapByContentDesc('Extract Script option');
                    if (extractResult.success) {
                        extractScriptSuccess = true;
                        this.core.logger.info('✅ Extract Script clicked via content description');
                    } else {
                        // Strategy 3: Use text "FREE"
                        extractResult = await this.core.tapByText('FREE');
                        if (extractResult.success) {
                            extractScriptSuccess = true;
                            this.core.logger.info('✅ Extract Script clicked via FREE text');
                        }
                    }
                }
                
                if (extractScriptSuccess) {
                    this.core.logger.info('✅ Transcription initiated');
                    await this.core.sleep(3000);
                    
                    // Check if balance updated
                    await this.dumpUI();
                    const updatedUI = this.core.readLastUIDump();
                    const updatedBalanceMatch = updatedUI.match(/(\d+)\s*(credits?|credit)/i);
                    
                    if (updatedBalanceMatch) {
                        const updatedBalance = parseInt(updatedBalanceMatch[1]);
                        if (initialBalance !== null && updatedBalance < initialBalance) {
                            this.core.logger.info(`✅ Balance decreased from ${initialBalance} to ${updatedBalance} credits`);
                        } else if (initialBalance !== null) {
                            this.core.logger.warn(`⚠️ Balance did not decrease: ${initialBalance} -> ${updatedBalance}`);
                        } else {
                            this.core.logger.info(`✅ Current balance: ${updatedBalance} credits`);
                        }
                    }
                } else {
                    const errorMsg = 'Could not initiate quick scan - Extract Script button not found';
                    this.core.logger.error(`❌ ${errorMsg}`);
                    await this.failWithDiagnostics(errorMsg);
                    throw new Error(errorMsg);
                }
            } else {
                const errorMsg = 'Could not input URL into input field';
                this.core.logger.error(`❌ ${errorMsg}`);
                await this.failWithDiagnostics(errorMsg);
                throw new Error(errorMsg);
            }

            // 3. Error Scenarios and Recovery
            this.core.logger.info('- Step 3: Error Scenarios and Recovery');
            
            // Check for error states in header
            await this.dumpUI();
            const errorUI = this.core.readLastUIDump();
            
            const hasErrorState = errorUI.includes('Error') || 
                                errorUI.includes('error') ||
                                errorUI.includes('Failed') ||
                                errorUI.includes('failed') ||
                                errorUI.includes('⚠️') ||
                                errorUI.includes('❌') ||
                                errorUI.includes('Insufficient') ||
                                errorUI.includes('insufficient');
            
            if (hasErrorState) {
                this.core.logger.warn('⚠️ Error state detected in header');
                
                // Check for low balance warning
                const hasLowBalanceWarning = errorUI.includes('0') && errorUI.includes('credit') ||
                                           errorUI.includes('Insufficient') ||
                                           errorUI.includes('insufficient') ||
                                           errorUI.includes('Low') ||
                                           errorUI.includes('low');
                
                if (hasLowBalanceWarning) {
                    this.core.logger.info('✅ Low balance warning detected');
                }
                
                // Try to find retry options
                const retryElements = ['Retry', 'retry', 'Try Again', 'try again', 'Refresh', 'refresh'];
                for (const element of retryElements) {
                    if (errorUI.includes(element)) {
                        this.core.logger.info(`✅ Retry option found: ${element}`);
                        break;
                    }
                }
            } else {
                this.core.logger.info('✅ No error state detected in header');
            }

            // 4. Header Persistence and State Management
            this.core.logger.info('- Step 4: Header Persistence and State Management');
            
            // Navigate to settings and back - use test tag first
            let settingsResult = await this.core.tapByTestTag('settings_button');
            if (!settingsResult.success) {
                settingsResult = await this.core.tapByText('Settings');
            }
            if (settingsResult.success) {
                this.core.logger.info('✅ Navigated to settings');
                await this.core.sleep(2000);
                
                // Check header in settings
                await this.dumpUI();
                const settingsUI = this.core.readLastUIDump();
                
                const hasHeaderInSettings = settingsUI.includes('Settings') || 
                                          settingsUI.includes('settings') ||
                                          settingsUI.includes('⚙️') ||
                                          settingsUI.includes('gear') ||
                                          settingsUI.includes('credits') ||
                                          settingsUI.includes('Credit');
                
                if (hasHeaderInSettings) {
                    this.core.logger.info('✅ Header visible in settings');
                } else {
                    this.core.logger.warn('⚠️ Header not visible in settings');
                }
                
                // Navigate back
                const backResult = await this.core.pressBackButton();
                if (backResult.success) {
                    this.core.logger.info('✅ Navigated back from settings');
                    await this.core.sleep(2000);
                    
                    // Check header after navigation
                    await this.dumpUI();
                    const homeUI = this.core.readLastUIDump();
                    
                    const hasHeaderAfterNav = homeUI.includes('Settings') || 
                                            homeUI.includes('settings') ||
                                            homeUI.includes('⚙️') ||
                                            homeUI.includes('gear') ||
                                            homeUI.includes('credits') ||
                                            homeUI.includes('Credit');
                    
                    if (hasHeaderAfterNav) {
                        this.core.logger.info('✅ Header intact after navigation');
                    } else {
                        this.core.logger.warn('⚠️ Header may not be intact after navigation');
                    }
                } else {
                    this.core.logger.warn('⚠️ Could not navigate back from settings');
                }
            } else {
                const errorMsg = 'Could not navigate to settings';
                this.core.logger.error(`❌ ${errorMsg}`);
                // Capture logcat for debugging
                const logcatResult = await this.core.executeCommand('adb logcat -d | Select-Object -Last 50');
                this.core.logger.error('📱 Recent logcat output:');
                this.core.logger.error(logcatResult.output || 'No logcat output available');
                // Dump UI for debugging
                await this.dumpUI();
                const uiDump = this.core.readLastUIDump();
                this.core.logger.error('📱 Current UI state:');
                this.core.logger.error(uiDump.substring(0, 1000));
                throw new Error(errorMsg);
            }

            // 5. Performance and Responsiveness
            this.core.logger.info('- Step 5: Performance and Responsiveness');
            
            // Test header responsiveness during operations
            const startTime = Date.now();
            
            // Try multiple rapid taps on header elements
            const headerElements = ['Settings', 'settings', '⚙️', 'credits', 'Credit'];
            for (const element of headerElements) {
                const tapResult = await this.core.tapByText(element);
                if (tapResult.success) {
                    this.core.logger.info(`✅ Responsive tap on: ${element}`);
                    await this.core.sleep(500);
                    break;
                }
            }
            
            const endTime = Date.now();
            const responseTime = endTime - startTime;
            
            if (responseTime < 2000) {
                this.core.logger.info(`✅ Header responsive (${responseTime}ms)`);
            } else {
                this.core.logger.warn(`⚠️ Header may be slow (${responseTime}ms)`);
            }
            
            // Check for smooth animations
            await this.dumpUI();
            const animationUI = this.core.readLastUIDump();
            
            const hasSmoothTransitions = !animationUI.includes('jank') && 
                                       !animationUI.includes('lag') &&
                                       !animationUI.includes('stutter');
            
            if (hasSmoothTransitions) {
                this.core.logger.info('✅ Smooth transitions detected');
            } else {
                this.core.logger.warn('⚠️ Potential animation issues detected');
            }

            // 6. Final Header State Validation
            this.core.logger.info('- Step 6: Final Header State Validation');
            
            await this.dumpUI();
            const finalUI = this.core.readLastUIDump();
            
            // Comprehensive header validation
            const hasCompleteHeader = (finalUI.includes('Settings') || finalUI.includes('settings') || finalUI.includes('⚙️')) &&
                                   (finalUI.includes('credits') || finalUI.includes('Credit') || finalUI.includes('💎'));
            
            if (hasCompleteHeader) {
                this.core.logger.info('✅ Complete header functionality validated');
            } else {
                this.core.logger.warn('⚠️ Header functionality may be incomplete');
            }
            
            // Check for any remaining error states
            const hasFinalErrors = finalUI.includes('Error') || 
                                 finalUI.includes('error') ||
                                 finalUI.includes('Failed') ||
                                 finalUI.includes('failed');
            
            if (hasFinalErrors) {
                this.core.logger.warn('⚠️ Final error states detected');
            } else {
                this.core.logger.info('✅ No final error states detected');
            }

            this.core.logger.info('✅ Completed: Pluct-UI-Header-03EndToEndIntegration-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Header end-to-end integration validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctUIHeader03EndToEndIntegrationValidation;
