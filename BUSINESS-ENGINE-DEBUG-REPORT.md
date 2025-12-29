# 🔍 Business Engine & TTTranscribe Integration Debug Report

**Generated**: 2025-12-01  
**Report Type**: Comprehensive Error Analysis & Debugging Guide  
**Purpose**: Detailed investigation report for Business Engine team to diagnose and fix integration issues

---

## 📋 Executive Summary

This report documents critical authentication and API communication failures between the Pluct Mobile App and the Business Engine service. The primary issues are:

1. **JWT Token Authentication Failures** - 401 Unauthorized errors with "exp claim timestamp check failed"
2. **Circuit Breaker Activation** - System entering degraded state after consecutive failures
3. **Balance Check Failures** - All credit balance requests failing
4. **Token Vending Issues** - Potential endpoint path mismatches
5. **Transcription Flow Blocked** - Unable to proceed past authentication stage

---

## 🔐 Authentication Configuration

### JWT Secret (Testing Environment)
```
Secret: prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e
```
**Note**: This is a test secret. Change before production deployment.

### Mobile App JWT Generation Details

**Location**: `app/src/main/java/app/pluct/services/Pluct-Core-API-03JWTGenerator.kt`

**Current Configuration**:
- Algorithm: HS256
- Token Expiry: 3600 seconds (1 hour)
- Audience: "pluct-business-engine"
- Scope: "ttt:transcribe"
- User ID: "mobile" (hardcoded for testing)

**JWT Payload Structure**:
```json
{
  "sub": "mobile",
  "aud": "pluct-business-engine",
  "scope": "ttt:transcribe",
  "iat": <current_timestamp - 60>,
  "exp": <iat + 3600>
}
```

**Issue Identified**: The mobile app generates tokens with 1-hour expiry, but the Business Engine integration guide suggests 15-minute expiry for balance checks. This may be causing validation failures.

---

## 🚨 Critical Errors Found

### 1. JWT Token Expiration Validation Failures

**Error Pattern**: `"exp" claim timestamp check failed`

**Log Evidence**:
```
12-01 05:06:13.141 32023 32069 E PluctAPI: ❌ API ERROR [req_1764554772941]
12-01 05:06:13.141 32023 32069 E PluctAPI:    Status: 401 Unauthorized
12-01 05:06:13.141 32023 32069 E PluctAPI:    Error Body (full): {
  "ok": false,
  "code": "unauthorized",
  "message": "Invalid or expired token",
  "details": {
    "error": "\"exp\" claim timestamp check failed"
  },
  "build": {
    "ref": "c16de6c3-26f1-4f22-9873-d2c835209098",
    "deployedAt": "2025-10-15T04:28:10Z",
    "gitVersion": "2b7eec18e164afeb4e6b1e7332bd913964854df6"
  },
  "guidance": "Provide a valid authentication token"
}
```

**Frequency**: Multiple occurrences across all test runs

**Impact**: 
- All balance check requests fail
- Token vending requests fail
- Circuit breaker opens after 6 consecutive failures
- User cannot check credits or start transcriptions

**Root Cause Analysis**:
1. **Clock Skew**: The mobile device clock may be out of sync with Business Engine server time
2. **Expiry Validation**: Business Engine may be rejecting tokens that are too far in the future
3. **Timestamp Format**: Possible mismatch in how timestamps are calculated or validated

**Business Engine Investigation Needed**:
- Check server clock synchronization
- Verify JWT expiration validation logic
- Review clock skew tolerance settings (should be ±2 minutes per integration guide)
- Check if tokens with 1-hour expiry are being rejected when they should be accepted

---

### 2. JWT Signature Verification Failures

**Error Pattern**: `signature verification failed`

**Log Evidence** (from `artifacts/be_transcribe.json`):
```json
{
  "status": 401,
  "data": "{\"ok\":false,\"code\":\"unauthorized\",\"message\":\"Invalid or expired token\",\"details\":{\"error\":\"signature verification failed\"},...}"
}
```

**Frequency**: Intermittent, appears alongside expiration failures

**Impact**: Authentication completely blocked

**Root Cause Analysis**:
1. **Secret Mismatch**: The JWT secret used by mobile app may not match Business Engine's `ENGINE_JWT_SECRET`
2. **Encoding Issues**: Possible Base64 URL encoding/decoding mismatch
3. **Algorithm Mismatch**: HS256 should be used, verify Business Engine is using same algorithm

**Business Engine Investigation Needed**:
- Verify `ENGINE_JWT_SECRET` environment variable matches: `prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e`
- Check JWT signature verification code
- Verify Base64 URL encoding/decoding implementation
- Test with a known-good JWT token to isolate the issue

---

### 3. Circuit Breaker Activation

**Error Pattern**: Circuit breaker opens after consecutive failures

**Log Evidence**:
```
12-01 06:38:20.940  2225  2225 E PluctAPI: 🔴 Circuit breaker OPENED - 6 consecutive failures (threshold: 5)
12-01 06:38:20.940  2225  2225 E PluctCoreAPIUnified: 🔴 Circuit breaker OPENED - 6 consecutive failures (threshold: 5)
12-01 06:38:20.940  2225  2225 E PluctAPI: ❌ Request failed after 3 attempts [req_1764560297269]: HTTP 401: Invalid or expired token
12-01 06:38:20.940  2225  2225 E PluctCoreAPIUnified: ❌ Failed to get balance: HTTP 401: Invalid or expired token
```

**Impact**: 
- All subsequent API calls are blocked
- App cannot recover without restart
- User experience severely degraded

**Business Engine Investigation Needed**:
- Review why authentication is failing consistently
- Check if there are rate limiting or security measures blocking valid requests
- Verify that valid JWT tokens are being accepted

---

### 4. Balance Check Endpoint Failures

**Endpoint**: `GET /v1/credits/balance`

**Request Details**:
- Method: GET
- URL: `https://pluct-business-engine.romeo-lya2.workers.dev/v1/credits/balance`
- Authorization: `Bearer <jwt_token>`
- User ID: "mobile"

**Response**: 401 Unauthorized (all attempts)

**Log Evidence**:
```
12-01 05:06:12.941 32023 32069 D PluctCoreAPIHTTPClient: 🚀 API REQUEST [req_1764554772941] - GET /v1/credits/balance
12-01 05:06:12.942 32023 32069 D PluctCoreAPIHTTPClient:    ✅ Authorization header set
12-01 05:06:13.141 32023 32069 E PluctAPI: ❌ API ERROR [req_1764554772941]
12-01 05:06:13.141 32023 32069 E PluctAPI:    Status: 401 Unauthorized
```

**Business Engine Investigation Needed**:
- Check Business Engine logs for requests to `/v1/credits/balance` from mobile app
- Verify JWT token validation is working correctly
- Check if user "mobile" exists in the database
- Review authentication middleware for this endpoint

---

### 5. Token Vending Endpoint Issues

**Endpoint**: `POST /v1/vend-token`

**Historical Issue** (from older logs):
```
10-21 02:08:29.064 14657 14707 E BusinessEngineHealthChecker: Token vending test failed: 404
10-21 02:08:29.064 14657 14707 E TTT     : stage=VENDING_TOKEN url=- reqId=- msg=failed code=404
```

**Note**: This appears to be an older issue where the endpoint path was incorrect. Current code uses `/v1/vend-token` which should be correct.

**Current Status**: Unable to test due to authentication failures blocking all requests

**Business Engine Investigation Needed**:
- Verify `/v1/vend-token` endpoint is accessible
- Check if endpoint requires different authentication than balance check
- Review endpoint implementation for any special validation requirements

---

## 📊 Request Flow Analysis

### Expected Flow:
1. ✅ App Launch → Generate JWT Token
2. ❌ Balance Check → **FAILS (401 Unauthorized)**
3. ❌ Token Vending → **BLOCKED (Circuit Breaker)**
4. ❌ Transcription Submission → **NEVER REACHED**

### Actual Flow:
1. ✅ App Launch → JWT Token Generated
2. ❌ Balance Check → 401 Error → Retry 3 times → Circuit Breaker Opens
3. ❌ All subsequent requests blocked

---

## 🔍 Detailed Log Analysis

### Recent Test Run (2025-12-01 13:46:33 UTC)

**Test**: Journey-TikTok-Manual-URL-01Transcription.js

**Key Events**:
1. App launched successfully
2. Welcome screen detected and handled
3. Capture component found
4. URL entered: `https://vm.tiktok.com/ZMAKpqkpN/`
5. **FAILED**: "Failed to submit for processing"

**Error Context**:
- No API errors in recent logcat (errors occurred earlier)
- Circuit breaker likely already open from previous failures
- UI test cannot proceed past submission step

### Earlier Test Run (2025-12-01 10:51:10 UTC)

**Key Errors**:
```
12-01 05:06:13.141 32023 32069 E PluctAPI: ❌ API ERROR [req_1764554772941]
12-01 05:06:13.141 32023 32069 E PluctAPI:    Status: 401 Unauthorized
12-01 05:06:13.141 32023 32069 E PluctAPI:    Error Body (full): {
  "ok": false,
  "code": "unauthorized",
  "message": "Invalid or expired token",
  "details": {
    "error": "\"exp\" claim timestamp check failed"
  }
}
```

**Retry Pattern**:
- Request attempted 3 times
- All attempts failed with 401
- Circuit breaker opened after 6 consecutive failures

---

## 🛠️ Business Engine Investigation Checklist

### Immediate Actions Required:

1. **Verify JWT Secret Configuration**
   - [ ] Check `ENGINE_JWT_SECRET` environment variable in Business Engine
   - [ ] Confirm it matches: `prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e`
   - [ ] Verify secret is properly loaded and accessible to JWT validation code

2. **Review JWT Validation Logic**
   - [ ] Check expiration timestamp validation code
   - [ ] Verify clock skew tolerance (should be ±2 minutes per integration guide)
   - [ ] Review how `exp` claim is validated
   - [ ] Check if 1-hour expiry tokens are being rejected incorrectly

3. **Check Server Clock Synchronization**
   - [ ] Verify Business Engine server time is synchronized (NTP)
   - [ ] Check for time drift issues
   - [ ] Compare server time with mobile device time from logs

4. **Review Authentication Middleware**
   - [ ] Check JWT signature verification implementation
   - [ ] Verify Base64 URL encoding/decoding
   - [ ] Review error handling and logging for authentication failures

5. **Check Business Engine Logs**
   - [ ] Search for requests from mobile app (user ID: "mobile")
   - [ ] Review authentication failure logs
   - [ ] Check for any rate limiting or security blocks
   - [ ] Verify request headers are being received correctly

6. **Test with Known-Good Token**
   - [ ] Generate a test JWT token using Business Engine's own code
   - [ ] Verify it can authenticate successfully
   - [ ] Compare with mobile app generated token structure

7. **Review Endpoint Accessibility**
   - [ ] Verify `/v1/credits/balance` endpoint is accessible
   - [ ] Verify `/v1/vend-token` endpoint is accessible
   - [ ] Check CORS configuration if applicable
   - [ ] Review any IP-based restrictions

---

## 📝 Sample JWT Token for Testing

**Mobile App Generated Token Structure**:
```
Header: {"alg":"HS256","typ":"JWT"}
Payload: {
  "sub": "mobile",
  "aud": "pluct-business-engine",
  "scope": "ttt:transcribe",
  "iat": <timestamp - 60>,
  "exp": <timestamp + 3600>
}
Signature: HMAC-SHA256(header.payload, secret)
```

**Test Token Generation** (for Business Engine team to verify):
```javascript
const jwt = require('jsonwebtoken');
const secret = 'prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e';

const token = jwt.sign(
  {
    sub: 'mobile',
    aud: 'pluct-business-engine',
    scope: 'ttt:transcribe',
    iat: Math.floor(Date.now() / 1000) - 60,
    exp: Math.floor(Date.now() / 1000) + 3600
  },
  secret,
  { algorithm: 'HS256' }
);

console.log('Test Token:', token);
```

**Expected Behavior**: This token should authenticate successfully with Business Engine.

---

## 🔄 TTTranscribe Integration Status

### Current Status: BLOCKED

**Reason**: Cannot reach TTTranscribe integration due to authentication failures at Business Engine level.

### Expected Flow (Once Authentication Fixed):
1. Mobile App → Business Engine: Balance Check ✅
2. Mobile App → Business Engine: Vend Token ✅
3. Mobile App → Business Engine: Submit Transcription ✅
4. Business Engine → TTTranscribe: Forward Request
5. Business Engine → Mobile App: Return Job ID
6. Mobile App → Business Engine: Poll Status
7. Business Engine → TTTranscribe: Check Status
8. Business Engine → Mobile App: Return Transcript

### TTTranscribe Configuration
- **Base URL**: `https://iamromeoly-tttranscribe.hf.space`
- **Shared Secret**: Set in Business Engine `TTT_SHARED_SECRET` environment variable
- **Status**: Unknown (cannot test due to authentication block)

**Business Engine Investigation Needed**:
- Verify TTTranscribe connectivity from Business Engine
- Check `TTT_SHARED_SECRET` configuration
- Review webhook event queue (user mentioned clearing it)
- Test TTTranscribe integration independently

---

## 📋 Mobile App Configuration

### Base URL
```
https://pluct-business-engine.romeo-lya2.workers.dev
```

### User Identification
- **User ID**: "mobile" (hardcoded for testing)
- **Device**: Android 9 (API Level 28)
- **Network**: WiFi connected, validated

### JWT Generation
- **Secret**: `prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e`
- **Algorithm**: HS256
- **Expiry**: 3600 seconds (1 hour)
- **Clock Skew Handling**: `iat` set to 60 seconds in past

### Retry Logic
- **Max Retries**: 3 attempts
- **Circuit Breaker**: Opens after 5 consecutive failures
- **Current State**: Circuit breaker OPEN (blocking all requests)

---

## 🐛 Specific Error Messages

### Error 1: Expiration Claim Failed
```
"error": "\"exp\" claim timestamp check failed"
```
**Possible Causes**:
- Server clock ahead of device clock
- Expiration validation too strict
- Timestamp format mismatch

### Error 2: Signature Verification Failed
```
"error": "signature verification failed"
```
**Possible Causes**:
- Secret mismatch
- Encoding issue
- Algorithm mismatch

### Error 3: Circuit Breaker Open
```
🔴 Circuit breaker OPENED - 6 consecutive failures (threshold: 5)
```
**Cause**: Consecutive authentication failures
**Impact**: All API calls blocked until app restart

---

## 🔧 Recommended Fixes

### For Business Engine Team:

1. **Immediate**: Verify JWT secret configuration matches mobile app
2. **Immediate**: Review and fix expiration timestamp validation
3. **High Priority**: Check clock synchronization and skew tolerance
4. **High Priority**: Review authentication middleware error handling
5. **Medium Priority**: Add detailed logging for JWT validation failures
6. **Medium Priority**: Review circuit breaker configuration (if applicable)

### For Mobile App Team (After Business Engine Fixes):

1. **Verify**: Test with fixed Business Engine
2. **Monitor**: Watch for authentication success
3. **Update**: Adjust token expiry if Business Engine requires different duration
4. **Enhance**: Add better error recovery for authentication failures

---

## 📞 Contact Information

**Mobile App Logs Location**: `artifacts/logs/`
**Test Logs**: `test_log.txt`, `test_log_retry.txt`, `test_log_retry_2.txt`
**API Response Artifacts**: `artifacts/be_health.json`, `artifacts/be_transcribe.json`, `artifacts/be_vend.json`

**Key Timestamps for Business Engine Log Search**:
- 2025-12-01 05:06:13 UTC - Balance check failure
- 2025-12-01 06:38:20 UTC - Circuit breaker activation
- 2025-12-01 13:46:33 UTC - Latest test run failure

**Request IDs to Search**:
- `req_1764554772941` - Balance check request
- `req_1764560297269` - Failed balance check with circuit breaker
- `req_1764560300736` - Token vending attempt

---

## ✅ Next Steps

1. **Business Engine Team**: Review this report and investigate JWT validation issues
2. **Business Engine Team**: Verify configuration and fix authentication problems
3. **Business Engine Team**: Test with provided sample JWT token
4. **Mobile App Team**: Wait for Business Engine fixes
5. **Mobile App Team**: Retest integration after fixes
6. **Both Teams**: Coordinate on any required configuration changes

---

## 📎 Attachments

This report should be shared with:
- Business Engine development team
- TTTranscribe integration team (if separate)
- DevOps/Infrastructure team (for clock sync checks)

**Note**: All secrets in this document are test secrets and should be changed before production deployment.

---

**End of Report**

