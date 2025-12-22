# VoiceAI Test Runner Script
# Runs all tests: Rust (transcribe-rs) and Java (JUnit)

$ErrorActionPreference = "Stop"

Write-Host "=== VoiceAI Test Suite ===" -ForegroundColor Cyan
Write-Host ""

$testsFailed = 0
$totalTests = 0

# ===========================================================================
# RUST TESTS (transcribe-rs)
# ===========================================================================

Write-Host "--- Rust Tests (transcribe-rs) ---" -ForegroundColor Yellow

Push-Location "transcribe-rs"

# Check if model files exist for tests
$modelPath = "models/whisper-medium-q4_1.bin"
if (-not (Test-Path $modelPath)) {
    Write-Host "  [SKIP] Whisper model not found at $modelPath" -ForegroundColor DarkGray
    Write-Host "  [INFO] Download model to run whisper tests" -ForegroundColor DarkGray
}
else {
    Write-Host "  Running cargo test..."
    cargo test 2>&1 | ForEach-Object { Write-Host "    $_" }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Rust tests failed!" -ForegroundColor Red
        $testsFailed++
    }
    else {
        Write-Host "  [PASS] Rust tests passed!" -ForegroundColor Green
    }
    $totalTests++
}

Pop-Location

# ===========================================================================
# JAVA TESTS (JUnit)
# ===========================================================================

Write-Host ""
Write-Host "--- Java Tests (Processing Pipeline) ---" -ForegroundColor Yellow

# Check for JUnit
$junitJar = "libs/junit-4.13.2.jar"
$hamcrestJar = "libs/hamcrest-core-1.3.jar"

if (-not (Test-Path $junitJar)) {
    Write-Host "  [INFO] Downloading JUnit..." -ForegroundColor DarkGray
    New-Item -ItemType Directory -Force -Path "libs" | Out-Null
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar" -OutFile $junitJar
}

if (-not (Test-Path $hamcrestJar)) {
    Write-Host "  [INFO] Downloading Hamcrest..." -ForegroundColor DarkGray
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar" -OutFile $hamcrestJar
}

# Find Android platform for android.* imports
$ANDROID_HOME = $env:ANDROID_HOME
if (-not $ANDROID_HOME) {
    $ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
}
$platformDir = Get-ChildItem "$ANDROID_HOME\platforms" -Directory | Sort-Object Name -Descending | Select-Object -First 1
$androidJar = "$($platformDir.FullName)\android.jar"

# Create test output directory
New-Item -ItemType Directory -Force -Path "build_manual\test" | Out-Null

# Note: Java tests require Android stubs for Context, SharedPreferences, etc.
# Full testing requires Android instrumentation or Robolectric
Write-Host "  [INFO] Java unit tests require Android framework stubs" -ForegroundColor DarkGray
Write-Host "  [INFO] Run on device with: adb shell am instrument ..." -ForegroundColor DarkGray
Write-Host "  [SKIP] Java tests skipped (requires Android environment)" -ForegroundColor DarkGray

# ===========================================================================
# SUMMARY
# ===========================================================================

Write-Host ""
Write-Host "=== Test Summary ===" -ForegroundColor Cyan

if ($testsFailed -eq 0 -and $totalTests -gt 0) {
    Write-Host "All tests passed! ($totalTests/$totalTests)" -ForegroundColor Green
}
elseif ($totalTests -eq 0) {
    Write-Host "No tests were run (missing dependencies)" -ForegroundColor Yellow
}
else {
    Write-Host "FAILED: $testsFailed/$totalTests tests failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Manual Testing Required ===" -ForegroundColor Magenta
Write-Host "1. Build APK: .\build.ps1"
Write-Host "2. Install: adb install -r VoiceAI-v1.1.0.apk"
Write-Host "3. Test HeliBoard mic button -> RecognizeActivity overlay"
Write-Host "4. Test SwiftKey mic button -> waveform animation"
Write-Host "5. Verify text output appears in input field"
Write-Host ""
