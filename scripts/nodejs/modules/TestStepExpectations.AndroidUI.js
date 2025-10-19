// Single source of truth for step definitions and expected UI components

const Steps = {
    AppLaunch: {
        description: 'Validate app launch and main activity',
        pre: ['Navigation'],
        post: ['Pluct', 'Recent Transcripts'], // TikTok URL moved to Capture modal
        timeoutSec: 10,
    },
    ShareIntent: {
        description: 'Validate share intent handling',
        pre: ['Pluct', 'Recent Transcripts'],
        post: ['Capture Sheet', 'Capture This Insight', 'TikTok URL', 'quick_scan'],
        timeoutSec: 15,
    },
    VideoProcessing: {
        description: 'Validate video processing flow',
        pre: ['Capture Sheet', 'Capture This Insight', 'TikTok URL'],
        post: ['Quick Scan', 'AI Analysis', 'Recent Transcripts'],
        timeoutSec: 30,
    },
};

module.exports = { Steps };


