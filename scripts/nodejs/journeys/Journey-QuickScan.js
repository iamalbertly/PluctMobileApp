const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class QuickScanJourney extends BaseJourney {
    async execute() {
        this.core.logger.info('🎯 Testing QuickScan end-to-end...');

        const fg = await this.ensureAppForeground();
        if (!fg.success) return { success: false, error: 'App not in foreground' };

        // Open capture sheet
        const openResult = await this.core.openCaptureSheet();
        if (!openResult.success) {
            return { success: false, error: `Failed to open capture sheet: ${openResult.error}` };
        }
        
        // Wait for sheet to fully load
        await this.core.sleep(2000);
        const sheet = await this.core.waitForText('Capture This Insight', 5000);
        if (!sheet.success) return { success: false, error: 'Capture sheet not visible' };

        // Enter URL
        let urlTap = await this.core.tapByText('TikTok URL');
        if (!urlTap.success) {
            urlTap = await this.core.tapFirstEditText();
            if (!urlTap.success) return { success: false, error: 'URL field not found' };
        }
        await this.core.inputText(this.core.config.url);

        // Tap Quick Scan (by contentDescription)
        await this.core.dumpUIHierarchy();
        const xml1 = this.core.readLastUIDump();
        let quickTap;
        if (xml1 && /content-desc=\"quick_scan\"/i.test(xml1)) {
            const match = xml1.match(/content-desc=\"quick_scan\"[\s\S]*?bounds=\"(\[[^\"]+\])\"/i);
            if (match) {
                const nums = match[1].match(/\d+/g).map(n=>parseInt(n,10));
                const [x1,y1,x2,y2]=nums; const cx=Math.floor((x1+x2)/2), cy=Math.floor((y1+y2)/2);
                quickTap = await this.core.executeCommand(`adb shell input tap ${cx} ${cy}`);
            }
        }
        if (!quickTap || !quickTap.success) quickTap = await this.core.tapByText('Quick Scan');
        if (!quickTap.success) return { success: false, error: 'Quick Scan option not found' };

        // Validate processing end-to-end with 160s timeout
        const result = await this.core.waitForTranscriptResult(160000, 1500);
        if (!result.success) {
            return { success: false, error: `QuickScan timed out at stage: ${result.finalStage}`, history: result.history };
        }

        return { success: true };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('QuickScan', new QuickScanJourney(orchestrator.core));
}

module.exports = { register };


