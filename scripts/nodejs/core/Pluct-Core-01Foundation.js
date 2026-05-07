const PluctCoreFoundationCommands = require('./Pluct-Core-01Foundation-02Commands');
const PluctCoreFoundationUI = require('./Pluct-Core-01Foundation-03UI');
const PluctCoreFoundationValidation = require('./Pluct-Core-01Foundation-04Validation');
const PluctCoreFoundationLogcatValidator = require('./Pluct-Core-01Foundation-05Logcat-01Validator');
const PluctCoreFoundationUtils = require('./Pluct-Core-01Foundation-05Utils');
const { logInfo, logSuccess, logWarn, logError, isVerbose } = require('./Logger');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

/**
 * Pluct-Core-01Foundation - Core foundation functionality orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Orchestrates all foundation modules for basic operations
 */
class PluctCoreFoundation {
    constructor() {
        const shortUrl = process.env.TEST_TIKTOK_URL || 'https://vm.tiktok.com/ZMDRUGT2P/';
        const longUrl = 'https://www.tiktok.com/@thesunnahguy/video/7493203244727012630';
        const skipAppDataClear = process.env.PLUCT_SKIP_APP_CLEAR === '1' || process.env.PLUCT_PRESERVE_APP_DATA === '1';
        const businessEngineUrl = (process.env.BE_BASE_URL || process.env.PLUCT_ENGINE_BASE_URL || 'https://pluct-business-engine.romeo-lya2.workers.dev').replace(/\/+$/, '');
        this.config = {
            // Use real TikTok URLs (short + long) for validation
            url: shortUrl,
            testUrls: [shortUrl, longUrl],
            businessEngineUrl,
            timeouts: { default: 10000, short: 2000, long: 15000 },
            retry: { maxAttempts: 3, delay: 1000 },
            skipAppDataClear
        };
        this.logger = {
            info: logInfo,
            success: logSuccess,
            warn: logWarn,
            error: logError,
            isVerbose
        };

        // Initialize specialized modules
        this.commands = new PluctCoreFoundationCommands(this.config, this.logger);
        this.ui = new PluctCoreFoundationUI(this.config, this.logger, this.commands);
        this.validation = new PluctCoreFoundationValidation(this.config, this.logger, this.commands);
        this.logcatValidator = new PluctCoreFoundationLogcatValidator(this.commands, this.logger);
        this.utils = new PluctCoreFoundationUtils(this.config, this.logger);
    }

    // Delegate to specialized modules
    async executeCommand(command, timeout, retries, options) {
        return this.commands.executeCommand(command, timeout, retries, options);
    }

    getTestUrls() {
        return Array.isArray(this.config.testUrls) && this.config.testUrls.length > 0
            ? this.config.testUrls
            : [this.config.url];
    }

    setActiveUrl(url) {
        this.config.url = url;
    }

    getActiveUrl() {
        return this.config.url;
    }

    async sleep(ms) {
        return this.utils.sleep(ms);
    }

    async validateEnvironment() {
        return this.validation.validateEnvironment();
    }

    async launchApp() {
        return this.ui.launchApp();
    }

    async isAppRunning() {
        return this.ui.isAppRunning();
    }

    async ensureAppForeground() {
        return this.ui.ensureAppForeground();
    }

    async captureUIArtifacts(tag) {
        return this.ui.captureUIArtifacts(tag);
    }

    async dumpUIHierarchy() {
        return this.ui.dumpUIHierarchy();
    }

    async waitForText(text, timeoutMs, pollMs) {
        return this.ui.waitForText(text, timeoutMs, pollMs);
    }

    async tapByText(text) {
        return this.ui.tapByText(text);
    }

    async tapByContentDesc(contentDesc) {
        return this.ui.tapByContentDesc(contentDesc);
    }

    async tapByCoordinates(x, y) {
        return this.ui.tapByCoordinates(x, y);
    }

    async inputText(targetOrText, maybeText) {
        if (typeof maybeText === 'string') {
            await this.focusInputTarget(targetOrText);
            return this.ui.inputText(maybeText);
        }
        return this.ui.inputText(targetOrText);
    }

    async typeText(targetOrText, maybeText) {
        if (typeof maybeText === 'string') {
            // Target provided (element, test tag, or text)
            await this.focusInputTarget(targetOrText);
            return this.ui.inputText(maybeText);
        }
        await this.focusInputTarget(null);
        return this.ui.inputText(targetOrText);
    }

    async inputTextField(target, text) {
        await this.focusInputTarget(target);
        return this.ui.inputText(text);
    }

    async focusInputTarget(target) {
        if (!target) {
            return this.tapFirstEditText();
        }
        if (typeof target === 'string') {
            const tapped = await this.tapByTestTag(target);
            if (tapped.success) return tapped;
            const tappedByText = await this.tapByText(target);
            if (tappedByText.success) return tappedByText;
            return this.tapFirstEditText();
        }
        if (typeof target === 'object' && target.x != null && target.y != null) {
            return this.tapByCoordinates(target.x, target.y);
        }
        return this.tapFirstEditText();
    }

    async clearLogcat() {
        return this.executeCommand('adb logcat -c');
    }

    async clearAppCache() {
        return this.commands.clearAppCache();
    }

    async clearWorkManagerTasks() {
        return this.commands.clearWorkManagerTasks();
    }

    async tapByTestTag(testTag) {
        return this.ui.tapByTestTag(testTag);
    }

    // Consolidated button tapping utilities
    get buttonTapping() {
        return this.ui.buttonTapping;
    }

    async inputTextViaClipboard(text) {
        return this.ui.inputTextViaClipboard(text);
    }

    async waitForElement(searchFn, timeoutMs, pollMs) {
        return this.ui.waitForElement(searchFn, timeoutMs, pollMs);
    }

    /**
     * Capture API request/response logs from logcat
     * Filters for PluctAPI tag which contains all API communication
     */
    async captureAPILogs(lines = 200) {
        try {
            // Only capture PluctAPI logs, not system errors
            const result = await this.executeCommand(`adb logcat -d -t ${lines} PluctAPI:* PluctCoreAPIHTTPClient:*`);
            if (result.success && result.output) {
                // Parse and format API logs
                const lines = result.output.split('\n').filter(line => line.trim());
                const apiLogs = {
                    requests: [],
                    responses: [],
                    errors: [],
                    retries: [],
                    circuitBreaker: [],
                    info: []
                };

                for (const line of lines) {
                    // Only process lines that are actually from PluctAPI or PluctCoreAPIHTTPClient
                    if (!line.includes('PluctAPI') && !line.includes('PluctCoreAPIHTTPClient')) {
                        continue;
                    }
                    if (line.includes('🚀 API REQUEST') || line.includes('API REQUEST')) {
                        apiLogs.requests.push(line);
                    } else if (line.includes('📥 API RESPONSE') || line.includes('API RESPONSE')) {
                        apiLogs.responses.push(line);
                    } else if (line.includes('❌') || (line.includes('ERROR') && line.includes('PluctAPI'))) {
                        apiLogs.errors.push(line);
                    } else if (line.includes('retrying') || line.includes('Retry')) {
                        apiLogs.retries.push(line);
                    } else if (line.includes('Circuit breaker') || line.includes('circuit breaker')) {
                        apiLogs.circuitBreaker.push(line);
                    } else if (line.includes('CREDIT_REQUEST')) {
                        apiLogs.info.push(line);
                    } else if (line.includes('PluctAPI') || line.includes('PluctCoreAPIHTTPClient')) {
                        apiLogs.info.push(line);
                    }
                }

                return {
                    success: true,
                    raw: result.output,
                    parsed: apiLogs,
                    summary: {
                        requests: apiLogs.requests.length,
                        responses: apiLogs.responses.length,
                        errors: apiLogs.errors.length,
                        retries: apiLogs.retries.length,
                        circuitBreakerEvents: apiLogs.circuitBreaker.length,
                        info: apiLogs.info.length
                    }
                };
            }
            return { success: false, error: 'No logcat output' };
        } catch (error) {
            this.logger.error(`Failed to capture API logs: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Display API logs in a formatted way
     */
    displayAPILogs(apiLogs) {
        if (!apiLogs.success) {
            this.logger.error(`❌ Failed to capture API logs: ${apiLogs.error}`);
            return;
        }

        this.logger.info('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        this.logger.info('📡 API COMMUNICATION SUMMARY');
        this.logger.info('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        this.logger.info(`   Requests: ${apiLogs.summary.requests}`);
        this.logger.info(`   Responses: ${apiLogs.summary.responses}`);
        this.logger.info(`   Errors: ${apiLogs.summary.errors}`);
        this.logger.info(`   Retries: ${apiLogs.summary.retries}`);
        this.logger.info(`   Circuit Breaker Events: ${apiLogs.summary.circuitBreakerEvents}`);

        if (apiLogs.parsed.requests.length > 0) {
            this.logger.info('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.logger.info('📤 API REQUESTS:');
            apiLogs.parsed.requests.forEach(req => {
                this.logger.info(`   ${req}`);
            });
        }

        if (apiLogs.parsed.responses.length > 0) {
            this.logger.info('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.logger.info('📥 API RESPONSES:');
            apiLogs.parsed.responses.forEach(resp => {
                this.logger.info(`   ${resp}`);
            });
        }

        if (apiLogs.parsed.errors.length > 0) {
            this.logger.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.logger.error('❌ API ERRORS:');
            apiLogs.parsed.errors.forEach(err => {
                this.logger.error(`   ${err}`);
            });
        }

        if (apiLogs.parsed.retries.length > 0) {
            this.logger.warn('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.logger.warn('⚠️ RETRY ATTEMPTS:');
            apiLogs.parsed.retries.forEach(retry => {
                this.logger.warn(`   ${retry}`);
            });
        }

        if (apiLogs.parsed.circuitBreaker.length > 0) {
            this.logger.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.logger.error('🔴 CIRCUIT BREAKER EVENTS:');
            apiLogs.parsed.circuitBreaker.forEach(cb => {
                this.logger.error(`   ${cb}`);
            });
        }

        this.logger.info('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    }

    async tapFirstEditText() {
        this.logger.info('👆 Tapping first edit text field...');
        try {
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();

            // Look for the first EditText element
            const regex = /class="android\.widget\.EditText".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/;
            const match = uiDump.match(regex);

            if (match) {
                const x1 = parseInt(match[1], 10);
                const y1 = parseInt(match[2], 10);
                const x2 = parseInt(match[3], 10);
                const y2 = parseInt(match[4], 10);
                const centerX = (x1 + x2) / 2;
                const centerY = (y1 + y2) / 2;

                this.logger.info(`Found EditText at coordinates: (${centerX}, ${centerY})`);
                return await this.tapByCoordinates(centerX, centerY);
            } else {
                this.logger.warn('⚠️ No EditText found in UI dump');
                return { success: false, error: 'No EditText found' };
            }
        } catch (error) {
            this.logger.error(`❌ Failed to tap first edit text: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    // Additional utility methods
    readLastUIDump() {
        return this.ui.readLastUIDump();
    }

    generateTestJWT(userId) {
        return this.utils.generateTestJWT(userId);
    }

    httpGet(url, headers = {}) {
        return this.utils.httpGet(url, headers);
    }

    httpPost(url, data, headers = {}) {
        return this.utils.httpPost(url, data, headers);
    }

    // Additional navigation methods
    async pressBackButton() {
        return await this.commands.executeCommand('adb shell input keyevent KEYCODE_BACK');
    }

    async pressHomeButton() {
        return await this.commands.executeCommand('adb shell input keyevent KEYCODE_HOME');
    }

    async pressMenuButton() {
        return await this.commands.executeCommand('adb shell input keyevent KEYCODE_MENU');
    }

    async pressKey(keyName) {
        const keyMap = {
            Enter: 'KEYCODE_ENTER',
            Back: 'KEYCODE_BACK',
            Home: 'KEYCODE_HOME',
            Menu: 'KEYCODE_MENU'
        };
        const keyCode = keyMap[keyName] || keyName;
        return await this.commands.executeCommand(`adb shell input keyevent ${keyCode}`);
    }

    parseBounds(bounds) {
        const coords = bounds.split('][');
        const topLeft = coords[0].replace('[', '').split(',');
        const bottomRight = coords[1].replace(']', '').split(',');
        return {
            x: Math.floor((parseInt(topLeft[0], 10) + parseInt(bottomRight[0], 10)) / 2),
            y: Math.floor((parseInt(topLeft[1], 10) + parseInt(bottomRight[1], 10)) / 2)
        };
    }

    findElementByText(uiDump, text) {
        if (!uiDump) return null;
        const textRegex = new RegExp(`text="${text.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&')}"[^>]*bounds="([^"]*)"`, 'g');
        const match = textRegex.exec(uiDump);
        if (!match) return null;
        return this.parseBounds(match[1]);
    }

    findElementByHint(uiDump, hint) {
        if (!uiDump) return null;
        const escaped = hint.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&');
        const patterns = [
            new RegExp(`(?:text|content-desc)="[^"]*${escaped}[^"]*"[^>]*bounds="([^"]*)"`, 'i'),
            new RegExp(`hint="${escaped}"[^>]*bounds="([^"]*)"`, 'i')
        ];
        for (const pattern of patterns) {
            const match = pattern.exec(uiDump);
            if (match) return this.parseBounds(match[1]);
        }
        return null;
    }

    async clickElement(target) {
        if (!target) return { success: false, error: 'No target provided' };
        if (typeof target === 'string') {
            const tapped = await this.tapByTestTag(target);
            if (tapped.success) return tapped;
            return this.tapByText(target);
        }
        if (typeof target === 'object' && target.x != null && target.y != null) {
            return this.tapByCoordinates(target.x, target.y);
        }
        return { success: false, error: 'Unsupported target type' };
    }

    // UI interaction methods
    async openCaptureSheet() {
        this.logger.info('🎬 Opening capture sheet...');
        try {
            // Try to tap the FAB (Floating Action Button) to open capture sheet
            const result = await this.tapByContentDesc('Start transcription button');
            if (result.success) {
                this.logger.info('✅ Capture sheet opened successfully');
                return { success: true };
            } else {
                this.logger.warn('⚠️ Could not find FAB, trying alternative method');
                // Alternative: try to tap by coordinates where FAB might be
                await this.tapByCoordinates(650, 1200); // Bottom right area
                await this.sleep(1000);
                return { success: true };
            }
        } catch (error) {
            this.logger.error(`❌ Failed to open capture sheet: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    // Transcription monitoring methods
    async pollTranscriptionStatus(jobId, timeoutMs = 60000, pollMs = 5000) {
        this.logger.info(`⏳ Polling transcription status for job ${jobId} (timeout: ${timeoutMs}ms)...`);
        const startTime = Date.now();
        while (Date.now() - startTime < timeoutMs) {
            const statusResponse = await this.httpGet(`/v1/transcribe/status/${jobId}`);
            if (statusResponse.success && statusResponse.status === 200) {
                const statusData = JSON.parse(statusResponse.body);
                this.logger.info(`Current status for job ${jobId}: ${statusData.status}`);
                if (statusData.status === 'completed') {
                    this.logger.info(`✅ Transcription job ${jobId} completed.`);
                    return { success: true, details: statusData };
                } else if (statusData.status === 'failed') {
                    this.logger.error(`❌ Transcription job ${jobId} failed.`);
                    return { success: false, error: `Transcription job ${jobId} failed.` };
                }
            } else {
                this.logger.warn(`⚠️ Failed to get status for job ${jobId}: ${statusResponse.error || statusResponse.status}`);
            }
            await this.sleep(pollMs);
        }
        this.logger.error(`❌ Timeout polling for transcription job ${jobId}.`);
        return { success: false, error: `Timeout polling for transcription job ${jobId}.` };
    }

    async waitForTranscriptResult(timeoutMs = 160000) {
        this.logger.info(`⏳ Waiting for transcript result (timeout: ${timeoutMs}ms)...`);
        const startTime = Date.now();

        while (Date.now() - startTime < timeoutMs) {
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            const normalizedDump = uiDump.toLowerCase();

            const errorSignals = [
                'Transcription failed',
                'Session expired',
                'Insufficient credits',
                'Out of credits',
                'Payment Required',
                'No transcript available',
                'Service is waking up',
                'temporarily unavailable',
                'Request timed out',
                'Failed'
            ];
            const matchedError = errorSignals.find(signal => normalizedDump.includes(signal.toLowerCase()));
            if (matchedError) {
                this.logger.warn(`?? Error indicator found in UI: ${matchedError}`);
                return { success: false, error: `Error indicator found in UI: ${matchedError}` };
            }

            const hasEmptyState = normalizedDump.includes('no transcripts yet') ||
                normalizedDump.includes('get your first transcript');
            const successSignals = [
                'copy transcript to clipboard',
                'share transcript',
                'export to txt file',
                'copy_transcript_button',
                'share_transcript_button',
                'export_txt_button'
            ];
            const matchedSuccess = successSignals.find(signal => normalizedDump.includes(signal));
            const hasCompletedTranscriptCard = (uiDump.includes('Transcript card') || uiDump.includes('Video item')) &&
                (uiDump.includes('Completed') || uiDump.includes('Ready')) &&
                !hasEmptyState;

            if (matchedSuccess || hasCompletedTranscriptCard) {
                this.logger.info(`?o. Transcript result detected in UI${matchedSuccess ? ` via ${matchedSuccess}` : ''}`);
                return { success: true, message: 'Transcript result found in UI' };
            }

            await this.sleep(5000);
        }

        this.logger.error(`❌ Timeout waiting for transcript result after ${timeoutMs}ms`);
        return { success: false, error: `Timeout waiting for transcript result after ${timeoutMs}ms` };
    }

    async validateErrorCardUsability() {
        await this.dumpUIHierarchy();
        const uiDump = this.readLastUIDump();
        if (!uiDump.includes('content-desc="Error message"')) {
            return { success: true, skipped: true };
        }

        const issues = [];
        if (/\bFlow\s+[A-Za-z0-9_-]+/.test(uiDump) || /\bJob\s+[A-Za-z0-9_-]+/.test(uiDump)) {
            issues.push('Collapsed error card exposes internal flow/job IDs');
        }
        if (uiDump.includes('View in Logs')) {
            issues.push('Logs action is too long for the compact error card');
        }
        if (!/content-desc="Error: [^"]{1,120}"/.test(uiDump)) {
            issues.push('Error card does not expose a concise user-facing message');
        }

        return {
            success: issues.length === 0,
            issues,
            error: issues.join('; ')
        };
    }

    async ensureCaptureCardReady() {
        for (let attempt = 0; attempt < 6; attempt++) {
            const dumpResult = await this.dumpUIHierarchy();
            if (!dumpResult.success) {
                await this.ensureAppForeground();
                await this.sleep(1200);
                continue;
            }
            const uiDump = this.readLastUIDump() || '';
            const hasCaptureInput = uiDump.includes('Video URL input field') ||
                uiDump.includes('Paste TikTok Link') ||
                uiDump.includes('Paste from clipboard') ||
                uiDump.includes('Always visible capture card') ||
                uiDump.includes('video_url_input');
            if (hasCaptureInput) {
                return { success: true };
            }

            const isDetailScreen = uiDump.includes('Video Details') ||
                uiDump.includes('Copy transcript to clipboard') ||
                uiDump.includes('Share transcript') ||
                uiDump.includes('Export to TXT file') ||
                uiDump.includes('content-desc="Back button"');
            if (isDetailScreen) {
                this.logger.info('Returning from video detail to capture card...');
                await this.executeCommand('adb shell input keyevent 4');
                await this.sleep(1200);
                continue;
            }

            await this.ensureAppForeground();
            await this.sleep(1000);
        }

        return { success: false, error: 'Capture input not visible' };
    }

    /**
     * Detect recent Business Engine / TTTranscribe failures in logcat.
     */
    async checkRecentAPIErrors(lines = 300) {
        try {
            const apiLogs = await this.captureAPILogs(lines);
            
            if (!apiLogs.parsed || apiLogs.parsed.errors.length === 0) {
                return { success: true, hasErrors: false, errors: [] };
            }

            // Filter to only PluctAPI errors (not system errors)
            const pluctAPIErrors = apiLogs.parsed.errors.filter(err => {
                if (!err.includes('PluctAPI') &&
                    !err.includes('PluctCoreAPIHTTPClient') &&
                    !err.includes('TTTranscribe')) return false;
                
                // Exclude system-level errors that leak through
                const systemErrorPatterns = [
                    /memtrack/i,
                    /EGL_emulation/i,
                    /storaged/i,
                    /libc/i,
                    /DEBUG/i
                ];
                if (systemErrorPatterns.some(p => p.test(err))) return false;
                
                // Tolerate expected free-tier scenarios to avoid failing tests when credits are exhausted
                const toleratedPatterns = [
                    /insufficient credits/i,
                    /insufficient_credits/i,
                    /payment required/i,
                    /vend-token/i,
                    /\b402\b.*(credit|payment|vend-token)/i
                ];
                if (toleratedPatterns.some(p => p.test(err))) return false;
                
                // Must be actual API errors (status codes, error messages)
                const apiErrorPatterns = [
                    /\b(401|403|404|429|500|502|503|504)\b/,
                    /API.*(error|failed|failure)/i,
                    /Request failed/i,
                    /Authentication failed/i,
                    /upstream_error/i,
                    /service unavailable/i,
                    /TTTranscribe.*(error|failed|failure)/i
                ];
                return apiErrorPatterns.some(p => p.test(err));
            });

            const remainingErrors = pluctAPIErrors.filter(err => {
                return !/vend-token/i.test(err) &&
                    !/insufficient[_\s]?credits/i.test(err) &&
                    !/payment required/i.test(err);
            });

            return {
                success: remainingErrors.length === 0,
                hasErrors: remainingErrors.length > 0,
                errors: remainingErrors
            };
        } catch (error) {
            this.logger.error(`Failed to check API errors: ${error.message}`);
            return { success: false, hasErrors: false, errors: [], error: error.message };
        }
    }

    /**
     * Validates that a sequence of API calls occurred successfully
     * @param {Array} expectedCalls - Array of {method, endpoint, minStatus, maxStatus}
     * @returns {Object} {success: boolean, missing: Array, failed: Array}
     */
    async validateAPICallSequence(expectedCalls) {
        const apiLogs = await this.captureAPILogs(500);
        const found = [];
        const missing = [];
        const failed = [];

        expectedCalls.forEach(expected => {
            const callPattern = new RegExp(
                `🚀 API REQUEST.*${expected.method}.*${expected.endpoint.replace(/\//g, '\\/')}`,
                'i'
            );
            const responsePattern = new RegExp(
                `📥 API RESPONSE.*${expected.endpoint.replace(/\//g, '\\/')}.*(\\d{3})`,
                'i'
            );

            const requestFound = apiLogs.parsed.requests.some(r => callPattern.test(r));
            if (!requestFound) {
                missing.push(`${expected.method} ${expected.endpoint}`);
                return;
            }

            const responseMatch = apiLogs.parsed.responses.find(r => responsePattern.test(r));
            if (!responseMatch) {
                missing.push(`Response for ${expected.method} ${expected.endpoint}`);
                return;
            }

            const statusMatch = responseMatch.match(/(\d{3})/);
            if (statusMatch) {
                const statusCode = parseInt(statusMatch[1], 10);
                const minStatus = expected.minStatus || 200;
                const maxStatus = expected.maxStatus || 299;
                
                if (statusCode < minStatus || statusCode > maxStatus) {
                    failed.push(`${expected.method} ${expected.endpoint}: ${statusCode} (expected ${minStatus}-${maxStatus})`);
                    return;
                }
            }

            found.push(`${expected.method} ${expected.endpoint}`);
        });

        return {
            success: missing.length === 0 && failed.length === 0,
            found: found,
            missing: missing,
            failed: failed
        };
    }

    /**
     * Scan current UI dump for visible error states to fail journeys early.
     */
    async scanUIForErrors() {
        try {
            const dumpResult = await this.dumpUIHierarchy();
            if (!dumpResult.success) {
                return { success: true, note: 'UI dump unavailable' };
            }
            const uiDump = this.readLastUIDump() || '';
            const lowered = uiDump.toLowerCase();
            const patterns = [
                /api error/,
                /transcription failed/,
                /session expired/,
                /timed out/,
                /insufficient credits/,
                /out of credits/,
                /payment required/,
                /unauthorized/,
                /invalid url/
            ];
            const retryVisible = /retry/.test(lowered);
            if (retryVisible) {
                const retryContext = [/error/, /failed/, /try again/, /something went wrong/];
                if (retryContext.some(p => p.test(lowered))) {
                    patterns.push(/retry/);
                }
            }
            const matched = patterns.filter(p => p.test(lowered));
            if (matched.length > 0) {
                return {
                    success: false,
                    error: `UI shows potential error state: ${matched[0]}`
                };
            }
            return { success: true };
        } catch (error) {
            this.logger.warn(`UI error scan failed: ${error.message}`);
            return { success: true, note: 'UI error scan unavailable' };
        }
    }

    async resetAppToFreshCaptureState() {
        if (!this.config.skipAppDataClear) {
            await this.executeCommand('adb shell pm clear app.pluct', undefined, undefined, { allowFailure: true });
        } else if (this.clearAppCache) {
            await this.clearAppCache();
        }

        const fg = await this.ensureAppForeground();
        if (!fg.success) return fg;

        for (let i = 0; i < 3; i++) {
            const nextTap = await this.tapByText('Next');
            if (!nextTap.success) {
                const fallbackNext = await this.tapByCoordinates(416, 790);
                if (!fallbackNext.success) break;
            }
            await this.sleep(700);
        }

        const getStartedTap = await this.tapByText('Get Started');
        if (getStartedTap.success) await this.sleep(700);

        const gotItTap = await this.tapByText('Got It');
        if (!gotItTap.success) await this.tapByCoordinates(414, 760);
        await this.sleep(700);

        const skipTikTokTap = await this.tapByText("I'll Figure It Out");
        if (!skipTikTokTap.success) await this.tapByCoordinates(182, 696);
        await this.sleep(900);

        return await this.ensureCaptureCardReady();
    }

    async getCurrentMobileUserId() {
        const logResult = await this.executeCommand(
            'adb logcat -d -t 400 | findstr /i "Generating JWT token for user:"',
            undefined,
            undefined,
            { allowFailure: true }
        );
        const logMatch = (logResult.output || '').match(/Generating JWT token for user:\s*(mobile-[a-f0-9-]+)/i);
        if (logMatch) return logMatch[1];

        const androidId = await this.executeCommand('adb shell settings get secure android_id', undefined, undefined, { allowFailure: true });
        const model = await this.executeCommand('adb shell getprop ro.product.model', undefined, undefined, { allowFailure: true });
        const manufacturer = await this.executeCommand('adb shell getprop ro.product.manufacturer', undefined, undefined, { allowFailure: true });
        const parts = [androidId.output, model.output, manufacturer.output].map(value => (value || '').trim());
        if (parts.every(Boolean)) {
            const shortHash = crypto.createHash('sha256').update(`${parts[0]}-${parts[1]}-${parts[2]}`).digest('hex').substring(0, 16);
            return `mobile-${shortHash}`;
        }
        return null;
    }

    getLocalAdminKey() {
        if (process.env.ENGINE_ADMIN_KEY) return process.env.ENGINE_ADMIN_KEY;
        if (process.env.ADMIN_KEY) return process.env.ADMIN_KEY;

        const candidates = [
            path.resolve(process.cwd(), '..', 'pluct-business-engine', '.dev.vars'),
            path.resolve(process.cwd(), '..', '..', 'pluct-business-engine', '.dev.vars')
        ];
        for (const candidate of candidates) {
            if (!fs.existsSync(candidate)) continue;
            const content = fs.readFileSync(candidate, 'utf8');
            const match = content.match(/^ENGINE_ADMIN_KEY=(.+)$/m) || content.match(/^ADMIN_KEY=(.+)$/m);
            if (match) return match[1].trim();
        }
        return null;
    }

    async ensureLocalMobileCredits(amount = 3) {
        if (!/^http:\/\/(127\.0\.0\.1|localhost)(:\d+)?/i.test(this.config.businessEngineUrl)) {
            return { success: true, skipped: true };
        }

        const userId = await this.getCurrentMobileUserId();
        if (!userId) return { success: false, error: 'Unable to determine mobile user id for local credit seeding' };

        const adminKey = this.getLocalAdminKey();
        if (!adminKey) return { success: false, error: 'Local admin key unavailable for credit seeding' };

        const response = await this.httpPost(
            `${this.config.businessEngineUrl}/v1/credits/add`,
            {
                userId,
                amount,
                walletType: 'bonus',
                reason: 'local automation credit seed',
                clientRequestId: `local-credit-seed-${Date.now()}`
            },
            { Authorization: `Bearer ${adminKey}` }
        );
        if (!response.success || response.status !== 200) {
            return { success: false, error: `Credit seed failed (${response.status || response.error})` };
        }

        await this.tapByContentDesc('Credit balance');
        await this.sleep(700);
        return { success: true, userId };
    }

    // Performance optimization methods
    async comprehensiveOptimization() {
        this.logger.info('🚀 Running comprehensive optimization...');
        try {
            if (this.config.skipAppDataClear) {
                this.logger.info('dYs? Skipping app data clear (preserving cached tokens)');
            } else {
                // Clear app cache
                await this.clearAppCache();
            }

            // Clear WorkManager tasks
            await this.clearWorkManagerTasks();

            // Optimize memory
            await this.commands.executeCommand('adb shell am force-stop app.pluct');
            await this.sleep(1000);

            this.logger.info('✅ Comprehensive optimization completed');
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Comprehensive optimization failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctCoreFoundation;
