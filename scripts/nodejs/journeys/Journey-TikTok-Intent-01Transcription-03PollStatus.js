const PluctCoreFoundation = require('../core/Pluct-Core-01Foundation');

class ChildPollStatus {
    constructor() {
        this.core = new PluctCoreFoundation();
    }

    async execute(params = {}) {
        const { serviceToken, jobId } = params;
        try {
            this.core.logger.info(`⏳ [Child-03] Polling /ttt/status/${jobId} ...`);
            const maxAttempts = 20;
            for (let i = 0; i < maxAttempts; i++) {
                if (jobId) {
                    const url = `${this.core.config.businessEngineUrl}/ttt/status/${jobId}`;
                    const headers = { Authorization: `Bearer ${serviceToken}` };
                    const res = await this.core.httpGet(url, headers);
                    if (res.success && res.status === 200) {
                        const data = JSON.parse(res.body);
                        this.core.logger.info(`[Child-03] Attempt ${i + 1}: status=${data.status}, progress=${data.progress}`);
                        if (data.status === 'completed') return { success: true, details: data };
                        if (data.status === 'failed') return { success: false, error: 'Transcription failed' };
                    }
                } else {
                    // No jobId: infer status from UI/logcat signals
                    await this.core.dumpUIHierarchy();
                    const uiDump = this.core.readLastUIDump();
                    if (uiDump.includes('transcript') || uiDump.includes('completed')) {
                        return { success: true, details: { status: 'completed', source: 'ui' } };
                    }
                    const logRes = await this.core.executeCommand('adb logcat -d | findstr /i "CaptureCard completed transcript API Error TTTranscribe"');
                    if (logRes.success && /completed|transcript/i.test(logRes.stdout || '')) {
                        return { success: true, details: { status: 'completed', source: 'logcat' } };
                    }
                }
                await this.core.sleep(5000);
            }
            return { success: false, warning: 'Timeout waiting for completion' };
        } catch (err) {
            return { success: false, error: err.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('TikTok-Intent-01Transcription-03PollStatus', new ChildPollStatus());
}

module.exports = { ChildPollStatus, register };


