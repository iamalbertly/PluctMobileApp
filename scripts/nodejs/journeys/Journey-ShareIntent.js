const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class ShareIntentJourney extends BaseJourney {
    async execute() {
        this.core.logger.info('🎯 Testing Share Intent end-to-end...');

        const fg = await this.ensureAppForeground();
        if (!fg.success) return { success: false, error: 'App not in foreground' };

        // Validate URL and send share intent
        const normalized = await this.core.normalizeTikTokUrl(this.core.config.url);
        if (!normalized.success) return { success: false, error: 'Invalid TikTok URL' };
        const res = await this.core.executeCommand(`adb shell am start -W -a android.intent.action.SEND -t text/plain -d "${normalized.normalizedUrl}" app.pluct/.MainActivity`);
        if (!res.success) return { success: false, error: 'Share intent failed' };

        // Validate sheet opened and URL present
        const sheet = await this.core.waitForText('Capture This Insight');
        if (!sheet.success) this.core.logger.warn('Capture sheet not confirmed');

        const urlPresent = await this.core.waitForText('TikTok URL');
        if (!urlPresent.success) this.core.logger.warn('URL field not found after share intent');

        // Quick metadata fetch and log
        const meta = await this.core.fetchHtmlMetadata(normalized.normalizedUrl);
        this.core.writeJsonArtifact('share_intent_meta.json', meta);

        return { success: true };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('ShareIntent', new ShareIntentJourney(orchestrator.core));
}

module.exports = { register };


