#!/usr/bin/env node
/**
 * Focused runner: UX-27 redesign / shell parity (ADB + UI + logcat).
 */
process.env.TEST_FILTER = 'Journey-UX-27PluctRedesign-MockupParity-01Validation.js';
require('./Pluct-Main-01Orchestrator.js');
