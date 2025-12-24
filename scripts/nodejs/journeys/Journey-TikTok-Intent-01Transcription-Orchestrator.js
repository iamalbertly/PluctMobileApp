/**
 * Journey-TikTok-Intent-01Transcription-Orchestrator
 * Orchestrates child journeys that each validate a single concern:
 *  01 Vend token → 02 Submit transcription → 03 Poll status → 04 UI validation
 */

const { ChildVendToken } = require('./Journey-TikTok-Intent-01Transcription-01VendToken');
const { ChildSubmitTranscription } = require('./Journey-TikTok-Intent-01Transcription-02SubmitTranscription');
const { ChildPollStatus } = require('./Journey-TikTok-Intent-01Transcription-03PollStatus');
const { ChildUIValidation } = require('./Journey-TikTok-Intent-01Transcription-04UIValidation');
const { exec } = require('child_process');
const { promisify } = require('util');
const execAsync = promisify(exec);

class TikTokIntentTranscriptionOrchestrator {
    constructor() {
        this.name = 'TikTok-Intent-01Transcription-Orchestrator';
        this.child01 = new ChildVendToken();
        this.child02 = new ChildSubmitTranscription();
        this.child03 = new ChildPollStatus();
        this.child04 = new ChildUIValidation();
    }

    async execute() {
        // Clear logcat at start to track token vends accurately
        try {
            await execAsync('adb logcat -c');
        } catch (err) {
            // Ignore errors - logcat clear is optional
        }
        
        const results = { vend: null, submit: null, poll: null, ui: null, tokenCount: 0 };
        
        // Track token vends before starting
        let vendCountBefore = 0;
        try {
            const logcatBefore = await execAsync('adb logcat -d | findstr /i "vend-token vendToken TOKEN_MANAGEMENT token_vend"');
            const matches = (logcatBefore.stdout || '').match(/vend-token|vendToken|token_vend/g);
            vendCountBefore = matches ? matches.length : 0;
        } catch (err) {
            // Ignore errors - counting is best effort
        }
        
        results.vend = await this.child01.execute();
        if (!results.vend.success) throw new Error(`Vend token failed: ${results.vend.error}`);

        results.submit = await this.child02.execute({ serviceToken: results.vend.token });
        if (!results.submit.success) throw new Error(`Submit transcription failed: ${results.submit.error}`);

        results.poll = await this.child03.execute({ serviceToken: results.vend.token, jobId: results.submit.jobId });
        if (!results.poll.success && !results.poll.warning) throw new Error(`Polling failed: ${results.poll.error}`);

        // Count token vends after completion
        let vendCountAfter = 0;
        try {
            const logcatAfter = await execAsync('adb logcat -d | findstr /i "vend-token vendToken TOKEN_MANAGEMENT token_vend"');
            const matches = (logcatAfter.stdout || '').match(/vend-token|vendToken|token_vend/g);
            vendCountAfter = matches ? matches.length : 0;
        } catch (err) {
            // Ignore errors - counting is best effort
        }
        results.tokenCount = vendCountAfter - vendCountBefore;
        
        // Validate token count
        if (results.tokenCount > 1) {
            throw new Error(`Expected 1 token vend, but found ${results.tokenCount} vends`);
        }

        results.ui = await this.child04.execute();
        if (!results.ui.success && !results.ui.warning) throw new Error(`UI validation failed: ${results.ui.error}`);

        return { success: true, results };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('TikTok-Intent-01Transcription-Orchestrator', new TikTokIntentTranscriptionOrchestrator());
}

module.exports = { TikTokIntentTranscriptionOrchestrator, register };


