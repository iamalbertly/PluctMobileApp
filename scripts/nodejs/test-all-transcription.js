#!/usr/bin/env node

const PluctCoreUnified = require('../core/Pluct-Core-Unified-New');
const PluctJourney02AutoDiscoveryOrchestrator = require('./Pluct-Journey-02AutoDiscoveryOrchestrator');

/**
 * test-all-transcription.js - Comprehensive test runner for TikTok transcription
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Runs all transcription-related journeys with detailed logging and timeout management
 */
async function runAllTranscriptionTests() {
    console.log('🚀 Starting Pluct Transcription Test Suite');
    console.log('==========================================');
    
    const core = new PluctCoreUnified();
    const orchestrator = new PluctJourney02AutoDiscoveryOrchestrator(core);
    
    try {
        // Step 1: Discover all journeys
        console.log('\n📋 Step 1: Auto-discovering test journeys...');
        const journeys = await orchestrator.discoverJourneys();
        
        if (journeys.length === 0) {
            console.error('❌ No journeys discovered. Check the journeys directory.');
            process.exit(1);
        }
        
        console.log(`✅ Discovered ${journeys.length} test journeys`);
        
        // Step 2: Run all journeys
        console.log('\n🎯 Step 2: Executing all test journeys...');
        const results = await orchestrator.runAllJourneys();
        
        // Step 3: Generate detailed report
        console.log('\n📊 Step 3: Generating test report...');
        const report = orchestrator.generateReport();
        
        // Display summary
        console.log('\n' + '='.repeat(50));
        console.log('📈 TEST EXECUTION SUMMARY');
        console.log('='.repeat(50));
        console.log(`Total Journeys: ${report.summary.totalJourneys}`);
        console.log(`Executed: ${report.summary.totalExecuted}`);
        console.log(`Passed: ${report.summary.passed}`);
        console.log(`Failed: ${report.summary.failed}`);
        console.log(`Success Rate: ${report.summary.successRate}`);
        console.log(`Total Duration: ${results.duration}ms`);
        
        // Display results by type
        console.log('\n📋 Results by Type:');
        Object.entries(report.byType).forEach(([type, count]) => {
            console.log(`  ${type}: ${count} journeys`);
        });
        
        // Display detailed results
        console.log('\n📝 Detailed Results:');
        report.results.forEach((result, index) => {
            const status = result.success ? '✅ PASS' : '❌ FAIL';
            const duration = `${result.duration}ms`;
            console.log(`  ${index + 1}. ${result.name} - ${status} (${duration})`);
            
            if (!result.success && result.result.error) {
                console.log(`     Error: ${result.result.error}`);
                if (result.result.failedStep) {
                    console.log(`     Failed Step: ${result.result.failedStep}`);
                }
            }
            
            if (result.success && result.result.transcript) {
                console.log(`     Transcript: ${result.result.transcript.substring(0, 50)}...`);
            }
        });
        
        // Check for transcription-specific results
        const transcriptionResults = report.results.filter(r => 
            r.name.toLowerCase().includes('transcription')
        );
        
        if (transcriptionResults.length > 0) {
            console.log('\n🎬 Transcription-Specific Results:');
            transcriptionResults.forEach(result => {
                console.log(`  ${result.name}: ${result.success ? '✅' : '❌'}`);
                if (result.success && result.result.transcript) {
                    console.log(`    Transcript Length: ${result.result.transcript.length} characters`);
                }
                if (!result.success && result.result.failedStep) {
                    console.log(`    Failed at: ${result.result.failedStep}`);
                }
            });
        }
        
        // Final status
        console.log('\n' + '='.repeat(50));
        if (report.summary.failed === 0) {
            console.log('🎉 ALL TESTS PASSED! Transcription system is working correctly.');
        } else {
            console.log(`⚠️ ${report.summary.failed} test(s) failed. Check the details above.`);
        }
        console.log('='.repeat(50));
        
        // Exit with appropriate code
        if (report.summary.failed === 0) {
            console.log('\n✅ Test suite completed successfully');
            process.exit(0);
        } else {
            console.log('\n❌ Test suite completed with failures');
            process.exit(1);
        }
        
    } catch (error) {
        console.error('\n❌ Test suite failed with exception:', error.message);
        console.error('Stack trace:', error.stack);
        process.exit(1);
    }
}

// Handle command line arguments
const args = process.argv.slice(2);
if (args.includes('--help') || args.includes('-h')) {
    console.log('Pluct Transcription Test Suite');
    console.log('Usage: node test-all-transcription.js [options]');
    console.log('');
    console.log('Options:');
    console.log('  --help, -h     Show this help message');
    console.log('  --verbose, -v  Enable verbose logging');
    console.log('  --pattern <p>   Run only journeys matching pattern');
    console.log('');
    console.log('Examples:');
    console.log('  node test-all-transcription.js');
    console.log('  node test-all-transcription.js --verbose');
    console.log('  node test-all-transcription.js --pattern "Intent"');
    process.exit(0);
}

// Run the test suite
if (require.main === module) {
    runAllTranscriptionTests().catch(error => {
        console.error('Unhandled error in test suite:', error);
        process.exit(1);
    });
}

module.exports = { runAllTranscriptionTests };
