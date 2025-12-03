/**
 * Pluct-Test-Environment-Config - Test environment configuration
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Implements test environment configuration for different API endpoints
 */

class PluctTestEnvironmentConfig {
    constructor() {
        this.environments = {
            development: {
                businessEngine: 'https://dev-business-engine.pluct.app',
                ttTranscribe: 'https://dev-ttt.pluct.app',
                useMockResponses: true,
                timeout: 30000,
                retryAttempts: 3
            },
            staging: {
                businessEngine: 'https://staging-business-engine.pluct.app',
                ttTranscribe: 'https://staging-ttt.pluct.app',
                useMockResponses: false,
                timeout: 60000,
                retryAttempts: 5
            },
            production: {
                businessEngine: 'https://business-engine.pluct.app',
                ttTranscribe: 'https://ttt.pluct.app',
                useMockResponses: false,
                timeout: 120000,
                retryAttempts: 10
            },
            testing: {
                businessEngine: 'mock://business-engine.pluct.app',
                ttTranscribe: 'mock://ttt.pluct.app',
                useMockResponses: true,
                timeout: 5000,
                retryAttempts: 1
            }
        };
        
        this.currentEnvironment = this.detectEnvironment();
    }

    /**
     * Detect current test environment
     */
    detectEnvironment() {
        const nodeEnv = process.env.NODE_ENV || 'development';
        const testEnv = process.env.TEST_ENV || 'testing';
        
        if (testEnv === 'testing' || nodeEnv === 'test') {
            return 'testing';
        } else if (nodeEnv === 'production') {
            return 'production';
        } else if (nodeEnv === 'staging') {
            return 'staging';
        } else {
            return 'development';
        }
    }

    /**
     * Get current environment configuration
     */
    getCurrentConfig() {
        return this.environments[this.currentEnvironment];
    }

    /**
     * Get API endpoints for current environment
     */
    getAPIEndpoints() {
        const config = this.getCurrentConfig();
        return {
            businessEngine: config.businessEngine,
            ttTranscribe: config.ttTranscribe,
            health: '/health',
            vendToken: '/v1/vend-token',
            credits: '/v1/credits/balance',
            transcribe: '/ttt/transcribe'
        };
    }

    /**
     * Check if mock responses should be used
     */
    shouldUseMockResponses() {
        const config = this.getCurrentConfig();
        return config.useMockResponses;
    }

    /**
     * Get timeout configuration
     */
    getTimeoutConfig() {
        const config = this.getCurrentConfig();
        return {
            apiCall: config.timeout,
            uiInteraction: Math.floor(config.timeout / 2),
            transcription: config.timeout * 4,
            appLaunch: Math.floor(config.timeout / 3)
        };
    }

    /**
     * Get retry configuration
     */
    getRetryConfig() {
        const config = this.getCurrentConfig();
        return {
            maxRetries: config.retryAttempts,
            baseDelay: 1000,
            maxDelay: 10000,
            exponentialBackoff: true
        };
    }

    /**
     * Get test data configuration
     */
    getTestDataConfig() {
        return {
            testTikTokUrl: 'https://vm.tiktok.com/ZMAKpqkpN/',
            testVideoTitle: 'Test Video',
            testCreator: 'Test Creator',
            testDuration: 30,
            testLanguage: 'en'
        };
    }

    /**
     * Get logging configuration
     */
    getLoggingConfig() {
        return {
            level: this.currentEnvironment === 'testing' ? 'DEBUG' : 'INFO',
            enablePerformanceLogging: true,
            enableAPILogging: true,
            enableUILogging: true,
            logToFile: this.currentEnvironment !== 'testing'
        };
    }

    /**
     * Get security configuration
     */
    getSecurityConfig() {
        return {
            enableEncryption: this.currentEnvironment !== 'testing',
            enableValidation: true,
            enableRateLimiting: this.currentEnvironment === 'production',
            enableAuditLogging: this.currentEnvironment !== 'testing'
        };
    }

    /**
     * Get monitoring configuration
     */
    getMonitoringConfig() {
        return {
            enableHealthChecks: true,
            enableMetrics: this.currentEnvironment !== 'testing',
            enableAlerts: this.currentEnvironment === 'production',
            enableRealTimeMonitoring: this.currentEnvironment !== 'testing'
        };
    }

    /**
     * Get complete configuration for current environment
     */
    getCompleteConfig() {
        return {
            environment: this.currentEnvironment,
            api: this.getAPIEndpoints(),
            timeouts: this.getTimeoutConfig(),
            retry: this.getRetryConfig(),
            testData: this.getTestDataConfig(),
            logging: this.getLoggingConfig(),
            security: this.getSecurityConfig(),
            monitoring: this.getMonitoringConfig(),
            useMockResponses: this.shouldUseMockResponses()
        };
    }

    /**
     * Set environment manually
     */
    setEnvironment(env) {
        if (this.environments[env]) {
            this.currentEnvironment = env;
            console.log(`🔧 Environment set to: ${env}`);
        } else {
            console.warn(`⚠️ Unknown environment: ${env}, using current: ${this.currentEnvironment}`);
        }
    }

    /**
     * Get environment summary
     */
    getEnvironmentSummary() {
        const config = this.getCurrentConfig();
        return {
            environment: this.currentEnvironment,
            businessEngine: config.businessEngine,
            ttTranscribe: config.ttTranscribe,
            useMockResponses: config.useMockResponses,
            timeout: config.timeout,
            retryAttempts: config.retryAttempts
        };
    }
}

module.exports = PluctTestEnvironmentConfig;
