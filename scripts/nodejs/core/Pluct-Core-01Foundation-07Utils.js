/**
 * Pluct-Core-01Foundation-07Utils - Utility functions
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctCoreFoundationUtils {
    constructor() {
        this.logger = new PluctLogger();
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * Generate unique ID
     */
    generateUniqueId() {
        return `pluct_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Format timestamp
     */
    formatTimestamp(timestamp = Date.now()) {
        return new Date(timestamp).toISOString();
    }

    /**
     * Format duration
     */
    formatDuration(ms) {
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        
        if (hours > 0) {
            return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
        } else if (minutes > 0) {
            return `${minutes}m ${seconds % 60}s`;
        } else {
            return `${seconds}s`;
        }
    }

    /**
     * Validate URL
     */
    isValidUrl(url) {
        try {
            new URL(url);
            return true;
        } catch (error) {
            return false;
        }
    }

    /**
     * Validate TikTok URL
     */
    isValidTikTokUrl(url) {
        if (!this.isValidUrl(url)) {
            return false;
        }
        
        const tiktokPattern = /^https?:\/\/(?:vm\.tiktok\.com|www\.tiktok\.com|tiktok\.com)\/[\w\-]+\/?$/;
        return tiktokPattern.test(url);
    }

    /**
     * Extract TikTok URL from text
     */
    extractTikTokUrlFromText(text) {
        if (!text) return null;
        
        const tiktokPattern = /https?:\/\/(?:vm\.tiktok\.com|www\.tiktok\.com|tiktok\.com)\/[\w\-]+\/?/g;
        const matches = text.match(tiktokPattern);
        return matches ? matches[0] : null;
    }

    /**
     * Sanitize text for logging
     */
    sanitizeForLogging(text, maxLength = 1000) {
        if (!text) return '';
        
        let sanitized = text.toString();
        
        // Remove sensitive information
        sanitized = sanitized.replace(/Bearer\s+[A-Za-z0-9\-._~+/]+=*/g, 'Bearer [REDACTED]');
        sanitized = sanitized.replace(/token["\s]*[:=]["\s]*[A-Za-z0-9\-._~+/]+=*/g, 'token: [REDACTED]');
        sanitized = sanitized.replace(/key["\s]*[:=]["\s]*[A-Za-z0-9\-._~+/]+=*/g, 'key: [REDACTED]');
        
        // Truncate if too long
        if (sanitized.length > maxLength) {
            sanitized = sanitized.substring(0, maxLength) + '...';
        }
        
        return sanitized;
    }

    /**
     * Parse JSON safely
     */
    parseJsonSafely(jsonString, defaultValue = null) {
        try {
            return JSON.parse(jsonString);
        } catch (error) {
            this.logger.warn('⚠️ Failed to parse JSON:', error.message);
            return defaultValue;
        }
    }

    /**
     * Stringify JSON safely
     */
    stringifyJsonSafely(obj, defaultValue = '{}') {
        try {
            return JSON.stringify(obj, null, 2);
        } catch (error) {
            this.logger.warn('⚠️ Failed to stringify JSON:', error.message);
            return defaultValue;
        }
    }

    /**
     * Retry with exponential backoff
     */
    async retryWithBackoff(operation, maxRetries = 3, baseDelay = 1000) {
        let lastError;
        
        for (let attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return await operation();
            } catch (error) {
                lastError = error;
                
                if (attempt < maxRetries) {
                    const delay = baseDelay * Math.pow(2, attempt);
                    this.logger.warn(`⚠️ Operation failed, retrying in ${delay}ms (attempt ${attempt + 1}/${maxRetries + 1})`);
                    await this.sleep(delay);
                }
            }
        }
        
        throw lastError;
    }

    /**
     * Timeout promise
     */
    async withTimeout(promise, timeoutMs, timeoutMessage = 'Operation timed out') {
        const timeoutPromise = new Promise((_, reject) => {
            setTimeout(() => reject(new Error(timeoutMessage)), timeoutMs);
        });
        
        return Promise.race([promise, timeoutPromise]);
    }

    /**
     * Debounce function
     */
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * Throttle function
     */
    throttle(func, limit) {
        let inThrottle;
        return function executedFunction(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }

    /**
     * Deep clone object
     */
    deepClone(obj) {
        if (obj === null || typeof obj !== 'object') {
            return obj;
        }
        
        if (obj instanceof Date) {
            return new Date(obj.getTime());
        }
        
        if (obj instanceof Array) {
            return obj.map(item => this.deepClone(item));
        }
        
        if (typeof obj === 'object') {
            const clonedObj = {};
            for (const key in obj) {
                if (obj.hasOwnProperty(key)) {
                    clonedObj[key] = this.deepClone(obj[key]);
                }
            }
            return clonedObj;
        }
        
        return obj;
    }

    /**
     * Merge objects deeply
     */
    deepMerge(target, source) {
        const result = this.deepClone(target);
        
        for (const key in source) {
            if (source.hasOwnProperty(key)) {
                if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
                    result[key] = this.deepMerge(result[key] || {}, source[key]);
                } else {
                    result[key] = source[key];
                }
            }
        }
        
        return result;
    }

    /**
     * Generate random string
     */
    generateRandomString(length = 8) {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let result = '';
        for (let i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    }

    /**
     * Generate UUID v4
     */
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
}

module.exports = PluctCoreFoundationUtils;