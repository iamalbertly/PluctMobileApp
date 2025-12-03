/**
 * Pluct-Test-Mapper-01FeatureToTest - Map features to their validation tests
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 */
const fs = require('fs');
const path = require('path');

class PluctTestMapper01FeatureToTest {
    constructor() {
        this.featureMapping = null;
        this.loadFeatureMapping();
    }
    
    /**
     * Load feature mapping configuration
     */
    loadFeatureMapping() {
        try {
            const configPath = path.join(__dirname, '../config/Pluct-Test-Config-02FeatureMapping.json');
            const configContent = fs.readFileSync(configPath, 'utf8');
            this.featureMapping = JSON.parse(configContent);
        } catch (error) {
            console.error('Failed to load feature mapping:', error.message);
            this.featureMapping = { features: {}, testGroups: {} };
        }
    }
    
    /**
     * Get tests for a feature
     */
    getTestsForFeature(featureName) {
        if (!this.featureMapping || !this.featureMapping.features[featureName]) {
            return [];
        }
        return this.featureMapping.features[featureName].tests || [];
    }
    
    /**
     * Get tests for multiple features
     */
    getTestsForFeatures(featureNames) {
        const allTests = new Set();
        for (const featureName of featureNames) {
            const tests = this.getTestsForFeature(featureName);
            tests.forEach(test => allTests.add(test));
        }
        return Array.from(allTests);
    }
    
    /**
     * Get tests for a test group
     */
    getTestsForGroup(groupName) {
        if (!this.featureMapping || !this.featureMapping.testGroups[groupName]) {
            return [];
        }
        const features = this.featureMapping.testGroups[groupName];
        return this.getTestsForFeatures(features);
    }
    
    /**
     * Get all available features
     */
    getAllFeatures() {
        if (!this.featureMapping) return [];
        return Object.keys(this.featureMapping.features || {});
    }
    
    /**
     * Get all available test groups
     */
    getAllTestGroups() {
        if (!this.featureMapping) return [];
        return Object.keys(this.featureMapping.testGroups || {});
    }
}

module.exports = PluctTestMapper01FeatureToTest;

