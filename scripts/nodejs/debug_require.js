const path = require('path');
const journeysDir = path.join(__dirname, 'journeys');

console.log('Checking journeys in:', journeysDir);

try {
    const Journey1 = require(path.join(journeysDir, 'Journey-TikTok-Manual-URL-01Transcription.js'));
    console.log('Successfully required Journey-TikTok-Manual-URL-01Transcription.js');
    console.log('Export type:', typeof Journey1);
    try {
        const instance = new Journey1({ logger: console });
        console.log('Instance name:', instance.name);
    } catch (e) {
        console.error('Instantiation failed:', e.message);
    }

    const Journey2 = require(path.join(journeysDir, 'Pluct-Journey-Home-04EmptyState-01DemoLink.js'));
    console.log('Successfully required Pluct-Journey-Home-04EmptyState-01DemoLink.js');
    console.log('Export type:', typeof Journey2);
    try {
        const instance = new Journey2({ logger: console });
        console.log('Instance name:', instance.name);
    } catch (e) {
        console.error('Instantiation failed:', e.message);
    }

} catch (error) {
    console.error('Require failed:', error);
}
