/**
 * Pluct-Core-Unified-Clean - Consolidated core functionality
 * Single source of truth for all core operations
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctCoreFoundation = require('./Pluct-Core-01Foundation');
const PluctCoreUI = require('./Pluct-Core-02UI');
const PluctCorePerformance = require('./Pluct-Core-03Performance');

class PluctCoreUnified {
    constructor() {
        this.foundation = new PluctCoreFoundation();
        this.ui = new PluctCoreUI(this.foundation);
        this.performance = new PluctCorePerformance(this.foundation);
        
        // Expose foundation properties directly
        this.config = this.foundation.config;
        this.logger = this.foundation.logger;
    }

    // Foundation methods
    async executeCommand(command, timeout) {
        return await this.foundation.executeCommand(command, timeout);
    }

    async sleep(ms) {
        return await this.foundation.sleep(ms);
    }

    async validateEnvironment() {
        return await this.foundation.validateEnvironment();
    }

    async launchApp() {
        return await this.foundation.launchApp();
    }

    async ensureAppForeground() {
        return await this.foundation.ensureAppForeground();
    }

    async captureUIArtifacts(tag) {
        return await this.foundation.captureUIArtifacts(tag);
    }

    async dumpUIHierarchy() {
        return await this.foundation.dumpUIHierarchy();
    }

    readLastUIDump() {
        return this.foundation.readLastUIDump();
    }

    async waitForText(text, timeoutMs, pollMs) {
        return await this.foundation.waitForText(text, timeoutMs, pollMs);
    }

    findBoundsForText(targetText) {
        return this.foundation.findBoundsForText(targetText);
    }

    async tapByText(text) {
        return await this.foundation.tapByText(text);
    }

    async tapByContentDesc(contentDesc) {
        return await this.foundation.tapByContentDesc(contentDesc);
    }

    async inputText(rawText) {
        return await this.foundation.inputText(rawText);
    }

    async clearAppCache() {
        return await this.foundation.clearAppCache();
    }

    async clearWorkManagerTasks() {
        return await this.foundation.clearWorkManagerTasks();
    }

    // UI methods
    async openCaptureSheet() {
        return await this.ui.openCaptureSheet();
    }

    async tapFirstEditText() {
        return await this.ui.tapFirstEditText();
    }

    async clearEditText() {
        return await this.ui.clearEditText();
    }

    async waitForTranscriptResult(timeoutMs, pollMs) {
        return await this.ui.waitForTranscriptResult(timeoutMs, pollMs);
    }

    async validateTikTokUrl(url) {
        return await this.ui.validateTikTokUrl(url);
    }

    async fetchHtmlMetadata(url) {
        return await this.ui.fetchHtmlMetadata(url);
    }

    writeJsonArtifact(filename, data) {
        return this.ui.writeJsonArtifact(filename, data);
    }

    async checkNetworkConnectivity() {
        return await this.ui.checkNetworkConnectivity();
    }

    async httpGet(url) {
        return await this.ui.httpGet(url);
    }


    async httpPost(url, data, headers) {
        return await this.ui.httpPost(url, data, headers);
    }

    async httpPostJson(url, data, headers = {}) {
        const jsonHeaders = { 'Content-Type': 'application/json', ...headers };
        return await this.ui.httpPost(url, data, jsonHeaders);
    }

    async normalizeTikTokUrl(url) {
        return await this.ui.validateTikTokUrl(url);
    }

    async readLogcatSince(timestamp, tag) {
        try {
            const command = `adb logcat -t ${new Date(timestamp).toISOString().replace('T', ' ').replace('Z', '')} | grep "${tag}"`;
            const result = await this.foundation.executeCommand(command);
            return result.success ? result.output.split('\n').filter(line => line.trim()) : [];
        } catch (error) {
            this.logger.warn(`Failed to read logcat: ${error.message}`);
            return [];
        }
    }

    // Performance methods
    trackMetric(name, value) {
        return this.performance.trackMetric(name, value);
    }

    getMetrics() {
        return this.performance.getMetrics();
    }

    clearMetrics() {
        return this.performance.clearMetrics();
    }

    async optimizePerformance() {
        return await this.performance.optimizePerformance();
    }

    async monitorPerformance(operationName, operation) {
        return await this.performance.monitorPerformance(operationName, operation);
    }

    getPerformanceReport() {
        return this.performance.getPerformanceReport();
    }

    async clearAllAppCache() {
        return await this.performance.clearAllAppCache();
    }

    async optimizeMemory() {
        return await this.performance.optimizeMemory();
    }

    async optimizeNetwork() {
        return await this.performance.optimizeNetwork();
    }

    async optimizeBattery() {
        return await this.performance.optimizeBattery();
    }

    async optimizeStorage() {
        return await this.performance.optimizeStorage();
    }

    async comprehensiveOptimization() {
        return await this.performance.comprehensiveOptimization();
    }

    // Foundation methods
    generateTestJWT() {
        return this.foundation.generateTestJWT();
    }
}

module.exports = PluctCoreUnified;
