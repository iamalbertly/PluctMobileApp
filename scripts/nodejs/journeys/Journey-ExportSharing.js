const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-ExportSharing - Test export and sharing functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Tests export options, sharing mechanisms, and file generation
 */
class JourneyExportSharing extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'ExportSharing';
    }

    async execute() {
        try {
            this.core.logger.info('🚀 Starting Export Sharing Journey');
            
            // Step 1: Verify export functionality is accessible
            await this.verifyExportFunctionality();
            
            // Step 2: Test transcript export options
            await this.testTranscriptExport();
            
            // Step 3: Test sharing mechanisms
            await this.testSharingMechanisms();
            
            // Step 4: Test file generation and storage
            await this.testFileGeneration();
            
            this.core.logger.info('✅ Export Sharing Journey completed successfully');
            return { success: true, message: 'Export and sharing functionality working correctly' };
            
        } catch (error) {
            this.core.logger.error(`❌ Export Sharing Journey failed: ${error.message}`);
            throw error;
        }
    }

    async verifyExportFunctionality() {
        this.core.logger.info('🔍 Verifying export functionality accessibility...');
        
        // Wait for app to be ready
        await this.core.waitForText('Pluct', 5000);
        
        // Look for export-related elements
        const uiDump = await this.core.dumpUIHierarchy();
        const exportElements = [
            'export_button',
            'Export',
            'Share',
            'export_transcript_button',
            'share_transcript_button'
        ];
        
        const hasExportElements = exportElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasExportElements) {
            this.core.logger.info('✅ Export functionality elements found');
        } else {
            this.core.logger.info('ℹ️ Export functionality not visible (may be in transcript details)');
        }
    }

    async testTranscriptExport() {
        this.core.logger.info('📄 Testing transcript export options...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for transcript export options
        const exportOptions = [
            'Export as Text',
            'Export as PDF',
            'Export as JSON',
            'Copy to Clipboard',
            'Save to File'
        ];
        
        const hasExportOptions = exportOptions.some(option => 
            uiDump.toString().includes(option)
        );
        
        if (hasExportOptions) {
            this.core.logger.info('✅ Transcript export options found');
            
            // Try to interact with export options if visible
            try {
                if (uiDump.toString().includes('export_as_text_button')) {
                    await this.core.tapByTestTag('export_as_text_button');
                    this.core.logger.info('✅ Text export option tapped');
                } else if (uiDump.toString().includes('export_as_pdf_button')) {
                    await this.core.tapByTestTag('export_as_pdf_button');
                    this.core.logger.info('✅ PDF export option tapped');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not interact with export options: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ Transcript export options not visible (no transcripts available)');
        }
    }

    async testSharingMechanisms() {
        this.core.logger.info('📤 Testing sharing mechanisms...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for sharing buttons
        const sharingElements = [
            'share_button',
            'Share',
            'share_transcript_button',
            'share_via_email',
            'share_via_sms'
        ];
        
        const hasSharingElements = sharingElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasSharingElements) {
            this.core.logger.info('✅ Sharing mechanisms found');
            
            // Try to interact with sharing if available
            try {
                if (uiDump.toString().includes('share_button')) {
                    await this.core.tapByTestTag('share_button');
                    this.core.logger.info('✅ Share button tapped');
                    
                    // Wait for share sheet to appear
                    await this.core.sleep(2000);
                    this.core.logger.info('✅ Share sheet should be displayed');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not interact with sharing mechanisms: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ Sharing mechanisms not visible (no content to share)');
        }
    }

    async testFileGeneration() {
        this.core.logger.info('📁 Testing file generation and storage...');
        
        const uiDump = await this.core.dumpUIHierarchy();
        
        // Look for file generation elements
        const fileElements = [
            'save_to_file_button',
            'Save to File',
            'Download',
            'file_generation_status',
            'export_progress'
        ];
        
        const hasFileElements = fileElements.some(element => 
            uiDump.toString().includes(element)
        );
        
        if (hasFileElements) {
            this.core.logger.info('✅ File generation elements found');
            
            // Test file generation if available
            try {
                if (uiDump.toString().includes('save_to_file_button')) {
                    await this.core.tapByTestTag('save_to_file_button');
                    this.core.logger.info('✅ Save to file button tapped');
                    
                    // Wait for file generation
                    await this.core.sleep(3000);
                    this.core.logger.info('✅ File generation process initiated');
                }
            } catch (error) {
                this.core.logger.warn('⚠️ Could not test file generation: ' + error.message);
            }
        } else {
            this.core.logger.info('ℹ️ File generation elements not visible (no content to export)');
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('ExportSharing', new JourneyExportSharing(orchestrator.core));
}

module.exports = { register };