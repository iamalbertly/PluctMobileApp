const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');
const fs = require('fs');
const path = require('path');

class JourneyRefactor05NamingConsistencyValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-Refactor-05NamingConsistency-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Naming Consistency Validation');
        
        try {
            // Step 1: Launch app to verify it runs
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Step 2: Validate file naming follows pattern
            this.core.logger.info('- Step 2: Validate file naming patterns');
            const projectRoot = path.resolve(__dirname, '../../..');
            const kotlinDir = path.join(projectRoot, 'app/src/main/java/app/pluct');
            
            if (!fs.existsSync(kotlinDir)) {
                return { success: false, error: 'Kotlin source directory not found' };
            }
            
            // Get all Kotlin files
            const kotlinFiles = this.getAllKotlinFiles(kotlinDir);
            this.core.logger.info(`Found ${kotlinFiles.length} Kotlin files`);
            
            // Check naming patterns
            const patterns = {
                'Pluct-UI-': 0,
                'Pluct-Core-': 0,
                'Pluct-Data-': 0,
                'Pluct-Mobile-UI-': 0,
                'Pluct-': 0,
                'other': 0
            };
            
            const outliers = [];
            
            kotlinFiles.forEach(file => {
                const fileName = path.basename(file);
                if (fileName.startsWith('Pluct-UI-')) {
                    patterns['Pluct-UI-']++;
                } else if (fileName.startsWith('Pluct-Core-')) {
                    patterns['Pluct-Core-']++;
                } else if (fileName.startsWith('Pluct-Data-')) {
                    patterns['Pluct-Data-']++;
                } else if (fileName.startsWith('Pluct-Mobile-UI-')) {
                    patterns['Pluct-Mobile-UI-']++;
                } else if (fileName.startsWith('Pluct-')) {
                    patterns['Pluct-']++;
                } else {
                    patterns['other']++;
                    outliers.push(fileName);
                }
            });
            
            this.core.logger.info(`Naming patterns: ${JSON.stringify(patterns, null, 2)}`);
            
            // Step 3: Check for critical rename (VideoProcessor -> TranscriptionOrchestrator)
            this.core.logger.info('- Step 3: Validate VideoProcessor rename');
            const hasVideoProcessor = kotlinFiles.some(f => 
                f.includes('VideoProcessor') && !f.includes('TranscriptionOrchestrator')
            );
            
            if (hasVideoProcessor) {
                return { 
                    success: false, 
                    error: 'VideoProcessor file still exists (should be TranscriptionOrchestrator)' 
                };
            }
            
            const hasTranscriptionOrchestrator = kotlinFiles.some(f => 
                f.includes('TranscriptionOrchestrator')
            );
            
            if (!hasTranscriptionOrchestrator) {
                return { 
                    success: false, 
                    error: 'TranscriptionOrchestrator file not found' 
                };
            }
            
            this.core.logger.info('✅ VideoProcessor renamed to TranscriptionOrchestrator');
            
            // Step 4: Check logcat for class name consistency
            this.core.logger.info('- Step 4: Validate class names in logs');
            await this.core.clearLogcat();
            await this.core.sleep(2000);
            
            const logcat = await this.core.captureAPILogs(100);
            const logcatText = logcat.join('\n');
            
            // Check for old class names
            const hasOldClassName = logcatText.includes('PluctUIScreen01MainActivityVideoProcessor');
            
            if (hasOldClassName) {
                return { 
                    success: false, 
                    error: 'Old class name still appears in logs' 
                };
            }
            
            // Step 5: Verify app functionality
            this.core.logger.info('- Step 5: Verify app functionality');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump() || '';
            
            const hasUIElements = uiDump.includes('url_input_field') || 
                                 uiDump.includes('Paste a TikTok link');
            
            if (!hasUIElements) {
                this.core.logger.warn('⚠️ Expected UI elements not found');
            } else {
                this.core.logger.info('✅ UI elements present');
            }
            
            // Step 6: Report outliers (non-critical)
            if (outliers.length > 0) {
                this.core.logger.warn(`⚠️ Found ${outliers.length} files with non-standard naming: ${outliers.slice(0, 5).join(', ')}`);
            }
            
            this.core.logger.info('✅ Naming consistency validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
    
    getAllKotlinFiles(dir) {
        const files = [];
        
        function walk(currentDir) {
            const entries = fs.readdirSync(currentDir, { withFileTypes: true });
            
            for (const entry of entries) {
                const fullPath = path.join(currentDir, entry.name);
                
                if (entry.isDirectory()) {
                    walk(fullPath);
                } else if (entry.isFile() && entry.name.endsWith('.kt')) {
                    files.push(fullPath);
                }
            }
        }
        
        walk(dir);
        return files;
    }
}

module.exports = JourneyRefactor05NamingConsistencyValidation;

