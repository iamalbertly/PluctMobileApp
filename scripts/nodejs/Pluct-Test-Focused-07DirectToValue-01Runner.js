/**
 * Pluct-Test-Focused-07DirectToValue-01Runner — runs direct-to-value validation journeys only.
 * Usage: node scripts/nodejs/Pluct-Test-Focused-07DirectToValue-01Runner.js
 */
const PluctMainOrchestrator = require('./Pluct-Main-01Orchestrator');

const tests = [
    'Journey-UX-25DirectToValue-Readiness-01Validation.js',
    'Journey-Intent-03TikTok-04BalanceRace-01Validation.js'
];

async function main() {
    const orchestrator = new PluctMainOrchestrator();
    const result = await orchestrator.run({ tests, forceFull: true });
    process.exit(result.success ? 0 : 1);
}

main().catch((e) => {
    console.error(e);
    process.exit(1);
});
