const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');
const fs = require('fs');
const path = require('path');

/**
 * Journey-Refactor-07FileSizeCompliance-01Validation
 * Validates all Kotlin files are under 300 lines after refactoring
 * Follows naming convention: Journey-[Refactor]-[FileSizeCompliance]-[Validation]
 */
class JourneyRefactor07FileSizeCompliance01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Refactor-07FileSizeCompliance-01Validation';
    }

    async execute() {
        this.core.logger.info('Starting File Size Compliance Validation');
        
        const sourceDir = path.join(__dirname, '../../app/src/main/java');
        const violations = [];
        const exemptFiles = new Set();
        
        // Find all Kotlin files
        const kotlinFiles = this.findAllKotlinFiles(sourceDir);
        this.core.logger.info(`Found ${kotlinFiles.length} Kotlin files to validate`);
        
        for (const filePath of kotlinFiles) {
            const content = fs.readFileSync(filePath, 'utf-8');
            const lines = content.split('\n').length;
            const relativePath = path.relative(process.cwd(), filePath);
            
            // Check for SIZE-EXEMPT comment
            if (content.includes('SIZE-EXEMPT') || content.includes('// SIZE-EXEMPT')) {
                exemptFiles.add(relativePath);
                this.core.logger.info(`⚠️  ${relativePath}: ${lines} lines (SIZE-EXEMPT)`);
                continue;
            }
            
            // Check if file exceeds 300 lines
            if (lines > 300) {
                violations.push({
                    file: relativePath,
                    lines: lines,
                    excess: lines - 300
                });
                this.core.logger.error(`❌ ${relativePath}: ${lines} lines (exceeds by ${lines - 300})`);
            } else {
                this.core.logger.info(`✅ ${relativePath}: ${lines} lines`);
            }
        }
        
        // Report results
        if (violations.length > 0) {
            this.core.logger.error(`\n❌ File Size Compliance Validation FAILED`);
            this.core.logger.error(`Found ${violations.length} file(s) exceeding 300 lines:`);
            violations.forEach(v => {
                this.core.logger.error(`  - ${v.file}: ${v.lines} lines (exceeds by ${v.excess})`);
            });
            return {
                success: false,
                error: `${violations.length} file(s) exceed 300 lines`,
                violations: violations,
                exemptFiles: Array.from(exemptFiles)
            };
        }
        
        this.core.logger.info(`\n✅ File Size Compliance Validation PASSED`);
        this.core.logger.info(`All ${kotlinFiles.length} Kotlin files are under 300 lines`);
        if (exemptFiles.size > 0) {
            this.core.logger.info(`${exemptFiles.size} file(s) marked as SIZE-EXEMPT`);
        }
        
        return {
            success: true,
            totalFiles: kotlinFiles.length,
            exemptFiles: Array.from(exemptFiles)
        };
    }
    
    /**
     * Recursively find all Kotlin files
     */
    findAllKotlinFiles(dir) {
        const files = [];
        
        try {
            const entries = fs.readdirSync(dir, { withFileTypes: true });
            
            for (const entry of entries) {
                const fullPath = path.join(dir, entry.name);
                
                // Skip test directories and generated files
                if (entry.name === 'test' || entry.name === 'androidTest' || 
                    entry.name.startsWith('.') || entry.name === 'build') {
                    continue;
                }
                
                if (entry.isDirectory()) {
                    files.push(...this.findAllKotlinFiles(fullPath));
                } else if (entry.isFile() && entry.name.endsWith('.kt')) {
                    // Skip files marked for deletion
                    if (!entry.name.startsWith('DeleteThisFile_')) {
                        files.push(fullPath);
                    }
                }
            }
        } catch (error) {
            this.core.logger.warn(`Error reading directory ${dir}: ${error.message}`);
        }
        
        return files;
    }
}

module.exports = JourneyRefactor07FileSizeCompliance01Validation;
