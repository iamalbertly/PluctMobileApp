const PluctCoreUnified = require('./core/Pluct-Core-Unified-New');
const JourneyErrorNotificationValidation = require('./journeys/Journey-ErrorNotificationValidation');
const JourneyBusinessEngineIntegration = require('./journeys/Journey-BusinessEngineIntegration');
const JourneyCurrentAppValidation = require('./journeys/Journey-CurrentAppValidation');

async function runComprehensiveIntegrationTest() {
    const core = new PluctCoreUnified();
    
    try {
        console.log('🚀 Starting Comprehensive Integration Test...');
        
        const results = [];
        
        // Test 1: Current App Validation
        console.log('\n📱 Test 1: Current App Validation');
        const currentAppJourney = new JourneyCurrentAppValidation(core);
        const currentAppResult = await currentAppJourney.execute();
        results.push({ name: 'CurrentAppValidation', result: currentAppResult });
        console.log('Result:', currentAppResult.success ? '✅ PASS' : '❌ FAIL', currentAppResult.message || currentAppResult.error);
        
        // Test 2: Error Notification Validation
        console.log('\n🔴 Test 2: Error Notification Validation');
        const errorJourney = new JourneyErrorNotificationValidation(core);
        const errorResult = await errorJourney.execute();
        results.push({ name: 'ErrorNotificationValidation', result: errorResult });
        console.log('Result:', errorResult.success ? '✅ PASS' : '❌ FAIL', errorResult.message || errorResult.error);
        
        // Test 3: Business Engine Integration
        console.log('\n🔗 Test 3: Business Engine Integration');
        const businessEngineJourney = new JourneyBusinessEngineIntegration(core);
        const businessEngineResult = await businessEngineJourney.execute();
        results.push({ name: 'BusinessEngineIntegration', result: businessEngineResult });
        console.log('Result:', businessEngineResult.success ? '✅ PASS' : '❌ FAIL', businessEngineResult.message || businessEngineResult.error);
        
        // Summary
        console.log('\n📊 Test Summary:');
        const passed = results.filter(r => r.result.success).length;
        const total = results.length;
        
        console.log(`✅ Passed: ${passed}/${total}`);
        console.log(`❌ Failed: ${total - passed}/${total}`);
        
        results.forEach(r => {
            console.log(`  ${r.result.success ? '✅' : '❌'} ${r.name}: ${r.result.success ? 'PASS' : 'FAIL'}`);
        });
        
        const allPassed = passed === total;
        console.log(`\n🎯 Overall Result: ${allPassed ? '✅ ALL TESTS PASSED' : '❌ SOME TESTS FAILED'}`);
        
        return {
            success: allPassed,
            results: results,
            summary: {
                passed,
                total,
                allPassed
            }
        };
        
    } catch (error) {
        console.error('❌ Test execution failed:', error);
        return { success: false, error: error.message };
    } finally {
        // Cleanup if needed
    }
}

// Run the test
runComprehensiveIntegrationTest()
    .then(result => {
        console.log('\n🏁 Final Result:', result);
        process.exit(result.success ? 0 : 1);
    })
    .catch(error => {
        console.error('💥 Fatal error:', error);
        process.exit(1);
    });
