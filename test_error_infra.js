const PluctCoreUnified = require('./scripts/nodejs/core/Pluct-Core-Unified-New');
const ErrorInfraCheckJourney = require('./scripts/nodejs/journeys/Journey-ErrorInfraCheck');

async function testErrorInfra() {
    const core = new PluctCoreUnified();
    const journey = new ErrorInfraCheckJourney(core);
    
    console.log('Testing ErrorInfraCheck...');
    const result = await journey.run();
    console.log('Result:', result);
}

testErrorInfra().catch(console.error);
