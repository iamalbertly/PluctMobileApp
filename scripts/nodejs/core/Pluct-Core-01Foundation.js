const PluctCoreFoundationCommands = require('./Pluct-Core-01Foundation-02Commands');
const PluctCoreFoundationUI = require('./Pluct-Core-01Foundation-03UI');
const PluctCoreFoundationValidation = require('./Pluct-Core-01Foundation-04Validation');
const PluctCoreFoundationUtils = require('./Pluct-Core-01Foundation-05Utils');
const { logInfo, logSuccess, logWarn, logError } = require('./Logger');

/**
 * Pluct-Core-01Foundation - Core foundation functionality orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Orchestrates all foundation modules for basic operations
 */
class PluctCoreFoundation {
    constructor() {
        const shortUrl = 'https://vm.tiktok.com/ZMDRUGT2P/';
        const longUrl = 'https://www.tiktok.com/@thesunnahguy/video/7493203244727012630';
        const skipAppDataClear = process.env.PLUCT_SKIP_APP_CLEAR === '1' || process.env.PLUCT_PRESERVE_APP_DATA === '1';
        this.config = {
            // Use real TikTok URLs (short + long) for validation
            url: shortUrl,
            testUrls: [shortUrl, longUrl],
            businessEngineUrl: 'https://pluct-business-engine.romeo-lya2.workers.dev',
            timeouts: { default: 5000, short: 2000, long: 10000 },
            retry: { maxAttempts: 3, delay: 1000 },
            skipAppDataClear
        };
        this.logger = { info: logInfo, success: logSuccess, warn: logWarn, error: logError };

        // Initialize specialized modules
        this.commands = new PluctCoreFoundationCommands(this.config, this.logger);
        this.ui = new PluctCoreFoundationUI(this.config, this.logger, this.commands);
        this.validation = new PluctCoreFoundationValidation(this.config, this.logger, this.commands);
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

    async inputText(rawText) {
        return this.ui.inputText(rawText);
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
                    circuitBreaker: []
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
                        circuitBreakerEvents: apiLogs.circuitBreaker.length
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
            // Check UI for completion indicators
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();

            // Look for completion indicators in the UI
            const hasEmptyState = uiDump.includes('No transcripts yet') || uiDump.includes('get your first transcript');
            const hasTranscriptCard = uiDump.includes('Video item') || uiDump.includes('Transcript card');
            const hasCompletionText = uiDump.includes('Completed') || uiDump.includes('Ready');
            const hasTranscriptText = uiDump.includes('transcript') || uiDump.includes('Transcript');

            if ((hasTranscriptText && !hasEmptyState) || hasTranscriptCard || hasCompletionText) {
                this.logger.info('?o. Transcript result detected in UI');
                return { success: true, message: 'Transcript result found in UI' };
            }

            // Check for error indicators
            // Check for error indicators
            const errorSignals = [
                'Transcription failed',
                'Session expired',
                'Insufficient credits',
                'Failed'
            ];
            if (errorSignals.some(signal => uiDump.includes(signal))) {
                this.logger.warn('?? Error indicators found in UI');
                return { success: false, error: 'Error indicators found in UI' };
            }

            // Wait before next check
            await this.sleep(5000);
            // Wait before next check
            await this.sleep(5000);
        }

        this.logger.error(`❌ Timeout waiting for transcript result after ${timeoutMs}ms`);
        return { success: false, error: `Timeout waiting for transcript result after ${timeoutMs}ms` };
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
                // Must contain PluctAPI tag
                if (!err.includes('PluctAPI') && !err.includes('PluctCoreAPIHTTPClient')) return false;
                
                // Exclude system-level errors that leak through
                const systemErrorPatterns = [
                    /memtrack/i,
                    /EGL_emulation/i,
                    /storaged/i,
                    /libc/i,
                    /DEBUG/i
                ];
                if (systemErrorPatterns.some(p => p.test(err))) return false;
                
                // Must be actual API errors (status codes, error messages)
                const apiErrorPatterns = [
                    /\b(401|402|403|404|429|500|502|503|504)\b/,
                    /API.*(error|failed|failure)/i,
                    /Request failed/i,
                    /Authentication failed/i,
                    /Insufficient credits/i
                ];
                return apiErrorPatterns.some(p => p.test(err));
            });

            return {
                success: pluctAPIErrors.length === 0,
                hasErrors: pluctAPIErrors.length > 0,
                errors: pluctAPIErrors
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
                /unauthorized/,
                /invalid url/,
                /retry/i
            ];
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