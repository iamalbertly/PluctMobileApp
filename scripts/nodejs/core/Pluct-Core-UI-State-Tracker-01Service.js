const BaseCore = require('./Pluct-Core-01Foundation.js');
const PluctUIStateCapture = require('./Pluct-Core-UI-State-Tracker-02Capture');
const PluctUIStateAnalysis = require('./Pluct-Core-UI-State-Tracker-03Analysis');
const PluctUIStateChangeDetection = require('./Pluct-Core-UI-State-Tracker-04ChangeDetection');

/**
 * Pluct-Core-UI-State-Tracker-01Service - Main UI state tracking service
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Orchestrates UI state tracking, analysis, and change detection
 */
class PluctUIStateTrackerService extends BaseCore {
    constructor(core) {
        super();
        this.core = core;
        this.uiStateHistory = [];
        this.maxHistorySize = 10;
        this.changeDetectionTimeout = 5000; // 5 seconds
        this.criticalErrorThreshold = 3; // Max retries before critical error
        
        // Initialize specialized modules
        this.capture = new PluctUIStateCapture(core);
        this.analysis = new PluctUIStateAnalysis(core);
        this.changeDetection = new PluctUIStateChangeDetection(core);
    }

    /**
     * Capture current UI state with metadata
     */
    async captureUIState(label = 'unknown') {
        try {
            const state = await this.capture.captureCurrentState(label);
            this.uiStateHistory.push(state);
            
            // Maintain history size
            if (this.uiStateHistory.length > this.maxHistorySize) {
                this.uiStateHistory.shift();
            }
            
            return state;
        } catch (error) {
            this.core.logger.error(`UI state capture failed: ${error.message}`);
            throw error;
        }
    }

    /**
     * Analyze UI state for specific patterns
     */
    analyzeUIState(state, analysisType = 'general') {
        return this.analysis.analyzeState(state, analysisType);
    }

    /**
     * Detect changes between UI states
     */
    detectUIChanges(currentState, previousState) {
        return this.changeDetection.detectChanges(currentState, previousState);
    }

    /**
     * Wait for specific UI state change
     */
    async waitForUIStateChange(expectedChange, timeoutMs = 10000) {
        return this.changeDetection.waitForChange(expectedChange, timeoutMs);
    }

    /**
     * Get UI state history
     */
    getUIStateHistory() {
        return this.uiStateHistory;
    }

    /**
     * Clear UI state history
     */
    clearUIStateHistory() {
        this.uiStateHistory = [];
    }
}

module.exports = PluctUIStateTrackerService;