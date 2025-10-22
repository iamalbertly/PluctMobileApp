const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class ManualURLInputJourney extends BaseJourney {
    async execute() {
        this.core.logger.info('🎯 Testing Manual URL Input end-to-end...');

        const fg = await this.ensureAppForeground();
        if (!fg.success) return { success: false, error: 'App not in foreground' };

        const openResult = await this.core.openCaptureSheet();
        if (!openResult.success) {
            return { success: false, error: `Failed to open capture sheet: ${openResult.error}` };
        }
        
        // Wait for sheet to fully load
        await this.core.sleep(3000);
        
        // Check if capture sheet is visible by looking for the text
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();
        const hasCaptureSheet = uiDump.includes('Capture This Insight');
        
        if (!hasCaptureSheet) {
            // Try waiting a bit more and check again
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            const uiDump2 = this.core.readLastUIDump();
            const hasCaptureSheet2 = uiDump2.includes('Capture This Insight');
            
            if (!hasCaptureSheet2) {
                return { success: false, error: 'Capture sheet not visible' };
            }
        }

        let urlTap = await this.core.tapByText('TikTok URL');
        if (!urlTap.success) {
            urlTap = await this.core.tapFirstEditText();
            if (!urlTap.success) return { success: false, error: 'URL field not found' };
        }
        await this.core.clearEditText();
        const normalized = await this.core.normalizeTikTokUrl(this.core.config.url);
        if (!normalized.valid) return { success: false, error: 'Invalid TikTok URL' };
        await this.core.inputText(normalized.normalized);

        // Quick metadata fetch and log
        const meta = await this.core.fetchHtmlMetadata(normalized.normalized);
        this.core.writeJsonArtifact('manual_url_meta.json', meta);

        // Tap AI Analysis (by contentDescription)
        await this.core.dumpUIHierarchy();
        const xml2 = this.core.readLastUIDump();
        let aiTap;
        if (xml2 && /content-desc=\"ai_analysis\"/i.test(xml2)) {
            const match = xml2.match(/content-desc=\"ai_analysis\"[\s\S]*?bounds=\"(\[[^\"]+\])\"/i);
            if (match) {
                const nums = match[1].match(/\d+/g).map(n=>parseInt(n,10));
                const [x1,y1,x2,y2]=nums; const cx=Math.floor((x1+x2)/2), cy=Math.floor((y1+y2)/2);
                aiTap = await this.core.executeCommand(`adb shell input tap ${cx} ${cy}`);
            }
        }
        if (!aiTap || !aiTap.success) aiTap = await this.core.tapByText('AI Analysis');
        if (!aiTap.success) return { success: false, error: 'AI Analysis option not found' };

        // Track processing with 160s timeout as well
        const result = await this.core.waitForTranscriptResult(160000, 1500);
        if (!result.success) {
            return { success: false, error: `AI Analysis timed out at stage: ${result.finalStage}`, history: result.history };
        }

        return { success: true };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('ManualURLInput', new ManualURLInputJourney(orchestrator.core));
}

module.exports = { register };


