param(
    [string]$BaseUrl = 'https://pluct-business-engine.romeo-lya2.workers.dev',
    [string]$UserId  = 'mobile',
    [string]$TestUrl = 'https://vm.tiktok.com/ZMAPTWV7o/'
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Write-Step { param([string]$Message) Write-Host "[API-DEBUG] $Message" }
function Show-Req  { param([string]$Message) Write-Host "[REQUEST]  $Message" -ForegroundColor Cyan }
function Show-Res  { param([string]$Message) Write-Host "[RESPONSE] $Message" -ForegroundColor Yellow }

# Load local .dev.vars (KEY=VALUE per line) to avoid hardcoding secrets
$varsPath = Join-Path $PSScriptRoot '.dev.vars'
if (Test-Path $varsPath) {
    Get-Content $varsPath | ForEach-Object {
        if ($_ -and $_ -notmatch '^\s*#' -and $_ -match '=') {
            $k,$v = $_ -split '=',2
            $k = $k.Trim(); $v = $v.Trim()
            if ($k) { Set-Item -Path ("Env:" + $k) -Value $v }
        }
    }
}

$DEV_API_KEY        = $env:DEV_API_KEY
$DEV_ADMIN_BEARER   = $env:DEV_ADMIN_BEARER
$DEV_WEBHOOK_SECRET = $env:DEV_WEBHOOK_SECRET

# Unified HTTP executor that returns structured diagnostics (status, code, retryable, body)
function Invoke-HttpJson {
    param(
        [Parameter(Mandatory=$true)][ValidateSet('GET','POST')][string]$Method,
        [Parameter(Mandatory=$true)][string]$Url,
        [hashtable]$Headers=@{},
        [string]$BodyJson=''
    )
    $result = [ordered]@{
        method     = $Method
        url        = $Url
        headers    = $Headers
        requestBody= $BodyJson
        statusCode = $null
        code       = $null
        retryable  = $false
        bodyRaw    = $null
        body       = $null
        errorRaw   = $null
        error      = $null
        timestamp  = (Get-Date).ToString('o')
    }
    try {
        if (-not $Headers['User-Agent']) {
            $Headers['User-Agent'] = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36'
        }
        if (-not $Headers['Accept']) {
            $Headers['Accept'] = 'application/json, text/plain, */*'
        }
        $iw = Invoke-WebRequest -Method $Method -Uri $Url -Headers $Headers -Body $BodyJson -MaximumRedirection 5 -UseBasicParsing -ErrorAction Stop
        $result.statusCode = [int]$iw.StatusCode
        $result.bodyRaw    = $iw.Content
        try { $result.body = $iw.Content | ConvertFrom-Json } catch { $result.body = @{ text = $iw.Content } }
        # attach uniform code/retryable hints on success
        $result.code       = 'ok'
        $result.retryable  = $false
        # If some hosts still produce $null status (older PS), fall back to curl parsing
        if (-not $result.statusCode) {
            $curl = Invoke-CurlJson -Method $Method -Url $Url -Headers $Headers -BodyJson $BodyJson
            return $curl
        }
        return $result
    } catch {
        $ex = $_.Exception
        $status = $null
        if ($ex.Response -and $ex.Response.StatusCode) {
            $status = [int]$ex.Response.StatusCode
        }
        $result.statusCode = $status
        try {
            if ($ex.Response -and $ex.Response.GetResponseStream) {
                $reader = New-Object System.IO.StreamReader($ex.Response.GetResponseStream())
                $raw = $reader.ReadToEnd()
                $result.errorRaw = $raw
                try { $result.error = $raw | ConvertFrom-Json } catch { $result.error = @{ message = $raw } }
            }
        } catch {}
        # standardize code and retryable across errors
        switch ($status) {
            401 { $result.code='unauthorized'; $result.retryable=$false }
            403 { $result.code= if ($result.error.error) { "${$result.error.error}" } else { 'forbidden' }; $result.retryable=$false }
            408 { $result.code='timeout'; $result.retryable=$true }
            429 { $result.code='rate_limited'; $result.retryable=$true }
            500 { $result.code='server_error'; $result.retryable=$true }
            default { $result.code='request_failed'; $result.retryable=$true }
        }
        return $result
    }
}

# Fallback using curl.exe to guarantee status/body capture
function Invoke-CurlJson {
    param(
        [Parameter(Mandatory=$true)][ValidateSet('GET','POST')][string]$Method,
        [Parameter(Mandatory=$true)][string]$Url,
        [hashtable]$Headers=@{},
        [string]$BodyJson=''
    )
    $hArgs = @()
    foreach ($k in $Headers.Keys) { $hArgs += @('-H', ('{0}: {1}' -f $k, $Headers[$k])) }
    $mArg = @('-X', $Method)
    $dArg = @()
    if ($Method -eq 'POST') { $dArg = @('--data', $BodyJson) }
    $tmp = New-TemporaryFile
    try {
        $args = @('-s','-D','-') + $mArg + $hArgs + $dArg + @($Url)
        $raw = & curl.exe @args 2>$null | Tee-Object -FilePath $tmp.FullName
        $all = Get-Content $tmp.FullName -Raw
        $parts = $all -split "\r?\n\r?\n"
        if ($parts.Length -lt 2) { $parts = @($all,'') }
        $headersText = $parts[0]
        $bodyText    = ($parts[($parts.Length-1)])
        $statusLine  = ($headersText -split "\r?\n")[0]
        $statusCode  = ($statusLine -replace 'HTTP/(1\.1|2)\s+','') -split '\s+' | Select-Object -First 1
        $res = [ordered]@{
            method     = $Method
            url        = $Url
            headers    = $Headers
            requestBody= $BodyJson
            statusCode = [int]$statusCode
            code       = 'ok'
            retryable  = $false
            bodyRaw    = $bodyText
            body       = $null
            errorRaw   = $null
            error      = $null
            timestamp  = (Get-Date).ToString('o')
        }
        try { $res.body = $bodyText | ConvertFrom-Json } catch { $res.body = @{ text = $bodyText } }
        if ($res.statusCode -ge 400) {
            $res.errorRaw = $res.bodyRaw
            $res.error = $res.body
            switch ([int]$res.statusCode) {
                401 { $res.code='unauthorized'; $res.retryable=$false }
                403 { $res.code= if ($res.error.error) { "$($res.error.error)" } else { 'forbidden' }; $res.retryable=$false }
                408 { $res.code='timeout'; $res.retryable=$true }
                429 { $res.code='rate_limited'; $res.retryable=$true }
                500 { $res.code='server_error'; $res.retryable=$true }
                default { $res.code='request_failed'; $res.retryable=$true }
            }
        }
        return $res
    } finally { Remove-Item $tmp -ErrorAction SilentlyContinue }
}

try {
    $headers = @{ 'Content-Type' = 'application/json' }
    if ($DEV_API_KEY) { $headers['X-API-Key'] = $DEV_API_KEY }
    # Print 5 helpful placeholders for troubleshooting
    Write-Step ("Context => BaseUrl=$BaseUrl | UserId=$UserId | TestUrl=$TestUrl | Time=`"{0}`" | Host=`"{1}`"" -f ((Get-Date).ToString('o')),[System.Environment]::MachineName)
    Write-Step ("Headers => {0}" -f ($headers | ConvertTo-Json))

    # 1) Health
    Write-Step '1) GET /health'
    $healthUrl = "$BaseUrl/health"
    Show-Req "GET $healthUrl"
    $health = Invoke-HttpJson -Method GET -Url $healthUrl -Headers $headers
    Show-Res ($health | ConvertTo-Json -Depth 20)
    if (-not $health.body -or -not ($health.body.healthy -or $health.body.status -eq 'healthy')) {
        throw "Health check failed: $($health | ConvertTo-Json -Depth 20)"
    }

    # 2) Vend Token
    Write-Step '2) POST /vend-token'
    $vendUrl = "$BaseUrl/vend-token"
    $vendBody = @{ userId = $UserId } | ConvertTo-Json
    Show-Req ("POST $vendUrl`nBODY: $vendBody")
    $vend = Invoke-HttpJson -Method POST -Url $vendUrl -Headers $headers -BodyJson $vendBody
    Show-Res ($vend | ConvertTo-Json -Depth 20)
    if ($vend.statusCode -and $vend.statusCode -ne 200) { throw ("Vend-token failed: status=$($vend.statusCode) code=$($vend.code) retryable=$($vend.retryable) details=$($vend.errorRaw)") }
    if (-not $vend.body -or -not $vend.body.token) { throw 'Token vending failed (no token in response)' }
    $token = [string]$vend.body.token

    # 3) Transcribe
    Write-Step '3) POST /ttt/transcribe'
    $txUrl  = "$BaseUrl/ttt/transcribe"
    $txBody = @{ url = $TestUrl } | ConvertTo-Json
    $authHeaders = @{ 'Authorization' = "Bearer $token"; 'Content-Type' = 'application/json' }
    if ($DEV_API_KEY) { $authHeaders['X-API-Key'] = $DEV_API_KEY }
    Show-Req ("POST $txUrl`nAUTH: Bearer ****`nBODY: $txBody")
    $tx = Invoke-HttpJson -Method POST -Url $txUrl -Headers $authHeaders -BodyJson $txBody
    Show-Res ($tx | ConvertTo-Json -Depth 20)
    if ($tx.statusCode -and $tx.statusCode -ne 200) { throw ("Transcribe failed: status=$($tx.statusCode) code=$($tx.code) retryable=$($tx.retryable) details=$($tx.errorRaw)") }
    if (-not $tx.body -or -not $tx.body.request_id) { throw 'Transcribe call did not return request_id' }

    # 4) Single status poll
    Write-Step '4) GET /ttt/status/{request_id}'
    $statusUrl = "$BaseUrl/ttt/status/$($tx.body.request_id)"
    Show-Req ("GET $statusUrl`nAUTH: Bearer ****")
    $st = Invoke-HttpJson -Method GET -Url $statusUrl -Headers $authHeaders
    Show-Res ($st | ConvertTo-Json -Depth 20)

    Write-Step 'Completed successfully'
}
catch {
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response -and $_.Exception.Response.Content) {
        try { Write-Host ("[ERROR RESPONSE] " + ($_.Exception.Response.Content | ConvertTo-Json -Depth 10)) -ForegroundColor Red } catch {}
    }
    exit 1
}

