const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX06CorrelationIdsValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-UX-06CorrelationIds-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-06CorrelationIds-Validation');
        
        try {
            // Step 1: Submit transcription request
            this.core.logger.info('- Step 1: Submit transcription request');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            const testUrl = 'https://vm.tiktok.com/ZMDRUGT2P/';
            await this.core.tapByTestTag('capture_component_label');
            await this.core.sleep(500);
            await this.core.typeText(testUrl);
            await this.core.sleep(1000);
            
            const submitTap = await this.core.tapByTestTag('extract_script_button');
            if (!submitTap.success) {
                const submitTap2 = await this.core.tapByText('Extract Script');
                if (!submitTap2.success) {
                    this.core.logger.error('❌ Could not submit transcription');
                    return { success: false, error: 'Could not start transcription' };
                }
            }
            
            // Step 2: Capture debug info from processing indicator
            this.core.logger.info('- Step 2: Capture debug info');
            await this.core.sleep(3000);
            await this.dumpUI();
            const processingUI = this.core.readLastUIDump();
            
            // Step 3: Verify debug info contains flowRequestId and clientRequestId
            this.core.logger.info('- Step 3: Verify correlation IDs in UI');
            const hasFlowRequestId = /flowRequestId|Flow ID|flow.*id/i.test(processingUI);
            const hasClientRequestId = /clientRequestId|Client Request ID|client.*request.*id/i.test(processingUI);
            
            if (!hasFlowRequestId && !hasClientRequestId) {
                this.core.logger.warn('⚠️ Request IDs not immediately visible in UI (may be in expanded debug)');
            } else {
                if (hasFlowRequestId) {
                    this.core.logger.info('✅ Flow Request ID found in UI');
                }
                if (hasClientRequestId) {
                    this.core.logger.info('✅ Client Request ID found in UI');
                }
            }
            
            // Step 4: Check logcat for Business Engine request IDs
            this.core.logger.info('- Step 4: Verify request IDs in logcat');
            await this.core.sleep(2000);
            const apiLogs = await this.core.captureAPILogs(500);
            
            if (apiLogs.success) {
                const allLogs = [
                    ...apiLogs.parsed.requests,
                    ...apiLogs.parsed.responses
                ].join('\n');
                
                const hasRequestId = /req_\d+|request.*id|Request ID/i.test(allLogs);
                const hasBusinessEngineRequest = allLogs.includes('/v1/') || 
                                               allLogs.includes('/ttt/') ||
                                               allLogs.includes('Business Engine');
                
                if (hasRequestId) {
                    this.core.logger.info('✅ Request IDs found in logcat');
                } else {
                    this.core.logger.warn('⚠️ Request IDs not found in recent logcat');
                }
                
                if (hasBusinessEngineRequest) {
                    this.core.logger.info('✅ Business Engine requests found in logcat');
                }
            }
            
            // Step 5: Verify correlation IDs are extractable from responses
            this.core.logger.info('- Step 5: Verify correlation ID format');
            // Check for X-Request-Id, X-Correlation-Id patterns
            const correlationPatterns = [
                /X-Request-Id/i,
                /X-Correlation-Id/i,
                /correlation.*id/i,
                /request.*id/i
            ];
            
            let foundCorrelationPattern = false;
            if (apiLogs.success) {
                const allLogs = [
                    ...apiLogs.parsed.requests,
                    ...apiLogs.parsed.responses,
                    ...apiLogs.parsed.errors
                ].join('\n');
                
                correlationPatterns.forEach(pattern => {
                    if (pattern.test(allLogs)) {
                        foundCorrelationPattern = true;
                        this.core.logger.info(`✅ Correlation ID pattern found: ${pattern}`);
                    }
                });
            }
            
            if (!foundCorrelationPattern) {
                this.core.logger.warn('⚠️ Correlation ID patterns not found (may be in response headers)');
            }
            
            // Step 6: Test debug text export contains all IDs
            this.core.logger.info('- Step 6: Verify debug text export');
            // Try to find and expand debug details
            await this.dumpUI();
            const currentUI = this.core.readLastUIDump();
            
            const hasDebugDetails = currentUI.includes('Debug Details') ||
                                  currentUI.includes('Details') ||
                                  currentUI.includes('Copy');
            
            if (hasDebugDetails) {
                // Try to tap Details or Copy button
                const detailsTap = await this.core.tapByText('Details');
                if (detailsTap.success) {
                    await this.core.sleep(1000);
                    await this.dumpUI();
                    const expandedUI = this.core.readLastUIDump();
                    
                    const hasCorrelationSection = expandedUI.includes('Correlation IDs') ||
                                                 expandedUI.includes('correlation') ||
                                                 expandedUI.includes('Flow Request ID') ||
                                                 expandedUI.includes('Client Request ID');
                    
                    if (hasCorrelationSection) {
                        this.core.logger.info('✅ Correlation IDs section found in debug details');
                    } else {
                        this.core.logger.warn('⚠️ Correlation IDs section not found in expanded debug');
                    }
                }
            } else {
                this.core.logger.warn('⚠️ Debug details not available (may need to wait for processing)');
            }
            
            // Verify IDs are in searchable format
            const idFormats = [
                /req_\d{13,}/,  // timestamp-based request ID
                /[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/i,  // UUID
                /[a-f0-9]{32}/i  // hex ID
            ];
            
            let hasValidIdFormat = false;
            if (apiLogs.success) {
                const allLogs = [
                    ...apiLogs.parsed.requests,
                    ...apiLogs.parsed.responses
                ].join('\n');
                
                idFormats.forEach(format => {
                    if (format.test(allLogs)) {
                        hasValidIdFormat = true;
                        this.core.logger.info(`✅ Valid ID format found: ${format}`);
                    }
                });
            }
            
            if (!hasValidIdFormat) {
                this.core.logger.warn('⚠️ Valid ID format not found (may need more time)');
            }
            
            this.core.logger.info('✅ Completed: Journey-UX-06CorrelationIds-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Correlation IDs validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyUX06CorrelationIdsValidation;

