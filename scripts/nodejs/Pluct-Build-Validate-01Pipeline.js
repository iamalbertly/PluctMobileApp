/**
 * Pluct-Build-Validate-01Pipeline
 * Build and validation pipeline for UX fixes
 * Builds APK, installs on device, runs focused tests
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Responsibility]
 */

const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');
const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const PluctTestFocusedUXFixes = require('./Pluct-Test-Focused-01UXFixes');

class PluctBuildValidatePipeline {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.projectRoot = path.resolve(__dirname, '../..');
        this.apkPath = path.join(this.projectRoot, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
    }

    /**
     * Run complete build and validation pipeline
     */
    async run() {
        this.core.logger.info('🚀 Starting build and validation pipeline...');
        
        try {
            // Step 1: Build APK
            await this.buildAPK();
            
            // Step 2: Check if APK exists
            if (!fs.existsSync(this.apkPath)) {
                throw new Error(`APK not found at expected path: ${this.apkPath}`);
            }
            this.core.logger.info(`✅ APK built successfully: ${this.apkPath}`);
            
            // Step 3: Check ADB connection
            await this.checkAdbConnection();
            
            // Step 4: Install APK
            await this.installAPK();
            
            // Step 5: Run focused UX fixes tests
            await this.runFocusedTests();
            
            this.core.logger.info('✅ Pipeline completed successfully');
            process.exit(0);
        } catch (error) {
            this.core.logger.error(`❌ Pipeline failed: ${error.message}`);
            this.printDiagnostics();
            process.exit(1);
        }
    }

    /**
     * Build APK using gradlew
     */
    async buildAPK() {
        this.core.logger.info('🔨 Building APK...');
        
        try {
            const gradlewPath = process.platform === 'win32' 
                ? path.join(this.projectRoot, 'gradlew.bat')
                : path.join(this.projectRoot, 'gradlew');
            
            if (!fs.existsSync(gradlewPath)) {
                throw new Error(`Gradle wrapper not found: ${gradlewPath}`);
            }

            execSync(
                `"${gradlewPath}" assembleDebug`,
                {
                    cwd: this.projectRoot,
                    stdio: 'inherit',
                    encoding: 'utf-8'
                }
            );
            
            this.core.logger.info('✅ APK build completed');
        } catch (error) {
            throw new Error(`Build failed: ${error.message}`);
        }
    }

    /**
     * Check ADB connection
     */
    async checkAdbConnection() {
        this.core.logger.info('📱 Checking ADB connection...');
        
        try {
            const devices = execSync('adb devices', { encoding: 'utf-8' });
            const deviceLines = devices.split('\n').filter(line => line.trim() && !line.includes('List of devices'));
            
            if (deviceLines.length === 0) {
                throw new Error('No ADB devices connected');
            }
            
            const connectedDevices = deviceLines.filter(line => line.includes('device'));
            if (connectedDevices.length === 0) {
                throw new Error('No devices in "device" state (may be unauthorized or offline)');
            }
            
            this.core.logger.info(`✅ Found ${connectedDevices.length} connected device(s)`);
        } catch (error) {
            throw new Error(`ADB check failed: ${error.message}`);
        }
    }

    /**
     * Install APK on connected device
     */
    async installAPK() {
        this.core.logger.info('📦 Installing APK...');
        
        try {
            execSync(`adb install -r "${this.apkPath}"`, {
                stdio: 'inherit',
                encoding: 'utf-8'
            });
            
            this.core.logger.info('✅ APK installed successfully');
        } catch (error) {
            throw new Error(`Installation failed: ${error.message}`);
        }
    }

    /**
     * Run focused UX fixes tests
     */
    async runFocusedTests() {
        this.core.logger.info('🧪 Running focused UX fixes tests...');
        
        try {
            const runner = new PluctTestFocusedUXFixes();
            await runner.run();
        } catch (error) {
            throw new Error(`Test execution failed: ${error.message}`);
        }
    }

    /**
     * Print diagnostics on failure
     */
    printDiagnostics() {
        this.core.logger.info('\n📊 Diagnostics:');
        this.core.logger.info('='.repeat(50));
        
        // Check ADB
        try {
            const devices = execSync('adb devices', { encoding: 'utf-8' });
            this.core.logger.info('ADB Devices:');
            this.core.logger.info(devices);
        } catch (error) {
            this.core.logger.error(`ADB check error: ${error.message}`);
        }
        
        // Check APK existence
        this.core.logger.info(`APK Path: ${this.apkPath}`);
        this.core.logger.info(`APK Exists: ${fs.existsSync(this.apkPath)}`);
        
        // Check project structure
        this.core.logger.info(`Project Root: ${this.projectRoot}`);
        this.core.logger.info(`Project Root Exists: ${fs.existsSync(this.projectRoot)}`);
    }
}

// Run if executed directly
if (require.main === module) {
    const pipeline = new PluctBuildValidatePipeline();
    pipeline.run().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = PluctBuildValidatePipeline;






