/**
 * Pluct-Build-Deploy-01AutoService
 * Automatic build and deploy service for test fixes
 * Follows naming convention: [Project]-[Build]-[Deploy]-[AutoService]
 * 4 scope layers: Project, Build, Deploy, AutoService
 */

const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

class PluctBuildDeployAutoService {
    constructor(core) {
        this.core = core;
        this.apkPath = path.join(process.cwd(), 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
    }

    /**
     * Build and deploy APK to connected device
     * @returns {Promise<boolean>} True if successful
     */
    async buildAndDeploy() {
        try {
            this.core.logger.info('🔨 Building debug APK...');
            
            // Build debug APK
            execSync('./gradlew assembleDebug', {
                cwd: process.cwd(),
                stdio: 'inherit',
                encoding: 'utf-8'
            });

            // Verify APK exists
            if (!fs.existsSync(this.apkPath)) {
                this.core.logger.error('❌ APK not found after build');
                return false;
            }

            this.core.logger.info('✅ Build successful');

            // Check if device is connected
            this.core.logger.info('📱 Checking for connected device...');
            const deviceCheck = execSync('adb devices', { encoding: 'utf-8' });
            if (!deviceCheck.includes('device')) {
                this.core.logger.error('❌ No device connected');
                return false;
            }

            this.core.logger.info('✅ Device connected');

            // Install to connected device
            this.core.logger.info('📦 Installing APK to device...');
            execSync(`adb install -r "${this.apkPath}"`, {
                stdio: 'inherit',
                encoding: 'utf-8'
            });

            // Verify installation
            this.core.logger.info('✅ Verifying installation...');
            const packageCheck = execSync('adb shell pm list packages | findstr pluct', { encoding: 'utf-8' });
            if (!packageCheck.includes('app.pluct')) {
                this.core.logger.error('❌ Installation verification failed');
                return false;
            }

            this.core.logger.info('✅ Installation verified');
            this.core.logger.info('🎉 Build and deployment successful!');
            return true;
        } catch (error) {
            this.core.logger.error(`❌ Build/deploy error: ${error.message}`);
            return false;
        }
    }

    /**
     * Build APK only (no deploy)
     * @returns {Promise<boolean>} True if successful
     */
    async buildOnly() {
        try {
            this.core.logger.info('🔨 Building debug APK...');
            
            execSync('./gradlew assembleDebug', {
                cwd: process.cwd(),
                stdio: 'inherit',
                encoding: 'utf-8'
            });

            if (!fs.existsSync(this.apkPath)) {
                this.core.logger.error('❌ APK not found after build');
                return false;
            }

            this.core.logger.info('✅ Build successful');
            return true;
        } catch (error) {
            this.core.logger.error(`❌ Build error: ${error.message}`);
            return false;
        }
    }

    /**
     * Deploy existing APK (no build)
     * @returns {Promise<boolean>} True if successful
     */
    async deployOnly() {
        try {
            if (!fs.existsSync(this.apkPath)) {
                this.core.logger.error('❌ APK not found, build first');
                return false;
            }

            // Check if device is connected
            this.core.logger.info('📱 Checking for connected device...');
            const deviceCheck = execSync('adb devices', { encoding: 'utf-8' });
            if (!deviceCheck.includes('device')) {
                this.core.logger.error('❌ No device connected');
                return false;
            }

            // Install to connected device
            this.core.logger.info('📦 Installing APK to device...');
            execSync(`adb install -r "${this.apkPath}"`, {
                stdio: 'inherit',
                encoding: 'utf-8'
            });

            // Verify installation
            this.core.logger.info('✅ Verifying installation...');
            const packageCheck = execSync('adb shell pm list packages | findstr pluct', { encoding: 'utf-8' });
            if (!packageCheck.includes('app.pluct')) {
                this.core.logger.error('❌ Installation verification failed');
                return false;
            }

            this.core.logger.info('✅ Installation verified');
            return true;
        } catch (error) {
            this.core.logger.error(`❌ Deploy error: ${error.message}`);
            return false;
        }
    }

    /**
     * Check if APK exists
     * @returns {boolean} True if APK exists
     */
    apkExists() {
        return fs.existsSync(this.apkPath);
    }

    /**
     * Get APK path
     * @returns {string} APK path
     */
    getApkPath() {
        return this.apkPath;
    }
}

module.exports = PluctBuildDeployAutoService;

