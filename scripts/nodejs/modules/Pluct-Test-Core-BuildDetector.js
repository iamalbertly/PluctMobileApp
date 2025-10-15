const fs = require('fs');
const path = require('path');
const { execOut, fileExists } = require('../core/Pluct-Test-Core-Exec');
const { TestSession } = require('./Pluct-Test-Core-Status');

function listChangedFiles() {
    const out = execOut('git status --porcelain');
    return out
        .split(/\r?\n/)
        .map(s => s.trim())
        .filter(Boolean)
        .map(s => s.slice(3));
}

function isBuildRequired(forceBuild) {
    if (forceBuild) {
        TestSession.SmartBuildDetection.BuildReason = 'ForceBuild flag set';
        return true;
    }
    const changed = listChangedFiles();
    TestSession.SmartBuildDetection.ChangedFiles = changed;
    if (changed.length === 0) return false;
    const codeTouched = changed.some(f => /(\.kt|\.kts|\.java|\.xml|gradle)$/i.test(f) || f.startsWith('app/'));
    if (codeTouched) {
        TestSession.SmartBuildDetection.BuildReason = 'Detected code/config changes';
        return true;
    }
    return false;
}

module.exports = { isBuildRequired, fileExists };


