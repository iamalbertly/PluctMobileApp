/**
 * Pluct-Maestro-Test-01Runner-01Orchestrator.js
 * Maestro test runner that integrates with existing infrastructure
 * Follows naming: [Project]-[Maestro]-[Test]-[01Runner]-[01Orchestrator]
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

class PluctMaestroTestRunnerOrchestrator {
    constructor() {
        this.maestroDir = path.join(__dirname);
        this.flowsDir = path.join(this.maestroDir, 'flows');
        this.results = [];
        this.devMode = process.env.DEV_MODE === '1' || process.env.NODE_ENV === 'development';
    }

    /**
     * Discover all Maestro flow files
     */
    discoverFlows() {
        const flows = [];
        const categories = ['01-core', '02-ui', '03-transcription', '04-queue', '05-edge-cases', '06-ux-improvements', '07-intent-fixes', '08-onboarding', '09-onboarding-improvements'];
        
        // Filter by category if specified
        const categoryFilter = process.env.MAESTRO_CATEGORY;
        const filterPattern = process.env.MAESTRO_FILTER;
        
        for (const category of categories) {
            // Skip if category filter is set and doesn't match
            if (categoryFilter && !category.includes(categoryFilter)) {
                continue;
            }
            
            const categoryDir = path.join(this.flowsDir, category);
            if (fs.existsSync(categoryDir)) {
                const files = fs.readdirSync(categoryDir)
                    .filter(f => {
                        if (!f.endsWith('.yaml')) return false;
                        // Apply filter pattern if specified
                        if (filterPattern && !f.includes(filterPattern)) return false;
                        return true;
                    })
                    .sort();
                
                for (const file of files) {
                    flows.push({
                        category,
                        file,
                        path: path.join(categoryDir, file)
                    });
                }
            }
        }
        
        return flows;
    }

    /**
     * Find Maestro executable
     */
    findMaestro() {
        const os = require('os');
        const defaultPath = path.join(os.homedir(), '.maestro', 'bin', process.platform === 'win32' ? 'maestro.bat' : 'maestro');
        
        try {
            execSync('maestro --version', { stdio: 'pipe' });
            return 'maestro';
        } catch {
            if (fs.existsSync(defaultPath)) {
                return defaultPath;
            }
            throw new Error('Maestro not found. Install with: curl -Ls "https://get.maestro.mobile.dev" | bash');
        }
    }

    /**
     * Run a single Maestro flow
     */
    async runFlow(flow) {
        console.log(`\n🎯 Running: ${flow.category}/${flow.file}`);
        
        try {
            const maestroCmd = this.findMaestro();
            const flowPath = path.relative(process.cwd(), flow.path).replace(/\\/g, '/');
            
            // Build environment with PATH for Windows
            const env = { ...process.env };
            const os = require('os');
            if (process.platform === 'win32') {
                const maestroDir = path.dirname(maestroCmd);
                env.PATH = `${maestroDir};${env.PATH}`;
            }
            
            // Use absolute path for flow file
            const absoluteFlowPath = flow.path.replace(/\\/g, '/');
            const command = process.platform === 'win32' 
                ? `"${maestroCmd}" test "${absoluteFlowPath}"`
                : `${maestroCmd} test "${absoluteFlowPath}"`;
            
            console.log(`   Command: ${command}`);
            
            const startTime = Date.now();
            const output = execSync(command, { 
                encoding: 'utf-8',
                cwd: process.cwd(),
                stdio: 'pipe',
                env: env
            });
            const duration = Date.now() - startTime;
            
            const result = {
                flow: flow.file,
                category: flow.category,
                success: true,
                duration,
                output: output.substring(0, 500) // Truncate for logging
            };
            
            console.log(`   ✅ PASSED (${duration}ms)`);
            this.results.push(result);
            return result;
            
        } catch (error) {
            const duration = Date.now() - (error.startTime || Date.now());
            const result = {
                flow: flow.file,
                category: flow.category,
                success: false,
                duration,
                error: error.message,
                output: error.stdout || error.stderr || ''
            };
            
            console.log(`   ❌ FAILED (${duration}ms): ${error.message}`);
            this.results.push(result);
            
            if (this.devMode) {
                console.log(`\n❌ Terminating on first error (dev mode)`);
                throw error;
            }
            
            return result;
        }
    }

    /**
     * Run all discovered flows
     */
    async runAll() {
        console.log('🚀 Starting Maestro Test Runner...');
        console.log(`   Mode: ${this.devMode ? 'DEV (terminates on first error)' : 'PROD (continues on errors)'}`);
        
        const flows = this.discoverFlows();
        console.log(`   Found ${flows.length} flow files\n`);
        
        for (const flow of flows) {
            try {
                await this.runFlow(flow);
            } catch (error) {
                if (this.devMode) {
                    this.printSummary();
                    process.exit(1);
                }
            }
        }
        
        this.printSummary();
        return this.getExitCode() === 0;
    }

    /**
     * Print test summary
     */
    printSummary() {
        const successful = this.results.filter(r => r.success).length;
        const total = this.results.length;
        const successRate = total > 0 ? (successful / total * 100).toFixed(1) : 0;
        
        console.log('\n📊 === MAESTRO TEST SUMMARY ===');
        console.log(`   Total: ${total}`);
        console.log(`   Passed: ${successful}`);
        console.log(`   Failed: ${total - successful}`);
        console.log(`   Success Rate: ${successRate}%`);
        
        if (this.results.some(r => !r.success)) {
            console.log('\n❌ Failed Tests:');
            this.results.filter(r => !r.success).forEach(r => {
                console.log(`   - ${r.category}/${r.flow}: ${r.error}`);
            });
        }
    }

    /**
     * Get exit code
     */
    getExitCode() {
        return this.results.every(r => r.success) ? 0 : 1;
    }
}

// Main execution
if (require.main === module) {
    const runner = new PluctMaestroTestRunnerOrchestrator();
    runner.runAll()
        .then(success => {
            process.exit(success ? 0 : 1);
        })
        .catch(error => {
            console.error('❌ Runner failed:', error.message);
            process.exit(1);
        });
}

module.exports = PluctMaestroTestRunnerOrchestrator;
