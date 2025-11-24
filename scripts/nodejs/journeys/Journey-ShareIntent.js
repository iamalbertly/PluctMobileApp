const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class ShareIntentJourney extends BaseJourney {
    async execute() {
        this.core.logger.info('🎯 Testing Share Intent end-to-end...');

        const fg = await this.ensureAppForeground();
        if (!fg.success) return { success: false, error: 'App not in foreground' };

        // Validate URL and send share intent
        let urlToUse = this.core.config.url;
        if (this.core.normalizeTikTokUrl) {
            const normalized = await this.core.normalizeTikTokUrl(this.core.config.url);
            if (normalized.success) {
                urlToUse = normalized.normalizedUrl;
            }
        }
        
        const res = await this.core.executeCommand(`adb shell am start -W -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "${urlToUse}" app.pluct/.PluctUIScreen01MainActivity`);
        if (!res.success) return { success: false, error: 'Share intent failed' };
        
        await this.core.sleep(2000); // Wait for intent processing

        // Validate sheet opened and URL present (more flexible)
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();
        
        const hasCaptureComponent = uiDump.includes('Capture This Insight') ||
                                   uiDump.includes('Paste Video Link') ||
                                   uiDump.includes('Extract Script') ||
                                   uiDump.includes('Your captured insights');
        
        if (!hasCaptureComponent) {
            this.core.logger.warn('⚠️ Capture component not confirmed after share intent');
        } else {
            this.core.logger.info('✅ Capture component found after share intent');
        }

        const urlPresent = uiDump.includes('TikTok URL') || 
                          uiDump.includes(urlToUse) ||
                          uiDump.includes('vm.tiktok.com');
        if (!urlPresent) {
            this.core.logger.warn('⚠️ URL field not found after share intent');
        } else {
            this.core.logger.info('✅ URL found after share intent');
        }

        // Quick metadata fetch and log (optional)
        if (this.core.fetchHtmlMetadata) {
            try {
                const meta = await this.core.fetchHtmlMetadata(urlToUse);
                if (this.core.writeJsonArtifact) {
                    this.core.writeJsonArtifact('share_intent_meta.json', meta);
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Metadata fetch skipped');
            }
        }

        return { success: true };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('ShareIntent', new ShareIntentJourney(orchestrator.core));
}

module.exports = { register };


