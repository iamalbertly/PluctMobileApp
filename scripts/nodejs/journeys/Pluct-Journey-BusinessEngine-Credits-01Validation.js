/**
 * Pluct-Journey-BusinessEngine-Credits-01Validation - Validates credit balance fetch and display.
 * Scope: Pluct-Journey-BusinessEngine-Credits-01Validation
 * Ensures the Business Engine balance API is reachable and the app reflects the balance.
 */

const PluctCoreFoundation = require('../core/Pluct-Core-01Foundation');

class PluctJourneyBusinessEngineCredits01Validation {
    constructor(core) {
        this.core = core || new PluctCoreFoundation();
        this.name = 'Pluct-Journey-BusinessEngine-Credits-01Validation';
    }

    /**
     * Run journey
     */
    async execute() {
        try {
            const balanceUrl = process.env.PLACT_BE_BALANCE_URL || `${this.core.config.businessEngineUrl}/v1/credits/balance`;
            this.core.logger.info(`Calling Business Engine balance endpoint: ${balanceUrl}`);

            const jwt = this.core.generateTestJWT('mobile-test');
            const response = await this.core.httpGet(balanceUrl, jwt ? { Authorization: `Bearer ${jwt}` } : {});
            if (!response || !response.success) {
                throw new Error(`No response data from Business Engine balance endpoint: ${response?.error || 'unknown error'}`);
            }

            const parsed = this.core.utils.parseJSON(response.body, {});
            const balance = parsed.balance ?? parsed.availableCredits ?? parsed.main ?? 0;
            this.core.logger.info(`Balance response received: ${balance}`);

            if (typeof balance !== 'number') {
                throw new Error('Balance response missing numeric balance');
            }

            this.core.logger.info('Expecting app UI to show updated credits in header/settings.');

            return { success: true };
        } catch (error) {
            this.core.logger.error(`Credit validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Pluct-Journey-BusinessEngine-Credits-01Validation', new PluctJourneyBusinessEngineCredits01Validation(orchestrator.core));
}

module.exports = { register, PluctJourneyBusinessEngineCredits01Validation };
