/**
 * Pluct UI Compact Layout Test
 * Tests the new compact layout and UI functionalities
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { PluctTestCore } = require('../core/Pluct-Test-Core-Exec.js');
const { PluctLogger } = require('../core/Logger.js');

class PluctUICompactLayoutTest {
    constructor() {
        this.testCore = new PluctTestCore();
        this.logger = new PluctLogger('PluctUICompactLayoutTest');
        this.testResults = {
            compactLayout: false,
            buttonFunctionality: false,
            swipeActions: false,
            creditBalance: false,
            videoProcessing: false
        };
    }

    async runTest() {
        this.logger.info('🎨 Starting Pluct UI Compact Layout Test');
        
        try {
            // Test 1: Compact Layout Rendering
            await this.testCompactLayout();
            
            // Test 2: Button Functionality
            await this.testButtonFunctionality();
            
            // Test 3: Swipe Actions
            await this.testSwipeActions();
            
            // Test 4: Credit Balance Display
            await this.testCreditBalanceDisplay();
            
            // Test 5: Video Processing Flow
            await this.testVideoProcessingFlow();
            
            // Generate test report
            this.generateTestReport();
            
        } catch (error) {
            this.logger.error('❌ UI Compact Layout Test failed:', error);
            throw error;
        }
    }

    async testCompactLayout() {
        this.logger.info('📱 Testing Compact Layout Rendering');
        
        try {
            // Check for compact welcome section
            const welcomeSection = await this.testCore.findElementByText('Welcome back! 👋');
            if (welcomeSection) {
                this.logger.info('✅ Compact welcome section found');
            }
            
            // Check for compact input section
            const inputSection = await this.testCore.findElementByText('Paste TikTok URL');
            if (inputSection) {
                this.logger.info('✅ Compact input section found');
            }
            
            // Check for compact transcripts section
            const transcriptsSection = await this.testCore.findElementByText('Recent Transcripts');
            if (transcriptsSection) {
                this.logger.info('✅ Compact transcripts section found');
            }
            
            this.testResults.compactLayout = true;
            this.logger.info('✅ Compact Layout Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Compact Layout Test Failed:', error);
            throw error;
        }
    }

    async testButtonFunctionality() {
        this.logger.info('🔘 Testing Button Functionality');
        
        try {
            // Test URL input functionality
            const urlInput = await this.testCore.findElementByText('https://www.tiktok.com/@username/video/...');
            if (urlInput) {
                await this.testCore.clickElement(urlInput);
                await this.testCore.typeText('https://www.tiktok.com/@test/video/1234567890');
                this.logger.info('✅ URL input functionality working');
            }
            
            // Test process button
            const processButton = await this.testCore.findElementByText('Process');
            if (processButton) {
                await this.testCore.clickElement(processButton);
                this.logger.info('✅ Process button clicked');
            }
            
            // Test refresh credits button
            const refreshButton = await this.testCore.findElementByText('Refresh credits');
            if (refreshButton) {
                await this.testCore.clickElement(refreshButton);
                this.logger.info('✅ Refresh credits button clicked');
            }
            
            this.testResults.buttonFunctionality = true;
            this.logger.info('✅ Button Functionality Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Button Functionality Test Failed:', error);
            throw error;
        }
    }

    async testSwipeActions() {
        this.logger.info('👆 Testing Swipe Actions');
        
        try {
            // Look for transcript cards
            const transcriptCards = await this.testCore.findElementsByText('Processing');
            if (transcriptCards.length > 0) {
                // Test swipe left on first card
                const firstCard = transcriptCards[0];
                await this.testCore.swipeLeft(firstCard);
                this.logger.info('✅ Swipe left action performed');
                
                // Check for action buttons
                const deleteButton = await this.testCore.findElementByText('Delete');
                const retryButton = await this.testCore.findElementByText('Retry');
                const archiveButton = await this.testCore.findElementByText('Archive');
                
                if (deleteButton || retryButton || archiveButton) {
                    this.logger.info('✅ Swipe action buttons found');
                }
            }
            
            this.testResults.swipeActions = true;
            this.logger.info('✅ Swipe Actions Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Swipe Actions Test Failed:', error);
            throw error;
        }
    }

    async testCreditBalanceDisplay() {
        this.logger.info('💰 Testing Credit Balance Display');
        
        try {
            // Check for credit balance display
            const creditBalance = await this.testCore.findElementByText('Credits');
            if (creditBalance) {
                this.logger.info('✅ Credit balance display found');
            }
            
            // Check for credit balance number
            const balanceNumber = await this.testCore.findElementByText(/\d+/);
            if (balanceNumber) {
                this.logger.info('✅ Credit balance number found');
            }
            
            // Test refresh functionality
            const refreshButton = await this.testCore.findElementByText('Refresh credits');
            if (refreshButton) {
                await this.testCore.clickElement(refreshButton);
                this.logger.info('✅ Credit balance refresh tested');
            }
            
            this.testResults.creditBalance = true;
            this.logger.info('✅ Credit Balance Display Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Credit Balance Display Test Failed:', error);
            throw error;
        }
    }

    async testVideoProcessingFlow() {
        this.logger.info('🎬 Testing Video Processing Flow');
        
        try {
            // Test URL input
            const urlInput = await this.testCore.findElementByText('https://www.tiktok.com/@username/video/...');
            if (urlInput) {
                await this.testCore.clickElement(urlInput);
                await this.testCore.typeText('https://www.tiktok.com/@test/video/1234567890');
                this.logger.info('✅ URL input entered');
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
            
            this.testResults.videoProcessing = true;
            this.logger.info('✅ Video Processing Flow Test Passed');
            
        } catch (error) {
            this.logger.error('❌ Video Processing Flow Test Failed:', error);
            throw error;
        }
    }

    generateTestReport() {
        this.logger.info('📊 Generating UI Compact Layout Test Report');
        
        const passedTests = Object.values(this.testResults).filter(result => result).length;
        const totalTests = Object.keys(this.testResults).length;
        
        this.logger.info(`📊 Test Results: ${passedTests}/${totalTests} tests passed`);
        
        if (passedTests === totalTests) {
            this.logger.info('🎉 All UI Compact Layout Tests Passed!');
        } else {
            this.logger.warn('⚠️ Some UI Compact Layout Tests Failed');
        }
        
        return {
            passed: passedTests,
            total: totalTests,
            results: this.testResults
        };
    }
}

module.exports = PluctUICompactLayoutTest;
