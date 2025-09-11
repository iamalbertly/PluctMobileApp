package app.pluct.ui.utils.scripts

import org.json.JSONObject

/**
 * Core transcript automation logic - extracted from AutomationScriptBuilder
 */
object TranscriptAutomation {
    
    fun buildCoreScript(targetUrl: String, runId: String): String {
        val jsonUrl = JSONObject().put("url", targetUrl).toString()
        
        return """
            (()=>{"use strict";
                const RUN_ID = "${runId}";
                const TARGET_URL = ${jsonUrl};
                const startTs = Date.now();
                let transcriptSent = false;
                let inflightCount = 0;
                
                // Global deduplication - prevent any transcript processing if already sent
                if (window.transcriptAlreadyProcessed) {
                    log('script_already_processed_globally');
                    return;
                }
                
                function log(x){ try{ console.log("WV:J:"+x+" run="+RUN_ID); }catch(e){} }
                
                if(!TARGET_URL||!TARGET_URL.url||!TARGET_URL.url.trim()){ 
                    log("fatal_blank_url"); 
                    try{Android.onError("blank_url")}catch(e){}; 
                    return; 
                }
                log("url="+TARGET_URL.url);
                
                // Start automation immediately - no waiting
                function startAutomation() {
                    try {
                        log('starting_automation');
                        
                        // Check if we're on the right page
                        if (!location.hostname.endsWith('tokaudit.io')) {
                            log('not_on_tokaudit_page_current_url=' + location.href);
                            if (window.Android && window.Android.onError) {
                                try {
                                    window.Android.onError('Not on transcript service page. Current URL: ' + location.href);
                                } catch (e) {
                                    log('error_calling_onError: ' + e.message);
                                }
                            }
                            return;
                        }
                        
                        log('phase=page_ready');
                        
                        // Close modals first
                        dismissModals();
                        
                        // Set up AJAX monitoring
                        setupAjaxMonitoring();
                        
                        // Fill and submit URL
                        fillAndSubmitUrl();
                        
                        // Start monitoring for completion
                        setTimeout(monitorTranscriptCompletion, 3000);
                        
                    } catch (e) {
                        log('error_in_startAutomation: ' + e.message);
                        if (window.Android && window.Android.onError) {
                            try {
                                window.Android.onError('Automation failed: ' + e.message);
                            } catch (err) {
                                log('error_calling_onError: ' + err.message);
                            }
                        }
                    }
                }
                
                // Start the automation immediately
                startAutomation();
                
            })();
        """.trimIndent()
    }
}
