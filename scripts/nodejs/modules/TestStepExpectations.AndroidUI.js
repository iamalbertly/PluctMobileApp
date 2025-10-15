// Single source of truth for step definitions and expected UI components

const Steps = {
    AppLaunch: {
        description: 'Validate app launch and main activity',
        pre: ['Navigation'],
        post: ['MainActivity'],
        timeoutSec: 10,
    },
    ShareIntent: {
        description: 'Validate share intent handling',
        pre: ['MainActivity'],
        post: [],
        timeoutSec: 15,
    },
    VideoProcessing: {
        description: 'Validate video processing flow',
        pre: [],
        post: [],
        timeoutSec: 30,
    },
};

module.exports = { Steps };


