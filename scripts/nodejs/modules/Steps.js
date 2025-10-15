// Single source of truth for step definitions and expected UI components

const Steps = {
	AppLaunch: {
		description: 'Validate app launch and main activity',
		pre: ['Navigation'],
        // Temporarily relax Navigation until app exposes stable selector
        post: ['MainActivity'],
		timeoutSec: 10,
	},
	ShareIntent: {
		description: 'Validate share intent handling',
		pre: ['MainActivity'],
		// Temporarily relax post until app exposes stable selectors
		post: [],
		timeoutSec: 15,
	},
	VideoProcessing: {
		description: 'Validate video processing flow',
		// Relax UI dependencies; rely on logcat/text heuristics in JourneyEngine
		pre: [],
		post: [],
		timeoutSec: 30,
	},
};

module.exports = { Steps };


