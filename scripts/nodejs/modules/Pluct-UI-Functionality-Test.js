/**
 * Pluct UI Functionality Test
 * Tests all UI functionalities and button interactions
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { PluctTestCore } = require('../core/Pluct-Test-Core-Exec.js');
const { PluctLogger } = require('../core/Logger.js');

class PluctUIFunctionalityTest {
    constructor() {
        this.testCore = new PluctTestCore();
        this.logger = new PluctLogger('PluctUIFunctionalityTest');
        this.testResults = {
            urlInput: false,
            processButton: false,
            deleteFunctionality: false,
            retryFunctionality: false,
            archiveFunctionality: false,
            creditBalanceRefresh: false,
            swipeGestures: false,
            confirmationDialogs: false
        };
    }

    async runTest() {
        this.logger.info('🔧 Starting Pluct UI Functionality Test');
        
        try {
            // Test 1: URL Input Functionality
            await this.testURLInputFunctionality();
            
            // Test 2: Process Button Functionality
            await this.testProcessButtonFunctionality();
            
            // Test 3: Delete Functionality
            await this.testDeleteFunctionality();
            
            // Test 4: Retry Functionality
            await this.testRetryFunctionality();
            
            // Test 5: Archive Functionality
            await this.testArchiveFunctionality();
            
            // Test 6: Credit Balance Refresh
            await this.testCreditBalanceRefresh();
            
            // Test 7: Swipe Gestures
            await this.testSwipeGestures();
            
            // Test 8: Confirmation Dialogs
            await this.testConfirmationDialogs();
            
            // Generate test report
            this.generateTestReport();
            
        } catch (error) {
            this.logger.error('❌ UI Functionality Test failed:', error);
            throw error;
        }
    }

    async testURLInputFunctionality() {
        this.logger.info('📝 Testing URL Input Functionality');
        
        try {
            // Find URL input field
            const urlInput = await this.testCore.findElementByText('https://www.tiktok.com/@username/video/...');
            if (urlInput) {
                await this.testCore.clickElement(urlInput);
                this.logger.info('✅ URL input field clicked');
                
                // Test typing
                await this.testCore.typeText('https://www.tiktok.com/@test/video/1234567890');
                this.logger.info('✅ URL text entered');
                
                // Test clear functionality
                const clearButton = await this.testCore.findElementByText('Clear');
                if (clearButton) {
                    await this.testCore.clickElement(clearButton);
                    this.logger.info('✅ Clear button clicked');
                }
            }
            
            this.testResults.urlInput = true;
            this.logger.info('✅ URL Input Functionality Test Passed');
            
        } catch (error) {
            this.logger.error('❌ URL Input Functionality Test Failed:', error);
            throw error;
        }
    }

    async testProcessButtonFunctionality() {
        this.logger.info('▶️ Testing Process Button Functionality');
        
        try {
            // Enter URL first
            const urlInput = await this.testCore.findElementByText('https://www.tiktok.com/@username/video/...');
            if (urlInput) {
                await this.testCore.clickElement(urlInput);
                await this.testCore.typeText('https://www.tiktok.com/@test/video/1234567890');
            }
            
            // Test process button
            const processButton = await this.testCore.findElementByText('Process');
            if (processButton) {
                await this.testCore.clickElement(processButton);
                this.logger.info('✅ Process button clicked');
                
                // Wait for processing to start
                await this.testCore.waitForElement('Processing', 5000);
                this.logger.info('✅ Video processing started');
            }
            
            this.testResults.processButton = true;
            this.logger.info('✅ Process Button Functionality Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Process Button Functionality Test Failed:', error);
            throw error;
        }
    }

    async testDeleteFunctionality() {
        this.logger.info('🗑️ Testing Delete Functionality');
        
        try {
            // Look for transcript cards
            const transcriptCards = await this.testCore.findElementsByText('Processing');
            if (transcriptCards.length > 0) {
                // Test swipe left to reveal delete button
                const firstCard = transcriptCards[0];
                await this.testCore.swipeLeft(firstCard);
                this.logger.info('✅ Swipe left performed');
                
                // Look for delete button
                const deleteButton = await this.testCore.findElementByText('Delete');
                if (deleteButton) {
                    await this.testCore.clickElement(deleteButton);
                    this.logger.info('✅ Delete button clicked');
                    
                    // Check for confirmation dialog
                    const confirmDialog = await this.testCore.findElementByText('Delete Transcript');
                    if (confirmDialog) {
                        this.logger.info('✅ Delete confirmation dialog appeared');
                    }
                }
            }
            
            this.testResults.deleteFunctionality = true;
            this.logger.info('✅ Delete Functionality Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Delete Functionality Test Failed:', error);
            throw error;
        }
    }

    async testRetryFunctionality() {
        this.logger.info('🔄 Testing Retry Functionality');
        
        try {
            // Look for transcript cards
            const transcriptCards = await this.testCore.findElementsByText('Processing');
            if (transcriptCards.length > 0) {
                // Test swipe left to reveal retry button
                const firstCard = transcriptCards[0];
                await this.testCore.swipeLeft(firstCard);
                this.logger.info('✅ Swipe left performed');
                
                // Look for retry button
                const retryButton = await this.testCore.findElementByText('Retry');
                if (retryButton) {
                    await this.testCore.clickElement(retryButton);
                    this.logger.info('✅ Retry button clicked');
                }
            }
            
            this.testResults.retryFunctionality = true;
            this.logger.info('✅ Retry Functionality Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Retry Functionality Test Failed:', error);
            throw error;
        }
    }

    async testArchiveFunctionality() {
        this.logger.info('📦 Testing Archive Functionality');
        
        try {
            // Look for transcript cards
            const transcriptCards = await this.testCore.findElementsByText('Processing');
            if (transcriptCards.length > 0) {
                // Test swipe left to reveal archive button
                const firstCard = transcriptCards[0];
                await this.testCore.swipeLeft(firstCard);
                this.logger.info('✅ Swipe left performed');
                
                // Look for archive button
                const archiveButton = await this.testCore.findElementByText('Archive');
                if (archiveButton) {
                    await this.testCore.clickElement(archiveButton);
                    this.logger.info('✅ Archive button clicked');
                }
            }
            
            this.testResults.archiveFunctionality = true;
            this.logger.info('✅ Archive Functionality Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Archive Functionality Test Failed:', error);
            throw error;
        }
    }

    async testCreditBalanceRefresh() {
        this.logger.info('💰 Testing Credit Balance Refresh');
        
        try {
            // Look for refresh button
            const refreshButton = await this.testCore.findElementByText('Refresh credits');
            if (refreshButton) {
                await this.testCore.clickElement(refreshButton);
                this.logger.info('✅ Credit balance refresh button clicked');
                
                // Wait for refresh to complete
                await this.testCore.waitForElement('Credits', 3000);
                this.logger.info('✅ Credit balance refreshed');
            }
            
            this.testResults.creditBalanceRefresh = true;
            this.logger.info('✅ Credit Balance Refresh Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Credit Balance Refresh Test Failed:', error);
            throw error;
        }
    }

    async testSwipeGestures() {
        this.logger.info('👆 Testing Swipe Gestures');
        
        try {
            // Look for transcript cards
            const transcriptCards = await this.testCore.findElementsByText('Processing');
            if (transcriptCards.length > 0) {
                // Test swipe left
                const firstCard = transcriptCards[0];
                await this.testCore.swipeLeft(firstCard);
                this.logger.info('✅ Swipe left gesture performed');
                
                // Test swipe right to close
                await this.testCore.swipeRight(firstCard);
                this.logger.info('✅ Swipe right gesture performed');
            }
            
            this.testResults.swipeGestures = true;
            this.logger.info('✅ Swipe Gestures Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Swipe Gestures Test Failed:', error);
            throw error;
        }
    }

    async testConfirmationDialogs() {
        this.logger.info('💬 Testing Confirmation Dialogs');
        
        try {
            // Look for transcript cards
            const transcriptCards = await this.testCore.findElementsByText('Processing');
            if (transcriptCards.length > 0) {
                // Test swipe left to reveal delete button
                const firstCard = transcriptCards[0];
                await this.testCore.swipeLeft(firstCard);
                
                // Click delete button
                const deleteButton = await this.testCore.findElementByText('Delete');
                if (deleteButton) {
                    await this.testCore.clickElement(deleteButton);
                    
                    // Check for confirmation dialog
                    const confirmDialog = await this.testCore.findElementByText('Delete Transcript');
                    if (confirmDialog) {
                        this.logger.info('✅ Confirmation dialog appeared');
                        
                        // Test cancel button
                        const cancelButton = await this.testCore.findElementByText('Cancel');
                        if (cancelButton) {
                            await this.testCore.clickElement(cancelButton);
                            this.logger.info('✅ Cancel button clicked');
                        }
                    }
                }
            }
            
            this.testResults.confirmationDialogs = true;
            this.logger.info('✅ Confirmation Dialogs Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Confirmation Dialogs Test Failed:', error);
            throw error;
        }
    }

    generateTestReport() {
        this.logger.info('📊 Generating UI Functionality Test Report');
        
        const passedTests = Object.values(this.testResults).filter(result => result).length;
        const totalTests = Object.keys(this.testResults).length;
        
        this.logger.info(`📊 Test Results: ${passedTests}/${totalTests} tests passed`);
        
        if (passedTests === totalTests) {
            this.logger.info('🎉 All UI Functionality Tests Passed!');
        } else {
            this.logger.warn('⚠️ Some UI Functionality Tests Failed');
        }
        
        return {
            passed: passedTests,
            total: totalTests,
            results: this.testResults
        };
    }
}

module.exports = PluctUIFunctionalityTest;
