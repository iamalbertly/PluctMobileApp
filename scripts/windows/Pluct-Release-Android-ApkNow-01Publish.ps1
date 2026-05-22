param(
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [string]$BusinessEnginePath = "C:\Shared\Projects\Pluct\pluct-business-engine",
    [string]$PublishedUrl = $env:PLUCT_PUBLISHED_APK_URL,
    [string]$ChromeDebugUrl = $env:PLUCT_CHROME_CDP_URL,
    [switch]$SkipDeploy
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Resolve-Path "$PSScriptRoot\..\.."
$apk = Resolve-Path (Join-Path $root $ApkPath)
if (-not (Test-Path $apk)) {
    throw "APK not found: $apk"
}

function Test-DownloadUrl([string]$Url) {
    if ([string]::IsNullOrWhiteSpace($Url)) { return $false }
    $response = Invoke-WebRequest -Uri $Url -Method Head -MaximumRedirection 3 -TimeoutSec 20 -ErrorAction SilentlyContinue
    return ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400)
}

function Get-ChromeDebugUrl {
    if (-not [string]::IsNullOrWhiteSpace($ChromeDebugUrl)) { return $ChromeDebugUrl }
    $portFile = Join-Path $env:LOCALAPPDATA "Google\Chrome\User Data\DevToolsActivePort"
    if (-not (Test-Path $portFile)) { return "" }
    $lines = Get-Content $portFile | Where-Object { $_.Trim().Length -gt 0 }
    if ($lines.Count -lt 1) { return "" }
    return "http://127.0.0.1:$($lines[0].Trim())"
}

function Try-UploadWithExistingChromeSession {
    $debug = Get-ChromeDebugUrl
    if ([string]::IsNullOrWhiteSpace($debug)) { return "" }
    $probe = Invoke-WebRequest -Uri "$debug/json/version" -TimeoutSec 8 -ErrorAction SilentlyContinue
    if ($probe.StatusCode -ne 200) { return "" }
    $env:PLUCT_APK_PATH = $apk
    $env:PLUCT_CHROME_CDP_URL = $debug
    $uploadScript = @'
const fs = require('fs');

function loadPlaywright() {
  try { return require('playwright-core'); } catch (_) {}
  return require('playwright');
}

async function findPublishedUrl(page) {
  const urls = await page.locator('a[href*="apknow.one/"]').evaluateAll((nodes) =>
    nodes.map((node) => node.href).filter(Boolean)
  ).catch(() => []);
  return urls.find((url) => /^https:\/\/apknow\.one\/[A-Za-z0-9_-]{8,}$/i.test(url)) || '';
}

(async () => {
  const apkPath = process.env.PLUCT_APK_PATH;
  const cdpUrl = process.env.PLUCT_CHROME_CDP_URL;
  if (!fs.existsSync(apkPath)) throw new Error(`APK not found: ${apkPath}`);
  const { chromium } = loadPlaywright();
  const browser = await chromium.connectOverCDP(cdpUrl);
  const context = browser.contexts()[0] || await browser.newContext();
  const pages = context.pages();
  const page = pages.find((p) => /apknow\.one/i.test(p.url())) || await context.newPage();
  if (!/apknow\.one/i.test(page.url())) {
    await page.goto('https://apknow.one/', { waitUntil: 'domcontentloaded', timeout: 30000 });
  }
  const input = page.locator('input[type="file"]').first();
  await input.waitFor({ state: 'attached', timeout: 30000 });
  await input.setInputFiles(apkPath);
  for (const label of ['Upload', 'Publish', 'Submit', 'Create', 'Save']) {
    const button = page.getByRole('button', { name: new RegExp(label, 'i') }).first();
    if (await button.count().catch(() => 0)) {
      await button.click({ timeout: 10000 }).catch(() => {});
      break;
    }
  }
  for (let attempt = 0; attempt < 60; attempt++) {
    const published = await findPublishedUrl(page);
    if (published) {
      console.log(published);
      await browser.close();
      return;
    }
    await page.waitForTimeout(1000);
  }
  await browser.close();
  throw new Error('Apknow upload did not expose a published URL within 60 seconds.');
})().catch((error) => {
  console.error(error.message || String(error));
  process.exit(2);
});
'@
    $output = & node -e $uploadScript 2>&1
    if ($LASTEXITCODE -eq 0) {
        $published = ($output | Where-Object { $_ -match '^https://apknow\.one/' } | Select-Object -Last 1)
        if (-not [string]::IsNullOrWhiteSpace($published)) { return $published.Trim() }
    }
    Write-Host "Chrome debug session detected, but automated Apknow upload did not return a URL. Rerun with -PublishedUrl <url> after manual upload."
    return ""
}

if ([string]::IsNullOrWhiteSpace($PublishedUrl)) {
    $PublishedUrl = Try-UploadWithExistingChromeSession
}

if (-not (Test-DownloadUrl $PublishedUrl)) {
    throw "Published APK URL did not resolve. Provide -PublishedUrl after uploading the latest APK to Apknow."
}

$engine = Resolve-Path $BusinessEnginePath
Push-Location $engine
try {
    Write-Host "Configuring MOBILE_APK_URL in Cloudflare Worker secret store."
    $PublishedUrl | npx wrangler secret put MOBILE_APK_URL
    if (-not $SkipDeploy) {
        npx wrangler deploy
    }
    $policy = Invoke-WebRequest -Uri "https://pluct-business-engine.romeo-lya2.workers.dev/v1/public/client-policy" -TimeoutSec 30
    if ($policy.StatusCode -ne 200 -or $policy.Content -notmatch [regex]::Escape($PublishedUrl)) {
        throw "Production client-policy does not reference the published APK URL yet."
    }
    $latest = Invoke-WebRequest -Uri "https://pluct-business-engine.romeo-lya2.workers.dev/downloads/android/latest.apk" -MaximumRedirection 0 -TimeoutSec 30 -ErrorAction SilentlyContinue
    if ($latest.StatusCode -notin @(200, 302, 307, 308)) {
        throw "Production latest.apk did not resolve after publish."
    }
}
finally {
    Pop-Location
}
