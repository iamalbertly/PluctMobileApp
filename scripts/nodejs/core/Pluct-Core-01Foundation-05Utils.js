/**
 * Pluct-Core-01Foundation-05Utils - Utility functions module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles utility functions and helper methods
 */
class PluctCoreFoundationUtils {
    constructor(config, logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * Generate test JWT token
     */
    generateTestJWT(userId = 'mobile') {
        try {
            const now = Math.floor(Date.now() / 1000);
            const payload = {
                sub: userId,
                scope: 'ttt:transcribe',
                iat: now,
                exp: now + 900 // 15 minutes
            };
            
            // Simple JWT generation for testing
            const header = { alg: 'HS256', typ: 'JWT' };
            const secret = 'prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e';
            
            const encodedHeader = Buffer.from(JSON.stringify(header)).toString('base64url');
            const encodedPayload = Buffer.from(JSON.stringify(payload)).toString('base64url');
            const signature = this.createSignature(encodedHeader + '.' + encodedPayload, secret);
            
            return `${encodedHeader}.${encodedPayload}.${signature}`;
        } catch (error) {
            this.logger.error(`JWT generation failed: ${error.message}`);
            return null;
        }
    }

    /**
     * Create HMAC signature
     */
    createSignature(data, secret) {
        const crypto = require('crypto');
        const hmac = crypto.createHmac('sha256', secret);
        hmac.update(data);
        return hmac.digest('base64url');
    }

    /**
     * Format timestamp
     */
    formatTimestamp(timestamp = Date.now()) {
        return new Date(timestamp).toISOString();
    }

    /**
     * Generate unique ID
     */
    generateUniqueId() {
        return Date.now().toString(36) + Math.random().toString(36).substr(2);
    }

    /**
     * Validate URL format
     */
    isValidUrl(url) {
        try {
            new URL(url);
            return true;
        } catch {
            return false;
        }
    }

    /**
     * Sanitize text for ADB input
     */
    sanitizeTextForADB(text) {
        return text.replace(/["\\]/g, '\\$&');
    }

    /**
     * Parse JSON safely
     */
    parseJSON(jsonString, defaultValue = null) {
        try {
            return JSON.parse(jsonString);
        } catch (error) {
            this.logger.warn(`JSON parse failed: ${error.message}`);
            return defaultValue;
        }
    }

    /**
     * Retry operation with exponential backoff
     */
    async retryWithBackoff(operation, maxRetries = 3, baseDelay = 1000) {
        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                const result = await operation();
                if (result.success) {
                    return result;
                }
            } catch (error) {
                this.logger.warn(`Attempt ${attempt + 1} failed: ${error.message}`);
            }
            
            if (attempt < maxRetries - 1) {
                const delay = baseDelay * Math.pow(2, attempt);
                await this.sleep(delay);
            }
        }
        
        return { success: false, error: 'Max retries exceeded' };
    }

    /**
     * Check if string contains any of the given patterns
     */
    containsAny(text, patterns) {
        return patterns.some(pattern => text.includes(pattern));
    }

    /**
     * Extract number from string
     */
    extractNumber(text) {
        const match = text.match(/\d+/);
        return match ? parseInt(match[0]) : null;
    }

    /**
     * Truncate string to specified length
     */
    truncateString(str, maxLength = 100) {
        if (str.length <= maxLength) return str;
        return str.substring(0, maxLength - 3) + '...';
    }

    /**
     * HTTP GET request
     */
    async httpGet(url, headers = {}) {
        try {
            const https = require('https');
            const { URL } = require('url');
            
            return new Promise((resolve) => {
                const parsedUrl = new URL(url);
                const options = {
                    hostname: parsedUrl.hostname,
                    port: parsedUrl.port || 443,
                    path: parsedUrl.pathname + parsedUrl.search,
                    method: 'GET',
                    headers: {
                        'User-Agent': 'Pluct-Test-Automation/1.0.0',
                        ...headers
                    }
                };

                const req = https.request(options, (res) => {
                    let data = '';
                    res.on('data', (chunk) => data += chunk);
                    res.on('end', () => {
                        resolve({
                            success: true,
                            status: res.statusCode,
                            body: data,
                            headers: res.headers
                        });
                    });
                });

                req.on('error', (error) => {
                    resolve({ success: false, error: error.message });
                });

                req.setTimeout(30000, () => {
                    req.destroy();
                    resolve({ success: false, error: 'Request timeout' });
                });

                req.end();
            });
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * HTTP POST request
     */
    async httpPost(url, data, headers = {}) {
        try {
            const https = require('https');
            const { URL } = require('url');
            
            const postData = JSON.stringify(data);
            
            return new Promise((resolve) => {
                const parsedUrl = new URL(url);
                const options = {
                    hostname: parsedUrl.hostname,
                    port: parsedUrl.port || 443,
                    path: parsedUrl.pathname + parsedUrl.search,
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Content-Length': Buffer.byteLength(postData),
                        'User-Agent': 'Pluct-Test-Automation/1.0.0',
                        ...headers
                    }
                };

                const req = https.request(options, (res) => {
                    let responseData = '';
                    res.on('data', (chunk) => responseData += chunk);
                    res.on('end', () => {
                        resolve({
                            success: true,
                            status: res.statusCode,
                            body: responseData,
                            headers: res.headers
                        });
                    });
                });

                req.on('error', (error) => {
                    resolve({ success: false, error: error.message });
                });

                req.setTimeout(30000, () => {
                    req.destroy();
                    resolve({ success: false, error: 'Request timeout' });
                });

                req.write(postData);
                req.end();
            });
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctCoreFoundationUtils;
