# Pluct Business Engine - Mobile App Integration Specification

**Version**: 1.0  
**Last Updated**: 2025-11-30  
**Base URL**: `https://pluct-business-engine.romeo-lya2.workers.dev`

This document provides comprehensive technical specifications for mobile app clients to integrate with the Pluct Business Engine and communicate with downstream services like TTTranscribe.

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [API Endpoints](#api-endpoints)
4. [Complete Integration Flow](#complete-integration-flow)
5. [Error Handling](#error-handling)
6. [Edge Cases & Negative Scenarios](#edge-cases--negative-scenarios)
7. [Best Practices](#best-practices)
8. [Code Examples](#code-examples)

---

## Overview

### Architecture Flow

```
Mobile App → Business Engine → TTTranscribe Service
     ↓              ↓                    ↓
  JWT Auth    Credit Check         X-Engine-Auth
     ↓              ↓                    ↓
  Token Vend   Deduct Credit      Process Video
     ↓              ↓                    ↓
  Submit Job   Return Job ID      Return Transcript
     ↓              ↓                    ↓
  Poll Status  Extract Result     Final Transcript
```

### Key Concepts

- **User JWT**: Long-lived token for user authentication (obtained from your auth system)
- **Service Token**: Short-lived token (15 minutes) obtained via `/v1/vend-token` (costs 1 credit)
- **Credit System**: Dual-wallet system (main + bonus credits)
- **Idempotency**: Use `clientRequestId` to prevent duplicate charges
- **Transcript Extraction**: Business Engine handles multiple response formats automatically

---

### 2025-11-30 Monetization and Multi-tenant Update

- **User identity must be device-bound**: continue sending unique per-device userIds (e.g., `mobile-<device-id>`). Do not reuse across devices; web clients should use `web-<fingerprint>`; partners should use `partner-<org>-<client-id>`. This keeps balances isolated per tenant/client.
- **Vend token is prepaid session only**: `/v1/vend-token` returns a 15-minute token plus `balanceAfter`; it does not deduct credits. Deduction happens when `/ttt/transcribe` places a hold, then finalizes on webhook/status completion.
- **Mandatory holds**: `/ttt/transcribe` now enforces a credit hold before forwarding to TTTranscribe. If a hold fails, you receive `402 insufficient_credits`. Do not retry without topping up.
- **Backoff on repeated hold failures**: Three failed holds inside 60s trigger a 429 with `retryAfterSeconds`; wait before retrying and fix the underlying issue (credits/token expiry).
- **Balance refresh rule**: After any job completes (status `done/completed`), refetch `/v1/credits/balance` to show the final debited amount (holds may refund if the final cost is lower).
- **Token hygiene**: Vend a fresh token per submission; do not reuse tokens across different URLs or after an error. If a token nears `expiresIn`, vend again.

---

IMPORTANT OPERATOR NOTE (NOV 29 2025)

- I ran a direct connectivity test from the repository using `test-simple.js` (it uses the `.env.local` file in the repo). The test attempted the configured `TTT_BASE` host and tried `/health` and `/transcribe` endpoints.
- Result: the configured host returned HTML (Hugging Face landing page) and `404` for both `/health` and `/transcribe`. This means the URL in `TTT_BASE` / `BASE_URL` points to a public Hugging Face Space or a site that does not expose `/transcribe` and `/status` endpoints with the contract the Business Engine expects.

Consequence for mobile app developers: the Business Engine (and the `test-simple.js` script) expect an upstream service that implements the following contract:

- POST /transcribe
    - Request: { "url": "https://vm.tiktok.com/..../", "clientRequestId": "optional" }
    - Response (202 or 200): { id: "<jobId>", status: "queued" }
- GET /status/:id
    - Response (200): { status: "queued|processing|completed|failed", result: { transcription: "..." } }

If your upstream provider is a Hugging Face Space you control, either:

1. Implement the above `/transcribe` and `/status` routes in the Space (recommended), or
2. Run a small adapter service that translates the Business Engine's `/transcribe` and `/status` contract to whatever endpoints the Space exposes (for example `/run/predict` or the HF Inference API). The Business Engine must talk to a service that understands the contract above.

Do NOT change the mobile app flow — the mobile app must always talk to the Business Engine and never directly to the Hugging Face Space (the Business Engine supplies the private `X-Engine-Auth` header).

If you'd like, I can implement an optional adapter inside the Business Engine that detects when `TTT_BASE` is a Hugging Face Space (hostname contains `hf.space` or `huggingface.co`) and attempts to translate our `/transcribe` request into common HF Space endpoints as a best-effort compatibility mode. Confirm if you want that and which Space model/endpoint to target.

---

## Authentication

### User JWT Token

**Purpose**: Authenticate user identity and authorize credit operations

**Requirements**:
- **Algorithm**: HS256
- **Secret**: Must match `ENGINE_JWT_SECRET` configured in Business Engine
- **Payload Structure**:
  ```json
  {
    "sub": "user_id_here",
    "scope": "ttt:transcribe",
    "iat": 1234567890,
    "exp": 1234567890
  }
  ```
- **Scope**: Must include `ttt:transcribe` for transcription endpoints
- **Expiration**: Set appropriate expiration (recommended: 1 hour for mobile apps)

**Usage**: Include in `Authorization` header:
```
Authorization: Bearer <user_jwt_token>
```

### Service Token (Short-lived)

**Purpose**: Authenticate with TTTranscribe service endpoints

**Obtained Via**: `POST /v1/vend-token` (no credit deducted at vend; hold is taken at `/ttt/transcribe`)

**Characteristics**:
- **Lifetime**: 15 minutes (900 seconds)
- **Scope**: `ttt:transcribe` (automatically set)
- **Cost**: 0 credits at vend time; a hold and final debit occur when `/ttt/transcribe` is called
- **Idempotent**: Can use `clientRequestId` to prevent duplicate charges
- **Balance Echo**: Response now includes `balanceAfter` to refresh UI with the last-known balance after vend

**Usage**: Include in `Authorization` header for `/ttt/*` endpoints:
```
Authorization: Bearer <service_token>
```

---

## API Endpoints

### 1. Health Check

**Endpoint**: `GET /health`

**Authentication**: None required

**Request**:
```http
GET /health HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
```

**Success Response (200)**:
```json
{
  "status": "ok",
  "uptimeSeconds": 3600,
  "version": "1.0.0",
  "build": {
    "ref": "c16de6c3-26f1-4f22-9873-d2c835209098",
    "deployedAt": "2025-10-15T04:28:10Z",
    "gitVersion": "8a58865..."
  },
  "configuration": {
    "ENGINE_JWT_SECRET": "Configured",
    "TTT_SHARED_SECRET": "Configured",
    "TTT_BASE": "Configured"
  }
}
```

**Use Cases**:
- Verify service availability before making requests
- Check configuration status
- Monitor deployment version

---

### 2. Get Credit Balance

**Endpoint**: `GET /v1/credits/balance`

**Authentication**: User JWT Bearer token required

**Request**:
```http
GET /v1/credits/balance HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
Authorization: Bearer <user_jwt_token>
```

**Success Response (200)**:
```json
{
  "ok": true,
  "userId": "mobile",
  "balance": 6,
  "main": 0,
  "bonus": 6,
  "updatedAt": "2025-11-24T04:51:27.372Z",
  "build": {
    "ref": "...",
    "deployedAt": "...",
    "gitVersion": "..."
  }
}
```

**Response Fields**:
- `balance`: Total credits (main + bonus) - for backward compatibility
- `main`: Main wallet balance (purchased credits)
- `bonus`: Bonus wallet balance (promotional credits)
- `userId`: User identifier extracted from JWT

**Error Responses**:
- `401 Unauthorized`: Invalid or missing JWT token
- `500 Internal Server Error`: Database or system error

**Use Cases**:
- Display credit balance to user
- Check if user has sufficient credits before vending token
- Show breakdown of main vs bonus credits

---

### 3. Vend Service Token

**Endpoint**: `POST /v1/vend-token`

**Authentication**: User JWT Bearer token required

**Cost**: 1 credit (deducted atomically)

**Request**:
```http
POST /v1/vend-token HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
Authorization: Bearer <user_jwt_token>
Content-Type: application/json

{
  "userId": "mobile",
  "clientRequestId": "unique_request_id_optional"
}
```

**Request Body Fields**:
- `userId`: User identifier (must match JWT `sub` claim)
- `clientRequestId`: (Optional) Unique request ID for idempotency

**Success Response (200)**:
```json
{
  "ok": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900,
  "balanceAfter": 5,
  "requestId": "req_1640995200000",
  "build": {
    "ref": "...",
    "deployedAt": "...",
    "gitVersion": "..."
  }
}
```

**Response Fields**:
- `token`: Short-lived service token (15 minutes)
- `expiresIn`: Token expiration in seconds (900)
- `balanceAfter`: New total balance after deduction
- `requestId`: Request identifier (your `clientRequestId` or generated)

**Error Responses**:

**402 Payment Required** (Insufficient Credits):
```json
{
  "ok": false,
  "code": "insufficient_credits",
  "message": "Insufficient credits",
  "details": {
    "balance": 0,
    "required": 1
  },
  "build": {
    "ref": "...",
    "deployedAt": "...",
    "gitVersion": "..."
  },
  "guidance": "Add credits to your account to continue"
}
```

**401 Unauthorized** (Invalid Token):
```json
{
  "ok": false,
  "code": "unauthorized",
  "message": "Invalid or expired token",
  "details": {
    "error": "signature verification failed"
  },
  "build": {...},
  "guidance": "Provide a valid authentication token"
}
```

**429 Too Many Requests** (Rate Limited):
```json
{
  "ok": false,
  "code": "rate_limit_exceeded",
  "message": "Rate limit exceeded",
  "details": {
    "limit": 10,
    "resetTime": "2025-11-24T05:51:27.372Z"
  },
  "build": {...},
  "guidance": "Please wait before making another request"
}
```

**Use Cases**:
- Obtain service token before submitting transcription
- Check credit balance before attempting transcription
- Handle insufficient credits gracefully

**Important Notes**:
- **Idempotency**: If you provide the same `clientRequestId` within 1 hour, you'll get the cached response without deducting credits again
- **Rate Limiting**: Maximum 10 requests per hour per user
- **Atomic Deduction**: Credit is deducted atomically - no race conditions possible
- **Priority Spending**: Bonus credits are spent first, then main credits

---

### 4. Submit Transcription Job

**Endpoint**: `POST /ttt/transcribe`

**Authentication**: Service token (from `/v1/vend-token`) Bearer token required

**Timeout**: 10 minutes (600,000ms)

**Request**:
```http
POST /ttt/transcribe HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
Authorization: Bearer <service_token>
Content-Type: application/json

{
  "url": "https://vm.tiktok.com/ZMATN7F41/",
  "clientRequestId": "unique_request_id_optional"
}
```

**Request Body Fields**:
- `url`: TikTok video URL (required)
- `clientRequestId`: (Optional) Unique request ID for idempotency

**Valid TikTok URL Formats**:
- `https://vm.tiktok.com/[ID]/` - Short format (recommended)
- `https://www.tiktok.com/@user/video/[ID]/` - Full format
- `https://tiktok.com/@user/video/[ID]/` - Alternative format

**Success Response (202 Accepted)**:
```json
{
  "ok": true,
  "jobId": "ttt_job_abc123",
  "status": "queued",
  "submittedAt": "2025-11-24T04:51:27.372Z",
  "build": {
    "ref": "...",
    "deployedAt": "...",
    "gitVersion": "..."
  }
}
```

**Response Fields**:
- `jobId`: Job identifier for status polling
- `status`: Initial status (usually "queued")
- `submittedAt`: ISO 8601 timestamp

**Error Responses**:

**400 Bad Request** (Invalid URL):
```json
{
  "ok": false,
  "code": "invalid_url",
  "message": "Invalid TikTok URL format",
  "details": {
    "providedUrl": "https://example.com/video",
    "error": "Invalid TikTok URL format. Supported formats: https://vm.tiktok.com/[ID]/ or https://www.tiktok.com/@user/video/[ID]/",
    "expectedFormat": "https://vm.tiktok.com/[ID]/ or https://www.tiktok.com/@user/video/[ID]/"
  },
  "build": {...},
  "guidance": "Please provide a valid TikTok URL in the correct format"
}
```

**401 Unauthorized** (Invalid/Expired Token):
```json
{
  "ok": false,
  "code": "unauthorized",
  "message": "Invalid or expired token",
  "details": {
    "error": "token expired"
  },
  "build": {...},
  "guidance": "Provide a valid authentication token"
}
```

**403 Forbidden** (Insufficient Scope):
```json
{
  "ok": false,
  "code": "forbidden",
  "message": "Insufficient scope",
  "details": {
    "requiredScope": "ttt:transcribe",
    "providedScope": "read"
  },
  "build": {...},
  "guidance": "Token must have ttt:transcribe scope"
}
```

**502 Bad Gateway** (TTTranscribe Service Error):
```json
{
  "ok": false,
  "code": "upstream_error",
  "message": "TTTranscribe service error",
  "details": {
    "upstreamStatus": 500,
    "upstreamResponse": "Internal server error"
  },
  "build": {...},
  "guidance": "Please try again or contact support if the issue persists"
}
```

**Use Cases**:
- Submit TikTok video for transcription
- Get job ID for status polling
- Handle URL validation errors

**Important Notes**:
- **Idempotency**: Same `clientRequestId` returns cached response within 1 hour
- **Timeout**: Request can take up to 10 minutes (for long videos)
- **URL Validation**: Business Engine validates URL format before forwarding
- **X-Engine-Auth**: Automatically added by Business Engine (you don't need to set it)

---

### 5. Check Transcription Status

**Endpoint**: `GET /ttt/status/:id`

**Authentication**: Service token (from `/v1/vend-token`) Bearer token required

**Timeout**: 30 seconds (default)

**Request**:
```http
GET /ttt/status/{jobId} HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
Authorization: Bearer <service_token>
```

**URL Parameters**:
- `id`: Job ID from transcription submission response

**Success Response (200)** - Job Queued:
```json
{
  "ok": true,
  "jobId": "ttt_job_abc123",
  "status": "queued",
  "progress": 0,
  "build": {...}
}
```

**Success Response (200)** - Job Processing:
```json
{
  "ok": true,
  "jobId": "ttt_job_abc123",
  "status": "processing",
  "progress": 45,
  "estimatedCompletion": "2025-11-24T05:00:00.000Z",
  "build": {...}
}
```

**Success Response (200)** - Job Completed:
```json
{
  "ok": true,
  "jobId": "ttt_job_abc123",
  "status": "completed",
  "progress": 100,
  "result": {
    "transcription": "Full transcript text here...",
    "confidence": 0.95,
    "language": "en",
    "duration": 30.5,
    "wordCount": 45,
    "speakerCount": 1,
    "audioQuality": "high",
    "processingTime": 225
  },
  "build": {...}
}
```

**Response Fields**:
- `status`: Job status (`queued`, `processing`, `completed`, `failed`)
- `progress`: Progress percentage (0-100)
- `result`: Transcript result object (when completed)
  - `transcription`: Full transcript text (string)
  - `confidence`: Confidence score (0.0-1.0)
  - `language`: Detected language code
  - `duration`: Video duration in seconds
  - Additional fields may be present depending on TTTranscribe response

**Error Responses**:

**404 Not Found** (Job Not Found):
```json
{
  "ok": false,
  "code": "job_not_found",
  "message": "Job not found",
  "details": {
    "jobId": "ttt_job_abc123"
  },
  "build": {...},
  "guidance": "Check the job ID and try again"
}
```

**401 Unauthorized** (Invalid/Expired Token):
```json
{
  "ok": false,
  "code": "unauthorized",
  "message": "Invalid or expired token",
  "details": {...},
  "build": {...},
  "guidance": "Provide a valid authentication token"
}
```

**502 Bad Gateway** (TTTranscribe Service Error):
```json
{
  "ok": false,
  "code": "upstream_error",
  "message": "TTTranscribe service error",
  "details": {
    "upstreamStatus": 500,
    "upstreamResponse": "Service unavailable"
  },
  "build": {...},
  "guidance": "Please try again or contact support if the issue persists"
}
```

**Use Cases**:
- Poll for transcription completion
- Display progress to user
- Extract transcript when completed
- Handle job failures

**Important Notes**:
- **Polling Interval**: Recommended 3-5 seconds between polls
- **Transcript Extraction**: Business Engine automatically extracts transcript from various response formats
- **Result Field**: Always check `result.transcription` for the transcript text
- **Status Values**: `queued` → `processing` → `completed` or `failed`

---

### 6. Get TikTok Metadata

**Endpoint**: `GET /meta?url=<tiktok_url>`

**Authentication**: None required

**Request**:
```http
GET /meta?url=https://vm.tiktok.com/ZMATN7F41/ HTTP/1.1
Host: pluct-business-engine.romeo-lya2.workers.dev
```

**Query Parameters**:
- `url`: TikTok video URL (required, URL-encoded)

**Success Response (200)**:
```json
{
  "title": "TikTok Video Title",
  "author": "username",
  "description": "Video description",
  "duration": 30,
  "handle": "@username",
  "url": "https://vm.tiktok.com/ZMATN7F41/",
  "build": {...}
}
```

**Error Responses**:

**422 Unprocessable Entity** (Non-TikTok URL):
```json
{
  "ok": false,
  "code": "invalid_url",
  "message": "Only TikTok URLs are supported",
  "details": {
    "providedUrl": "https://youtube.com/watch?v=...",
    "supportedDomains": ["tiktok.com", "vm.tiktok.com"]
  },
  "build": {...},
  "guidance": "Please provide a valid TikTok URL"
}
```

**500 Internal Server Error** (Metadata Fetch Failed):
```json
{
  "ok": false,
  "code": "METADATA_FETCH_FAILED",
  "message": "Failed to fetch metadata",
  "details": {
    "error": "Network timeout"
  },
  "build": {...},
  "guidance": "Please try again or contact support if the issue persists"
}
```

**Use Cases**:
- Display video metadata before transcription
- Validate video URL
- Show video title and author

---

## Complete Integration Flow

### Standard Transcription Flow

```
1. Check Credit Balance
   GET /v1/credits/balance
   → Verify user has credits

2. Vend Service Token
   POST /v1/vend-token
   → Get short-lived token (costs 1 credit)
   → Store token and expiration time

3. Submit Transcription
   POST /ttt/transcribe
   → Submit TikTok URL
   → Receive job ID

4. Poll Status
   GET /ttt/status/{jobId}
   → Poll every 3-5 seconds
   → Check status: queued → processing → completed

5. Extract Transcript
   → When status = "completed"
   → Extract result.transcription
   → Display to user
```

### Code Flow Example

```kotlin
// 1. Check balance
val balanceResponse = apiClient.getBalance(userJwt)
if (balanceResponse.balance < 1) {
    showInsufficientCreditsError()
    return
}

// 2. Vend token
val tokenResponse = apiClient.vendToken(userJwt, clientRequestId)
val serviceToken = tokenResponse.token
val tokenExpiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)

// 3. Submit transcription
val transcribeResponse = apiClient.submitTranscription(
    serviceToken, 
    tiktokUrl, 
    clientRequestId
)
val jobId = transcribeResponse.jobId

// 4. Poll for completion
var statusResponse: StatusResponse
do {
    Thread.sleep(3000) // Wait 3 seconds
    statusResponse = apiClient.getStatus(serviceToken, jobId)
    
    if (statusResponse.status == "failed") {
        showError("Transcription failed")
        return
    }
    
    // Check token expiration
    if (System.currentTimeMillis() >= tokenExpiresAt) {
        // Token expired, vend new one
        val newTokenResponse = apiClient.vendToken(userJwt, generateRequestId())
        serviceToken = newTokenResponse.token
        tokenExpiresAt = System.currentTimeMillis() + (newTokenResponse.expiresIn * 1000)
    }
} while (statusResponse.status != "completed")

// 5. Extract transcript
val transcript = statusResponse.result?.transcription
if (transcript != null) {
    displayTranscript(transcript)
} else {
    showError("Transcript not found in response")
}
```

---

## Error Handling

### Error Response Structure

All error responses follow this structure:

```json
{
  "ok": false,
  "code": "error_code",
  "message": "Human-readable error message",
  "details": {
    "additional": "context information"
  },
  "build": {
    "ref": "...",
    "deployedAt": "...",
    "gitVersion": "..."
  },
  "guidance": "Actionable guidance for user"
}
```

### HTTP Status Codes

| Status Code | Meaning | Common Error Codes |
|-------------|---------|-------------------|
| 200 | Success | N/A |
| 202 | Accepted (job queued) | N/A |
| 400 | Bad Request | `invalid_url` |
| 401 | Unauthorized | `unauthorized` |
| 402 | Payment Required | `insufficient_credits` |
| 403 | Forbidden | `forbidden` |
| 404 | Not Found | `job_not_found` |
| 422 | Unprocessable Entity | `invalid_url` |
| 429 | Too Many Requests | `rate_limit_exceeded`, `rate_limited` |
| 500 | Internal Server Error | Various |
| 502 | Bad Gateway | `upstream_error`, `upstream_client_error` |
| 504 | Gateway Timeout | `upstream_timeout` |

### Error Handling Strategy

1. **Check `ok` field**: Always check if `ok` is `true` or `false`
2. **Read `code` field**: Use error code for programmatic handling
3. **Display `message`**: Show user-friendly message
4. **Check `guidance`**: Provide actionable guidance to user
5. **Log `details`**: Log detailed error information for debugging

### Common Error Scenarios

#### Insufficient Credits (402)

**When**: `/ttt/transcribe` cannot place a hold (no credits or hold blocked)

**Handling**:
```kotlin
if (error.code == "insufficient_credits") {
    showMessage("You need more credits to start this transcription.")
    // Refresh balance from /v1/credits/balance to show up-to-date amounts
    val latestBalance = fetchBalance()
    renderBalance(latestBalance)
    navigateToPurchaseCreditsScreen()
}
```

#### Invalid URL (400)

**When**: URL format is invalid or not a TikTok URL

**Handling**:
```kotlin
if (error.code == "invalid_url") {
    val expectedFormat = error.details.expectedFormat
    showMessage("Invalid URL format. Expected: $expectedFormat")
    highlightUrlInputField()
}
```

#### Token Expired (401)

**When**: Service token expired during status polling

**Handling**:
```kotlin
if (error.code == "unauthorized" && error.message.contains("expired")) {
    // Vend new token and retry
    val newToken = vendNewToken()
    retryRequestWithNewToken(newToken)
}
```

#### Hold Backoff (429)

**When**: Multiple failed hold attempts within 60 seconds; Business Engine returns `rate_limited` with `retryAfterSeconds`.

**Handling**:
```kotlin
if (error.code == "rate_limited") {
    val retryAfter = error.details.retryAfterSeconds ?: 15
    showMessage("Too many attempts. Please wait $retryAfter seconds and try again.")
    scheduleRetryAfter(retryAfter)
}
```

#### Rate Limited (429)

**When**: Too many token vending requests

**Handling**:
```kotlin
if (error.code == "rate_limit_exceeded") {
    val resetTime = error.details.resetTime
    showMessage("Rate limit exceeded. Try again after $resetTime")
    disableVendTokenButton()
    scheduleReEnable(resetTime)
}
```

#### Job Not Found (404)

**When**: Job ID doesn't exist or was deleted

**Handling**:
```kotlin
if (error.code == "job_not_found") {
    showMessage("Transcription job not found. Please submit a new request.")
    resetTranscriptionState()
}
```

#### Upstream Service Error (502/504)

**When**: TTTranscribe service is unavailable or timed out

**Handling**:
```kotlin
if (error.code == "upstream_error" || error.code == "upstream_timeout") {
    showMessage("Transcription service temporarily unavailable. Please try again later.")
    enableRetryButton()
}
```

---

## Edge Cases & Negative Scenarios

### 1. Token Expiration During Polling

**Scenario**: Service token expires while polling for status

**Solution**:
- Check token expiration before each poll
- If expired, vend new token and continue polling
- Use same job ID (job persists even if token expires)

**Code Example**:
```kotlin
fun pollStatusWithTokenRefresh(jobId: String, initialToken: String) {
    var serviceToken = initialToken
    var tokenExpiresAt = System.currentTimeMillis() + 900000 // 15 minutes
    
    while (true) {
        // Check token expiration
        if (System.currentTimeMillis() >= tokenExpiresAt) {
            val tokenResponse = vendToken(userJwt)
            serviceToken = tokenResponse.token
            tokenExpiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
        }
        
        val status = getStatus(serviceToken, jobId)
        if (status.status == "completed") break
        Thread.sleep(3000)
    }
}
```

### 2. Network Interruption During Token Vending

**Scenario**: Network fails after credit deduction but before receiving token

**Solution**:
- Use `clientRequestId` for idempotency
- Retry with same `clientRequestId` - will return cached response
- Don't retry without `clientRequestId` (will deduct credits again)

**Code Example**:
```kotlin
fun vendTokenWithRetry(userJwt: String): TokenResponse {
    val clientRequestId = generateUniqueId()
    
    return try {
        apiClient.vendToken(userJwt, clientRequestId)
    } catch (e: NetworkException) {
        // Retry with same clientRequestId (idempotent)
        apiClient.vendToken(userJwt, clientRequestId)
    }
}
```

### 3. Concurrent Token Vending Requests

**Scenario**: User taps button multiple times quickly

**Solution**:
- Disable button after first tap
- Use same `clientRequestId` for all concurrent requests
- All requests will return same cached response (only 1 credit deducted)

**Code Example**:
```kotlin
var vendTokenInProgress = false
var currentRequestId: String? = null

fun vendToken(userJwt: String) {
    if (vendTokenInProgress) {
        // Use existing request ID
        return pollExistingRequest(currentRequestId!!)
    }
    
    vendTokenInProgress = true
    currentRequestId = generateUniqueId()
    
    try {
        val response = apiClient.vendToken(userJwt, currentRequestId!!)
        return response
    } finally {
        vendTokenInProgress = false
    }
}
```

### 4. Job Status Polling Timeout

**Scenario**: Transcription takes longer than expected

**Solution**:
- Set maximum polling duration (e.g., 10 minutes)
- Show progress indicator to user
- Allow user to cancel and retry later
- Store job ID for later retrieval

**Code Example**:
```kotlin
fun pollStatusWithTimeout(jobId: String, maxDurationMs: Long = 600000): TranscriptResult? {
    val startTime = System.currentTimeMillis()
    
    while (System.currentTimeMillis() - startTime < maxDurationMs) {
        val status = getStatus(serviceToken, jobId)
        
        when (status.status) {
            "completed" -> return status.result
            "failed" -> throw TranscriptionFailedException()
            else -> {
                updateProgress(status.progress)
                Thread.sleep(3000)
            }
        }
    }
    
    throw TranscriptionTimeoutException("Transcription took longer than $maxDurationMs ms")
}
```

### 5. Invalid TikTok URL Formats

**Scenario**: User pastes malformed URL

**Solution**:
- Validate URL format client-side before submission
- Show clear error message with expected format
- Provide URL format examples

**Code Example**:
```kotlin
fun validateTikTokUrl(url: String): ValidationResult {
    val patterns = listOf(
        Regex("^https://vm\\.tiktok\\.com/[A-Za-z0-9]+/?$"),
        Regex("^https://www\\.tiktok\\.com/@[A-Za-z0-9_.]+/video/\\d+/?$")
    )
    
    val isValid = patterns.any { it.matches(url.trim()) }
    
    return if (isValid) {
        ValidationResult.Success
    } else {
        ValidationResult.Error(
            "Invalid URL format. Expected: https://vm.tiktok.com/[ID]/"
        )
    }
}
```

### 6. Credit Balance Race Condition

**Scenario**: Balance checked, then another transaction deducts credits before vending

**Solution**:
- Business Engine handles this atomically (no race condition possible)
- Always handle 402 response gracefully
- Don't rely on balance check - handle insufficient credits error

**Code Example**:
```kotlin
fun vendTokenSafely(userJwt: String): TokenResponse {
    return try {
        apiClient.vendToken(userJwt, generateRequestId())
    } catch (e: InsufficientCreditsException) {
        // Handle gracefully even if balance check showed credits
        showInsufficientCreditsError(e.details.balance)
        throw e
    }
}
```

### 7. Service Token Used After Expiration

**Scenario**: App uses expired token for status check

**Solution**:
- Track token expiration time
- Check expiration before each request
- Vend new token if expired

**Code Example**:
```kotlin
class ServiceTokenManager {
    private var token: String? = null
    private var expiresAt: Long = 0
    
    fun getValidToken(userJwt: String): String {
        if (token == null || System.currentTimeMillis() >= expiresAt) {
            refreshToken(userJwt)
        }
        return token!!
    }
    
    private fun refreshToken(userJwt: String) {
        val response = apiClient.vendToken(userJwt, generateRequestId())
        token = response.token
        expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000)
    }
}
```

### 8. Malformed Response from Business Engine

**Scenario**: Unexpected response structure

**Solution**:
- Always validate response structure
- Handle missing fields gracefully
- Log unexpected responses for debugging

**Code Example**:
```kotlin
fun parseStatusResponse(json: String): StatusResponse {
    return try {
        val jsonObject = JSONObject(json)
        StatusResponse(
            ok = jsonObject.optBoolean("ok", false),
            jobId = jsonObject.optString("jobId"),
            status = jsonObject.optString("status", "unknown"),
            progress = jsonObject.optInt("progress", 0),
            result = parseResult(jsonObject.optJSONObject("result"))
        )
    } catch (e: Exception) {
        logError("Failed to parse status response", e)
        throw ParseException("Invalid response format")
    }
}

fun parseResult(resultJson: JSONObject?): TranscriptResult? {
    if (resultJson == null) return null
    
    return TranscriptResult(
        transcription = resultJson.optString("transcription", ""),
        confidence = resultJson.optDouble("confidence", 0.0),
        language = resultJson.optString("language", "en"),
        duration = resultJson.optDouble("duration", 0.0)
    )
}
```

### 9. Network Timeout During Long Operations

**Scenario**: Transcription submission times out (10-minute timeout)

**Solution**:
- Set appropriate timeout for transcription endpoint
- Show loading indicator
- Allow cancellation
- Handle timeout gracefully

**Code Example**:
```kotlin
fun submitTranscriptionWithTimeout(
    serviceToken: String,
    url: String,
    timeoutMs: Long = 600000
): TranscribeResponse {
    val call = apiClient.submitTranscription(serviceToken, url)
    
    return try {
        call.executeWithTimeout(timeoutMs)
    } catch (e: TimeoutException) {
        // Check if job was actually submitted
        // You may need to implement job status check here
        throw TranscriptionTimeoutException("Submission timed out")
    }
}
```

### 10. Duplicate Job Submission

**Scenario**: User submits same URL multiple times

**Solution**:
- Use `clientRequestId` for idempotency
- Check for existing job before submitting
- Show existing job status if found

**Code Example**:
```kotlin
fun submitTranscriptionIdempotent(
    serviceToken: String,
    url: String
): TranscribeResponse {
    val clientRequestId = generateRequestIdFromUrl(url)
    
    // Check if job already exists
    val existingJob = getExistingJob(clientRequestId)
    if (existingJob != null) {
        return existingJob
    }
    
    return apiClient.submitTranscription(
        serviceToken,
        url,
        clientRequestId
    )
}
```

---

## Best Practices

### 1. Token Management

- **Cache Service Token**: Store token and expiration time
- **Refresh Proactively**: Refresh token when < 2 minutes remaining
- **Handle Expiration**: Always check expiration before use
- **Single Token Per Session**: Reuse token for multiple requests

### 2. Error Handling

- **Always Check `ok` Field**: Don't assume success
- **Handle All Error Codes**: Implement handlers for each error code
- **Show User-Friendly Messages**: Use `message` and `guidance` fields
- **Log Details**: Log `details` field for debugging

### 3. Idempotency

- **Always Use `clientRequestId`**: Generate unique ID for each operation
- **Reuse for Retries**: Use same ID when retrying failed requests
- **Store Request IDs**: Keep track of submitted request IDs

### 4. Polling Strategy

- **Exponential Backoff**: Increase polling interval over time
- **Maximum Duration**: Set maximum polling time
- **Progress Updates**: Show progress to user
- **Cancellation**: Allow user to cancel long-running polls

### 5. Credit Management

- **Check Balance First**: Verify credits before vending token
- **Handle 402 Gracefully**: Show purchase option when credits insufficient
- **Display Balance**: Show main and bonus credits separately
- **Update After Vending**: Refresh balance after token vending

### 6. URL Validation

- **Client-Side Validation**: Validate URL format before submission
- **Show Examples**: Provide URL format examples
- **Handle Errors**: Show clear error messages for invalid URLs
- **Sanitize Input**: Trim whitespace and validate format

### 7. Network Resilience

- **Retry Logic**: Implement retry for transient failures
- **Timeout Handling**: Set appropriate timeouts
- **Offline Handling**: Handle network unavailability
- **Request Queuing**: Queue requests when offline

### 8. User Experience

- **Loading Indicators**: Show progress for long operations
- **Error Messages**: Display clear, actionable error messages
- **Success Feedback**: Confirm successful operations
- **Progress Updates**: Show transcription progress

---

## Code Examples

### Kotlin/Android Example

```kotlin
class PluctBusinessEngineClient(
    private val baseUrl: String,
    private val userJwt: String
) {
    private var serviceToken: String? = null
    private var tokenExpiresAt: Long = 0
    
    // Get credit balance
    suspend fun getBalance(): BalanceResponse {
        val response = httpClient.get("$baseUrl/v1/credits/balance") {
            header("Authorization", "Bearer $userJwt")
        }
        return parseBalanceResponse(response.bodyAsText())
    }
    
    // Vend service token
    suspend fun vendToken(clientRequestId: String? = null): TokenResponse {
        val requestId = clientRequestId ?: generateRequestId()
        
        val response = httpClient.post("$baseUrl/v1/vend-token") {
            header("Authorization", "Bearer $userJwt")
            header("Content-Type", "application/json")
            setBody(JSONObject().apply {
                put("userId", extractUserIdFromJwt(userJwt))
                put("clientRequestId", requestId)
            }.toString())
        }
        
        val tokenResponse = parseTokenResponse(response.bodyAsText())
        
        // Cache token
        serviceToken = tokenResponse.token
        tokenExpiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
        
        return tokenResponse
    }
    
    // Get valid service token (refresh if needed)
    suspend fun getValidServiceToken(): String {
        if (serviceToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
            vendToken()
        }
        return serviceToken!!
    }
    
    // Submit transcription
    suspend fun submitTranscription(
        url: String,
        clientRequestId: String? = null
    ): TranscribeResponse {
        val token = getValidServiceToken()
        val requestId = clientRequestId ?: generateRequestId()
        
        val response = httpClient.post("$baseUrl/ttt/transcribe") {
            header("Authorization", "Bearer $token")
            header("Content-Type", "application/json")
            setBody(JSONObject().apply {
                put("url", url)
                put("clientRequestId", requestId)
            }.toString())
        }
        
        return parseTranscribeResponse(response.bodyAsText())
    }
    
    // Poll status
    suspend fun pollStatus(
        jobId: String,
        maxDurationMs: Long = 600000,
        pollIntervalMs: Long = 3000
    ): StatusResponse? {
        val startTime = System.currentTimeMillis()
        var lastStatus: StatusResponse? = null
        
        while (System.currentTimeMillis() - startTime < maxDurationMs) {
            val token = getValidServiceToken()
            
            val response = httpClient.get("$baseUrl/ttt/status/$jobId") {
                header("Authorization", "Bearer $token")
            }
            
            lastStatus = parseStatusResponse(response.bodyAsText())
            
            when (lastStatus.status) {
                "completed" -> return lastStatus
                "failed" -> throw TranscriptionFailedException()
                else -> {
                    delay(pollIntervalMs)
                }
            }
        }
        
        throw TranscriptionTimeoutException()
    }
    
    // Complete transcription flow
    suspend fun transcribeVideo(url: String): String {
        // 1. Check balance
        val balance = getBalance()
        if (balance.balance < 1) {
            throw InsufficientCreditsException(balance.balance)
        }
        
        // 2. Submit transcription
        val submitResponse = submitTranscription(url)
        val jobId = submitResponse.jobId
        
        // 3. Poll for completion
        val statusResponse = pollStatus(jobId)
        
        // 4. Extract transcript
        return statusResponse?.result?.transcription
            ?: throw TranscriptNotFoundException()
    }
}
```

### Swift/iOS Example

```swift
class PluctBusinessEngineClient {
    let baseURL: URL
    let userJWT: String
    private var serviceToken: String?
    private var tokenExpiresAt: Date?
    
    init(baseURL: URL, userJWT: String) {
        self.baseURL = baseURL
        self.userJWT = userJWT
    }
    
    // Get credit balance
    func getBalance() async throws -> BalanceResponse {
        let url = baseURL.appendingPathComponent("v1/credits/balance")
        var request = URLRequest(url: url)
        request.setValue("Bearer \(userJWT)", forHTTPHeaderField: "Authorization")
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(BalanceResponse.self, from: data)
    }
    
    // Vend service token
    func vendToken(clientRequestId: String? = nil) async throws -> TokenResponse {
        let requestId = clientRequestId ?? UUID().uuidString
        let url = baseURL.appendingPathComponent("v1/vend-token")
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(userJWT)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "userId": extractUserIdFromJWT(userJWT),
            "clientRequestId": requestId
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        let response = try JSONDecoder().decode(TokenResponse.self, from: data)
        
        // Cache token
        serviceToken = response.token
        tokenExpiresAt = Date().addingTimeInterval(TimeInterval(response.expiresIn))
        
        return response
    }
    
    // Get valid service token
    func getValidServiceToken() async throws -> String {
        if serviceToken == nil || Date() >= tokenExpiresAt! {
            _ = try await vendToken()
        }
        return serviceToken!
    }
    
    // Submit transcription
    func submitTranscription(url: String, clientRequestId: String? = nil) async throws -> TranscribeResponse {
        let token = try await getValidServiceToken()
        let requestId = clientRequestId ?? UUID().uuidString
        let endpoint = baseURL.appendingPathComponent("ttt/transcribe")
        
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "url": url,
            "clientRequestId": requestId
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(TranscribeResponse.self, from: data)
    }
    
    // Poll status
    func pollStatus(jobId: String, maxDuration: TimeInterval = 600, pollInterval: TimeInterval = 3) async throws -> StatusResponse {
        let startTime = Date()
        
        while Date().timeIntervalSince(startTime) < maxDuration {
            let token = try await getValidServiceToken()
            let url = baseURL.appendingPathComponent("ttt/status/\(jobId)")
            
            var request = URLRequest(url: url)
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            
            let (data, _) = try await URLSession.shared.data(for: request)
            let status = try JSONDecoder().decode(StatusResponse.self, from: data)
            
            switch status.status {
            case "completed":
                return status
            case "failed":
                throw TranscriptionFailedError()
            default:
                try await Task.sleep(nanoseconds: UInt64(pollInterval * 1_000_000_000))
            }
        }
        
        throw TranscriptionTimeoutError()
    }
    
    // Complete transcription flow
    func transcribeVideo(url: String) async throws -> String {
        // 1. Check balance
        let balance = try await getBalance()
        guard balance.balance >= 1 else {
            throw InsufficientCreditsError(balance: balance.balance)
        }
        
        // 2. Submit transcription
        let submitResponse = try await submitTranscription(url: url)
        
        // 3. Poll for completion
        let statusResponse = try await pollStatus(jobId: submitResponse.jobId)
        
        // 4. Extract transcript
        guard let transcript = statusResponse.result?.transcription else {
            throw TranscriptNotFoundError()
        }
        
        return transcript
    }
}
```

### Error Handling Example

```kotlin
sealed class PluctError {
    data class InsufficientCredits(val balance: Int) : PluctError()
    data class InvalidURL(val message: String, val expectedFormat: String) : PluctError()
    data class Unauthorized(val message: String) : PluctError()
    data class RateLimited(val resetTime: String) : PluctError()
    data class JobNotFound(val jobId: String) : PluctError()
    data class UpstreamError(val status: Int, val message: String) : PluctError()
    data class NetworkError(val cause: Throwable) : PluctError()
    data class UnknownError(val code: String, val message: String) : PluctError()
}

fun handleError(error: PluctError): UserMessage {
    return when (error) {
        is PluctError.InsufficientCredits -> {
            UserMessage(
                title = "Insufficient Credits",
                message = "You need 1 credit to transcribe. Current balance: ${error.balance}",
                action = "Purchase Credits"
            )
        }
        is PluctError.InvalidURL -> {
            UserMessage(
                title = "Invalid URL",
                message = error.message,
                action = "Check URL Format"
            )
        }
        is PluctError.Unauthorized -> {
            UserMessage(
                title = "Authentication Error",
                message = "Your session has expired. Please log in again.",
                action = "Login"
            )
        }
        is PluctError.RateLimited -> {
            UserMessage(
                title = "Rate Limit Exceeded",
                message = "Too many requests. Try again after ${error.resetTime}",
                action = "Wait"
            )
        }
        is PluctError.UpstreamError -> {
            UserMessage(
                title = "Service Unavailable",
                message = "Transcription service is temporarily unavailable. Please try again later.",
                action = "Retry"
            )
        }
        else -> {
            UserMessage(
                title = "Error",
                message = "An unexpected error occurred. Please try again.",
                action = "Retry"
            )
        }
    }
}
```

---

## Summary

### Key Takeaways

1. **Two-Token System**: User JWT for credit operations, Service Token for transcription
2. **Idempotency**: Always use `clientRequestId` to prevent duplicate charges
3. **Token Management**: Cache service tokens and refresh before expiration
4. **Error Handling**: Check `ok` field and handle all error codes
5. **Polling Strategy**: Poll every 3-5 seconds with maximum duration
6. **Transcript Extraction**: Business Engine handles multiple formats automatically
7. **Credit System**: Dual-wallet (main + bonus) with priority spending
8. **URL Validation**: Validate client-side, handle server-side errors gracefully

### Integration Checklist

- [ ] Implement user JWT generation/management
- [ ] Implement credit balance check
- [ ] Implement token vending with idempotency
- [ ] Implement transcription submission
- [ ] Implement status polling with timeout
- [ ] Implement transcript extraction
- [ ] Implement error handling for all error codes
- [ ] Implement token expiration handling
- [ ] Implement retry logic for transient failures
- [ ] Implement URL validation client-side
- [ ] Test all edge cases
- [ ] Handle network interruptions
- [ ] Implement offline queue if needed

---

**For questions or support, refer to the main README.md or contact the development team.**



