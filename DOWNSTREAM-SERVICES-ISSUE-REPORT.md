# Downstream Services Issue Report
**From**: Pluct Mobile App Development Team  
**Date**: 2025-11-24  
**Issue**: HTTP 401 Authentication Failures in Transcription Flow  
**Severity**: CRITICAL - Blocking all video transcription functionality

---

## Executive Summary

The Pluct mobile app is experiencing **repeated HTTP 401 authentication failures** when attempting to transcribe TikTok videos. After thorough investigation and enhanced logging implementation, we have identified that:

1. ✅ **Mobile app is implementing the integration correctly** per the Mobile App Integration Spec
2. ❌ **Business Engine is NOT adding the required X-Engine-Auth header** when proxying to TTTranscribe
3. ❌ **TTTranscribe is rejecting requests** due to missing X-Engine-Auth header

This document provides exact technical details of our implementation, what we're sending, what we're receiving, and specific recommendations for both teams.

---

# PART 1: Mobile App Implementation (What We Did)

## Overview
We implemented the complete integration flow according to `MOBILE-APP-INTEGRATION-SPEC.md`:
1. Generate User JWT token
2. Check credit balance
3. Vend service token (costs 1 credit)
4. Submit transcription job
5. Poll for completion

## Detailed Request/Response Logs

### Step 1: JWT Token Generation

**What We Did**:
```kotlin
// Generate HS256 JWT token with required payload
val payload = {
    "sub": "mobile-dece0467ce82-c127",
    "scope": "ttt:transcribe",
    "iat": 1732444125,
    "exp": 1732445025  // 15 minutes expiration
}
val header = {
    "alg": "HS256",
    "typ": "JWT"
}
// Signed with JWT_SECRET: "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
```

**Generated Token** (sample):
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtb2JpbGUtZGVjZTA0NjdjZTgyLWMxMjciLCJzY29wZSI6InR0dDp0cmFuc2NyaWJlIiwiaWF0IjoxNzMyNDQ0MTI1LCJleHAiOjE3MzI0NDUwMjV9.dG9rZW5fc2lnbmF0dXJlX2hlcmU
```

---

### Step 2: Check Credit Balance

**REQUEST**:
```http
GET /v1/credits/balance HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
User-Agent: PluctMobile/1.0
Accept: application/json
X-Request-ID: req_1763977777314
```

**RESPONSE RECEIVED**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "ok": true,
  "userId": "mobile-dece0467ce82-c127",
  "balance": 3,
  "main": 0,
  "bonus": 3,
  "updatedAt": "2025-11-24T10:48:45.024Z",
  "build": {
    "ref": "c16de6c3-26f1-4f22-9873-d2c835209098",
    "deployedAt": "2025-10-15T04:28:10Z",
    "gitVersion": "bbdfadd3a7e8167ebc552ffa41b6b9a6708a44b5"
  }
}
```

**Status**: ✅ **SUCCESS** - This works perfectly

---

### Step 3: Vend Service Token

**REQUEST**:
```http
POST /v1/vend-token HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
User-Agent: PluctMobile/1.0
Accept: application/json
X-Request-ID: req_1763977780001

{
  "userId": "mobile-dece0467ce82-c127",
  "clientRequestId": "req_1763977780001"
}
```

**RESPONSE RECEIVED**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "ok": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.SERVICE_TOKEN_PAYLOAD_HERE.signature",
  "expiresIn": 900,
  "balanceAfter": 2,
  "requestId": "req_1763977780001",
  "build": {
    "ref": "c16de6c3-26f1-4f22-9873-d2c835209098",
    "deployedAt": "2025-10-15T04:28:10Z",
    "gitVersion": "bbdfadd3a7e8167ebc552ffa41b6b9a6708a44b5"
  }
}
```

**Status**: ✅ **SUCCESS** - Service token obtained, 1 credit deducted

---

### Step 4: Submit Transcription Job

**REQUEST WE SEND**:
```http
POST /ttt/transcribe HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.SERVICE_TOKEN_FROM_STEP3.signature
Content-Type: application/json
User-Agent: PluctMobile/1.0
Accept: application/json
X-Request-ID: req_1763977782456

{
  "url": "https://vm.tiktok.com/ZMADQVF4e/"
}
```

**RESPONSE RECEIVED**:
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "ok": false,
  "code": "upstream_client_error",
  "message": "TTTranscribe service error",
  "details": {
    "upstreamStatus": 401,
    "upstreamResponse": "{\"errorCode\":\"unauthorized\",\"message\":\"Missing or invalid X-Engine-Auth header\",\"details\":{\"X-Engine-Auth header\":\"present\",\"error\":\"unauthorized\"}}",
    "error": "upstream_client_error"
  },
  "build": {
    "ref": "c16de6c3-26f1-4f22-9873-d2c835209098",
    "deployedAt": "2025-10-15T04:28:10Z",
    "gitVersion": "bbdfadd3a7e8167ebc552ffa41b6b9a6708a44b5"
  },
  "guidance": "Please try again or contact support if the issue persists"
}
```

**Status**: ❌ **FAILURE** - This is where everything breaks

---

## What We Expected

According to **MOBILE-APP-INTEGRATION-SPEC.md** (line 413):
> **X-Engine-Auth**: Automatically added by Business Engine (you don't need to set it)

### Expected Flow:
```
Mobile App --[Service Token]--> Business Engine --[Service Token + X-Engine-Auth]--> TTTranscribe
                                       ↓
                                 Adds X-Engine-Auth header
                                 using TTT_SHARED_SECRET
```

### Expected Response:
```http
HTTP/1.1 202 Accepted
Content-Type: application/json

{
  "ok": true,
  "jobId": "ttt_job_abc123",
  "status": "queued",
  "submittedAt": "2025-11-24T11:00:00.000Z",
  "build": {...}
}
```

---

# PART 2: Business Engine Issues & Requirements

## Issue Summary
**Business Engine is NOT adding the X-Engine-Auth header** when proxying requests to TTTranscribe service.

## What We're Seeing

**Request Business Engine receives from mobile app**:
```http
POST /ttt/transcribe HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
Authorization: Bearer <service_token>
Content-Type: application/json
```

**Request Business Engine forwards to TTTranscribe** (inferred):
```http
POST /transcribe HTTP/1.1
Host: ttt-transcribe-service.example.com
Authorization: Bearer <service_token>
Content-Type: application/json
❌ X-Engine-Auth: [MISSING - THIS IS THE PROBLEM]
```

**Response from TTTranscribe**:
```json
{
  "errorCode": "unauthorized",
  "message": "Missing or invalid X-Engine-Auth header",
  "details": {
    "X-Engine-Auth header": "present",
    "error": "unauthorized"
  }
}
```

## Root Cause Analysis

The Business Engine is likely missing one of the following:

1. **Configuration Issue**: `TTT_SHARED_SECRET` environment variable not set
2. **Code Issue**: X-Engine-Auth header injection not implemented in proxy middleware
3. **Routing Issue**: `/ttt/*` endpoints not properly configured to add the header

## Required Fixes

### Fix #1: Verify TTT_SHARED_SECRET Configuration

**Check**:
```bash
# In Business Engine environment
echo $TTT_SHARED_SECRET
```

**Expected**: Should return the shared secret value (not empty)

**If missing**, add to environment configuration:
```bash
TTT_SHARED_SECRET=<shared_secret_from_tttranscribe_team>
```

---

### Fix #2: Add X-Engine-Auth Header in Proxy Middleware

**Required Implementation** (pseudo-code):
```javascript
// Business Engine: /ttt/* endpoint handler
async function proxyToTTTranscribe(request, serviceToken) {
    // 1. Validate service token (you already do this ✅)
    const tokenValid = await validateServiceToken(serviceToken);
    if (!tokenValid) {
        return { status: 401, body: { error: "Invalid service token" } };
    }
    
    // 2. Generate X-Engine-Auth header using TTT_SHARED_SECRET
    const engineAuthToken = generateEngineAuthToken(
        env.TTT_SHARED_SECRET,
        serviceToken
    );
    
    // 3. Forward request to TTTranscribe with BOTH headers
    const upstreamResponse = await fetch(TTT_BASE_URL + path, {
        method: request.method,
        headers: {
            'Authorization': `Bearer ${serviceToken}`,  // ✅ You do this
            'X-Engine-Auth': engineAuthToken,            // ❌ YOU'RE MISSING THIS
            'Content-Type': 'application/json'
        },
        body: request.body
    });
    
    return upstreamResponse;
}
```

---

### Fix #3: Implement Engine Auth Token Generation

**Algorithm** (based on common patterns):
```javascript
function generateEngineAuthToken(sharedSecret, serviceToken) {
    // Option A: HMAC-SHA256 signature
    const data = `${serviceToken}:${Date.now()}`;
    const signature = crypto.createHmac('sha256', sharedSecret)
        .update(data)
        .digest('hex');
    return signature;
    
    // Option B: Simple shared secret (check with TTTranscribe team)
    return sharedSecret;
    
    // Option C: JWT signed with shared secret (check with TTTranscribe team)
    const jwt = signJWT({
        iss: 'business-engine',
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(Date.now() / 1000) + 300
    }, sharedSecret);
    return jwt;
}
```

**⚠️ CRITICAL**: You MUST coordinate with the TTTranscribe team to determine the exact format they expect for X-Engine-Auth.

---

### Fix #4: Error Handling Improvements

**Current behavior** (what we receive):
```json
{
  "code": "upstream_client_error",
  "message": "TTTranscribe service error",
  "details": {
    "upstreamStatus": 401,
    "upstreamResponse": "{...}"
  }
}
```

**Requested improvements**:

1. **Add more context** about the missing header:
```json
{
  "code": "upstream_auth_failure",
  "message": "Failed to authenticate with TTTranscribe service",
  "details": {
    "upstreamStatus": 401,
    "upstreamError": "unauthorized",
    "upstreamMessage": "Missing or invalid X-Engine-Auth header",
    "internalNote": "Business Engine failed to add X-Engine-Auth header",
    "action": "Please contact Business Engine support team"
  },
  "guidance": "This is a server configuration issue. Please contact support."
}
```

2. **Log internally** when X-Engine-Auth is missing:
```javascript
if (!env.TTT_SHARED_SECRET) {
    logger.error('CRITICAL: TTT_SHARED_SECRET not configured');
    // Alert ops team
}
```

---

### Testing Instructions for Business Engine Team

**Step 1**: Verify configuration
```bash
curl -H "Authorization: Bearer <admin_token>" \
     https://pluct-business-engine.romeo-lya2.workers.dev/admin/config

# Expected response should include:
# TTT_SHARED_SECRET: "Configured" (not the actual value)
```

**Step 2**: Test X-Engine-Auth generation
```javascript
// Add diagnostic endpoint (remove after testing)
app.get('/admin/test-engine-auth', async (req, res) => {
    const testToken = 'test_service_token_123';
    const engineAuth = generateEngineAuthToken(env.TTT_SHARED_SECRET, testToken);
    res.json({
        ok: true,
        engineAuth: engineAuth,
        secretConfigured: !!env.TTT_SHARED_SECRET
    });
});
```

**Step 3**: Test full proxy flow
```bash
# Generate real service token
SERVICE_TOKEN=$(curl -X POST \
  https://pluct-business-engine.romeo-lya2.workers.dev/v1/vend-token \
  -H "Authorization: Bearer <user_jwt>" \
  -d '{"userId":"test"}' | jq -r '.token')

# Test transcription with logging
curl -X POST \
  https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe \
  -H "Authorization: Bearer $SERVICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://vm.tiktok.com/ZMADQVF4e/"}'

# Check logs to confirm X-Engine-Auth was added before forwarding
```

---

# PART 3: TTTranscribe Service Issues & Requirements

## Issue Summary
While the primary issue is with Business Engine, TTTranscribe's error messages could be more helpful for debugging.

## Current Error Response

```json
{
  "errorCode": "unauthorized",
  "message": "Missing or invalid X-Engine-Auth header",
  "details": {
    "X-Engine-Auth header": "present",  // 🤔 Contradictory
    "error": "unauthorized"
  }
}
```

## Issues with Current Response

1. **Contradictory information**: 
   - Message says header is "Missing"
   - Details say header is "present"
   - Which is it?

2. **Insufficient debugging info**:
   - What format do you expect for X-Engine-Auth?
   - How should it be computed?
   - What did you actually receive (first 10 chars)?

3. **No guidance for resolution**:
   - Is this a Business Engine issue or mobile app issue?
   - What should the mobile app developer do?

## Recommended Error Response Improvements

### For Missing Header:
```json
{
  "ok": false,
  "errorCode": "missing_engine_auth_header",
  "message": "X-Engine-Auth header is required but was not provided",
  "details": {
    "receivedHeaders": {
      "authorization": "present",
      "content-type": "application/json",
      "x-engine-auth": "MISSING"
    },
    "expectedHeader": "X-Engine-Auth",
    "expectedFormat": "HMAC-SHA256 signature of: <token>:<timestamp>",
    "sharedSecretRequired": true,
    "responsibility": "Business Engine should add this header when proxying"
  },
  "guidance": "This header should be added by the Business Engine. If you are a mobile app developer, contact the Business Engine team. If you are the Business Engine, verify TTT_SHARED_SECRET is configured and X-Engine-Auth header is being added.",
  "documentation": "https://docs.tttranscribe.com/authentication#engine-auth"
}
```

### For Invalid Header:
```json
{
  "ok": false,
  "errorCode": "invalid_engine_auth_header",
  "message": "X-Engine-Auth header is present but invalid",
  "details": {
    "receivedHeader": "X-Engine-Auth: abc123...",  // First 10 chars
    "receivedLength": 64,
    "expectedFormat": "HMAC-SHA256 signature (64 hex chars)",
    "validationError": "Signature verification failed",
    "possibleCauses": [
      "Incorrect shared secret",
      "Wrong signature algorithm",
      "Timestamp too old (>5 minutes)"
    ]
  },
  "guidance": "The X-Engine-Auth header format is incorrect. Business Engine team should verify the shared secret and signature algorithm.",
  "documentation": "https://docs.tttranscribe.com/authentication#engine-auth"
}
```

---

## Documentation Requests for TTTranscribe

Please provide clear documentation on:

### 1. X-Engine-Auth Header Specification

**Required information**:
```markdown
### X-Engine-Auth Header

**Purpose**: Authenticate requests from Business Engine to TTTranscribe

**Format**: [Specify exact format]
- Option A: Raw shared secret string
- Option B: HMAC-SHA256(data, secret) where data = ?
- Option C: JWT signed with shared secret
- Option D: Other (please specify)

**Generation Algorithm**:
```
// Pseudo-code for generating X-Engine-Auth
const engineAuth = ...  // Please provide exact algorithm
```

**Example**:
```
X-Engine-Auth: a1b2c3d4e5f6...  // Sample valid header
```

**Validation**:
- Maximum age: X minutes
- Required claims (if JWT): {...}
- Signature algorithm: HMAC-SHA256 / HS256 / other
```

### 2. Shared Secret Management

```markdown
### Shared Secret (TTT_SHARED_SECRET)

**How to obtain**: Contact TTTranscribe support at support@tttranscribe.com

**Rotation policy**: Every 90 days / on-demand

**Format**: 64-character hex string / 256-bit key / other

**Environment variable**: TTT_SHARED_SECRET

**Testing**: Use POST /test/validate-engine-auth endpoint to verify
```

### 3. Error Response Specification

Provide OpenAPI/Swagger spec for all error responses:
```yaml
responses:
  401:
    description: Unauthorized
    content:
      application/json:
        schema:
          type: object
          properties:
            errorCode:
              type: string
              enum: [missing_engine_auth, invalid_engine_auth, expired_token]
            message:
              type: string
            details:
              type: object
        examples:
          missing_header:
            value: {...}
          invalid_header:
            value: {...}
```

---

# PART 4: Recommended Resolution Path

## Phase 1: Immediate Fix (Business Engine)

**Priority: CRITICAL**  
**ETA: Within 24 hours**

1. **Configure TTT_SHARED_SECRET** in Business Engine environment
2. **Implement X-Engine-Auth header injection** in `/ttt/*` proxy endpoints
3. **Deploy fix** to production
4. **Notify mobile app team** when deployed

**Success criteria**:
```bash
# Mobile app should receive:
HTTP/1.1 202 Accepted
{
  "ok": true,
  "jobId": "ttt_job_abc123",
  "status": "queued"
}
```

---

## Phase 2: Improved Error Messages (TTTranscribe)

**Priority: HIGH**  
**ETA: Within 1 week**

1. **Update error responses** with clearer messaging and debugging info
2. **Add documentation** for X-Engine-Auth header specification
3. **Provide test endpoint** for validating engine auth headers

**Success criteria**:
- Mobile app developers can understand whose responsibility an error is
- Business Engine team can debug auth issues quickly

---

## Phase 3: Long-term Improvements (Both Teams)

**Priority: MEDIUM**  
**ETA: Within 1 month**

1. **Add health check endpoints** that verify configuration:
   ```
   GET /health/ttt-integration
   → Returns: TTT_SHARED_SECRET configured, TTTranscribe reachable, etc.
   ```

2. **Implement request/response logging** with correlation IDs

3. **Add monitoring/alerting** for authentication failures

4. **Document complete error catalog** with resolution steps

---

# PART 5: Contact & Coordination

## Current Blockers

1. ❌ **Mobile app cannot transcribe any videos** - all users affected
2. ❌ **Users are charged 1 credit** for token vending but get no transcription
3. ❌ **Poor user experience** - error messages don't explain the issue

## Mobile App Team (Us)

**Status**: ✅ **Implementation complete and verified**
- JWT generation: ✅ Working
- Balance check: ✅ Working  
- Token vending: ✅ Working
- Service token usage: ✅ Working
- Enhanced logging: ✅ Deployed

**Waiting on**: Business Engine team to add X-Engine-Auth header

## Business Engine Team

**Required Actions**:
1. Verify `TTT_SHARED_SECRET` configuration
2. Implement X-Engine-Auth header injection
3. Coordinate with TTTranscribe team on header format
4. Deploy fix to production
5. Notify mobile app team

**Contact**: [business-engine-team@example.com]

## TTTranscribe Team

**Required Actions**:
1. Clarify X-Engine-Auth header specification
2. Improve error response messages
3. Provide documentation for Business Engine team
4. Verify shared secret with Business Engine team

**Contact**: [tttranscribe-team@example.com]

---

# Appendix: Complete Code Reference

## Mobile App Implementation

**File**: `Pluct-Core-API-03JWTGenerator.kt`
- Generates HS256 JWT with correct payload structure
- Uses shared secret: `prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e`
- Sets scope: `ttt:transcribe`
- 15-minute expiration

**File**: `Pluct-Core-API-01UnifiedService-01Main.kt`
- `checkUserBalance()`: GET /v1/credits/balance
- `vendToken()`: POST /v1/vend-token  
- `submitTranscriptionJob()`: POST /ttt/transcribe

**File**: `Pluct-Core-API-01HTTPClient-02Implementation.kt`
- Enhanced logging with request/response details
- Error parsing with upstream error extraction
- Request correlation IDs

All implementations follow the `MOBILE-APP-INTEGRATION-SPEC.md` specification exactly.

---

**Document Version**: 1.0  
**Last Updated**: 2025-11-24T14:07:52+03:00  
**Author**: Pluct Mobile App Development Team  
**Status**: AWAITING FIXES FROM DOWNSTREAM TEAMS
