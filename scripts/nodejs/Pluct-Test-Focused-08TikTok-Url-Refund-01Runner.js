/**
 * Pluct-Test-Focused-08TikTok-Url-Refund-01Runner — credit-fairness + invalid TikTok URL ADB validation.
 * Usage: node scripts/nodejs/Pluct-Test-Focused-08TikTok-Url-Refund-01Runner.js
 */
const PluctMainOrchestrator = require('./Pluct-Main-01Orchestrator');

const tests = ['Journey-UX-26TikTok-Url-Refund-NoCharge-01Validation.js'];

async function main() {
    const orchestrator = new PluctMainOrchestrator();
    const result = await orchestrator.run({ tests, forceFull: true });
    process.exit(result.success ? 0 : 1);
}

main().catch((e) => {
    console.error(e);
    process.exit(1);
});
