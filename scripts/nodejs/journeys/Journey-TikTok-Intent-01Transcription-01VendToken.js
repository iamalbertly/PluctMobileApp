const PluctCoreFoundation = require('../core/Pluct-Core-01Foundation');

class ChildVendToken {
    constructor() {
        this.core = new PluctCoreFoundation();
    }

    async execute() {
        try {
            this.core.logger.info('🎫 [Child-01] Vending service token via Business Engine...');
            const userJwt = this.core.generateTestJWT('mobile');
            const url = `${this.core.config.businessEngineUrl}/v1/vend-token`;
            const payload = { userId: 'mobile', clientRequestId: `req_${Date.now()}` };
            const headers = { Authorization: `Bearer ${userJwt}`, 'Content-Type': 'application/json' };
            const res = await this.core.httpPost(url, payload, headers);
            if (!res.success || res.status !== 200) {
                return { success: false, error: res.body || res.error };
            }
            const data = JSON.parse(res.body);
            this.core.logger.info(`✅ [Child-01] Vended token, expiresIn=${data.expiresIn}, balanceAfter=${data.balanceAfter}`);
            return { success: true, token: data.token, requestId: data.requestId };
        } catch (err) {
            return { success: false, error: err.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('TikTok-Intent-01Transcription-01VendToken', new ChildVendToken());
}

module.exports = { ChildVendToken, register };


