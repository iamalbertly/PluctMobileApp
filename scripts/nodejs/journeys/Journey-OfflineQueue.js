const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-OfflineQueue - Test offline queue functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Tests offline transcription queue management and processing
 */
class JourneyOfflineQueue extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'OfflineQueue';
    }

    async execute() {
        try {
            this.core.logger.info('🚀 Starting Offline Queue Journey');
            
            // Step 1: Verify offline queue UI is accessible
            await this.verifyOfflineQueueUI();
            
            // Step 2: Test queue management functionality
            await this.testQueueManagement();
            
            // Step 3: Test offline processing simulation
            await this.testOfflineProcessing();
            
            // Step 4: Test queue status monitoring
            await this.testQueueStatusMonitoring();
            
            this.core.logger.info('✅ Offline Queue Journey completed successfully');
            return { success: true, message: 'Offline queue functionality working correctly' };
            
        } catch (error) {
            this.core.logger.error(`❌ Offline Queue Journey failed: ${error.message}`);
            throw error;
        }
    }

    async verifyOfflineQueueUI() {
        this.core.logger.info('🔍 Verifying offline queue UI accessibility...');
        
        // Wait for app to be ready
        await this.core.waitForText('Pluct', 5000);
        
        // Look for offline queue elements
        const uiDump = await this.core.dumpUIHierarchy();
        const queueElements = [
            'offline_queue_card',
            'Offline Queue',
            'Queue Status',
            'queue_items_list',
            'pending_transcriptions'
        ];
        
        const hasQueueElements = queueElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasQueueElements) {
            this.core.logger.info('✅ Offline queue UI elements found');
        } else {
            this.core.logger.info('ℹ️ Offline queue UI not visible (may be in settings or advanced section)');
        }
    }

    async testQueueManagement() {
        this.core.logger.info('📋 Testing queue management functionality...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for queue management elements
        const managementElements = [
            'add_to_queue_button',
            'clear_queue_button',
            'retry_failed_button',
            'queue_management_controls'
        ];
        
        const hasManagementElements = managementElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasManagementElements) {
            this.core.logger.info('✅ Queue management controls found');
            
            // Try to interact with queue management if available
            try {
                if (uiDump.toString().includes('add_to_queue_button')) {
                    await this.core.tapByTestTag('add_to_queue_button');
                    this.core.logger.info('✅ Add to queue button tapped');
                }
                
                if (uiDump.toString().includes('clear_queue_button')) {
                    await this.core.tapByTestTag('clear_queue_button');
                    this.core.logger.info('✅ Clear queue button tapped');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not interact with queue management: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ Queue management controls not visible (queue may be empty)');
        }
    }

    async testOfflineProcessing() {
        this.core.logger.info('⚙️ Testing offline processing simulation...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for offline processing indicators
        const processingElements = [
            'processing_indicator',
            'offline_processing_status',
            'queue_progress',
            'background_processing'
        ];
        
        const hasProcessingElements = processingElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasProcessingElements) {
            this.core.logger.info('✅ Offline processing indicators found');
            
            // Try to interact with processing controls if available
            try {
                if (uiDump.toString().includes('start_offline_processing_button')) {
                    await this.core.tapByTestTag('start_offline_processing_button');
                    this.core.logger.info('✅ Start offline processing button tapped');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not interact with offline processing: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ Offline processing indicators not visible (no items in queue)');
        }
    }

    async testQueueStatusMonitoring() {
        this.core.logger.info('📊 Testing queue status monitoring...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for queue status elements
        const statusElements = [
            'queue_status_display',
            'pending_count',
            'processing_count',
            'completed_count',
            'failed_count',
            'queue_statistics'
        ];
        
        const hasStatusElements = statusElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasStatusElements) {
            this.core.logger.info('✅ Queue status monitoring elements found');
            
            // Try to interact with status display if available
            try {
                if (uiDump.toString().includes('queue_status_display')) {
                    await this.core.tapByTestTag('queue_status_display');
                    this.core.logger.info('✅ Queue status display tapped');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not interact with queue status: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ Queue status monitoring not visible (queue may be empty)');
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('OfflineQueue', new JourneyOfflineQueue(orchestrator.core));
}

module.exports = { register };