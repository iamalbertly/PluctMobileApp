package app.pluct.ui.utils.scripts

/**
 * AJAX monitoring logic for TokAudit automation
 */
object AjaxMonitor {
    
    fun getAjaxMonitoringScript(): String {
        return """
            // Set up AJAX monitoring
            function setupAjaxMonitoring() {
                try {
                    log('setting_up_ajax_monitoring');
                    
                    // Monitor fetch requests
                    const originalFetch = window.fetch;
                    window.fetch = function(...args) {
                        const url = args[0] || 'unknown';
                        inflightCount++;
                        log('net+1 url=' + url);
                        
                        const startTime = Date.now();
                        return originalFetch.apply(this, args)
                            .then(response => {
                                inflightCount--;
                                const duration = Date.now() - startTime;
                                log('net-1 url=' + url + ' status=' + response.status + ' ms=' + duration);
                                return response;
                            })
                            .catch(error => {
                                inflightCount--;
                                const duration = Date.now() - startTime;
                                log('net-1 url=' + url + ' error ms=' + duration);
                                throw error;
                            });
                    };
                    
                    // Monitor XMLHttpRequest
                    const originalXHROpen = XMLHttpRequest.prototype.open;
                    const originalXHRSend = XMLHttpRequest.prototype.send;
                    
                    XMLHttpRequest.prototype.open = function(method, url, ...args) {
                        this._url = url;
                        return originalXHROpen.apply(this, [method, url, ...args]);
                    };
                    
                    XMLHttpRequest.prototype.send = function(...args) {
                        const xhr = this;
                        const url = xhr._url || 'unknown';
                        inflightCount++;
                        log('net+1 url=' + url);
                        
                        const startTime = Date.now();
                        const originalOnReadyStateChange = xhr.onreadystatechange;
                        
                        xhr.onreadystatechange = function() {
                            if (originalOnReadyStateChange) {
                                originalOnReadyStateChange.apply(this, arguments);
                            }
                            
                            if (xhr.readyState === 4) {
                                inflightCount--;
                                const duration = Date.now() - startTime;
                                log('net-1 url=' + url + ' status=' + xhr.status + ' ms=' + duration);
                                
                                // Check for network idle
                                if (inflightCount === 0) {
                                    log('network_idle');
                                }
                            }
                        };
                        
                        return originalXHRSend.apply(this, args);
                    };
                    
                    log('ajax_monitoring_setup_complete');
                    
                } catch (e) {
                    log('error_in_setupAjaxMonitoring: ' + e.message);
                }
            }
        """.trimIndent()
    }
}
