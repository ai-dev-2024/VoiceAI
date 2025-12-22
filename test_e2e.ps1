# VoiceAI Automated End-to-End Test Suite
# Uses ADB to perform automated testing on connected device
# Run this AFTER installing VoiceAI APK

$ErrorActionPreference = "Stop"

Write-Host "=== VoiceAI Automated E2E Testing ===" -ForegroundColor Cyan
Write-Host ""

# Check ADB connection
Write-Host "--- Checking Device Connection ---" -ForegroundColor Yellow
$devices = adb devices 2>&1
if ($devices -match "\bdevice\b") {
    Write-Host "[OK] Device connected" -ForegroundColor Green
}
else {
    Write-Host "[FAIL] No devices connected!" -ForegroundColor Red
    Write-Host "Connect device via USB or WiFi ADB first:"
    Write-Host "  adb connect <device-ip>:5555"
    exit 1
}

# Test 1: Verify APK Installation
Write-Host ""
Write-Host "--- Test 1: APK Installation ---" -ForegroundColor Yellow
$packages = adb shell pm list packages -3 2>&1
if ($packages -match "com.voiceai.app") {
    Write-Host "[PASS] VoiceAI APK is installed" -ForegroundColor Green
}
else {
    Write-Host "[FAIL] VoiceAI APK not found!" -ForegroundColor Red
    Write-Host "Install with: adb install -r VoiceAI-v1.1.0.apk"
    exit 1
}

# Test 2: Launch Settings Activity
Write-Host ""
Write-Host "--- Test 2: Settings Activity Launch ---" -ForegroundColor Yellow
adb shell am start -n com.voiceai.app/com.voiceai.app.SettingsActivity 2>&1 | Out-Null
Start-Sleep -Seconds 2
$window = adb shell dumpsys window windows 2>&1 | Select-String "SettingsActivity"
if ($window) {
    Write-Host "[PASS] SettingsActivity launched successfully" -ForegroundColor Green
}
else {
    Write-Host "[WARN] SettingsActivity may not have launched" -ForegroundColor DarkYellow
}

# Test 3: Check Accessibility Service Status
Write-Host ""
Write-Host "--- Test 3: Accessibility Service ---" -ForegroundColor Yellow
$services = adb shell settings get secure enabled_accessibility_services 2>&1
if ($services -match "VoiceTextInjectionService") {
    Write-Host "[PASS] Accessibility service is enabled" -ForegroundColor Green
}
else {
    Write-Host "[INFO] Accessibility service not enabled" -ForegroundColor DarkYellow
    Write-Host "       Enable in Settings > Accessibility > VoiceAI Text Injection"
}

# Test 4: Check Voice Input Provider
Write-Host ""
Write-Host "--- Test 4: Voice Input Provider ---" -ForegroundColor Yellow
$voiceInput = adb shell settings get secure voice_recognition_service 2>&1
if ($voiceInput -match "com.voiceai.app") {
    Write-Host "[PASS] VoiceAI set as voice input provider" -ForegroundColor Green
}
else {
    Write-Host "[INFO] VoiceAI is not the default voice input provider" -ForegroundColor DarkYellow
    Write-Host "       Set in Settings > Language & Input > Voice Input"
}

# Test 5: Check IME (Keyboard) Status
Write-Host ""
Write-Host "--- Test 5: IME Registration ---" -ForegroundColor Yellow
$imes = adb shell ime list -s 2>&1
if ($imes -match "com.voiceai.app") {
    Write-Host "[PASS] VoiceAI IME is registered" -ForegroundColor Green
}
else {
    Write-Host "[INFO] VoiceAI IME not enabled" -ForegroundColor DarkYellow
}

# Test 6: Launch RecognizeActivity (simulates mic tap)
Write-Host ""
Write-Host "--- Test 6: Voice Dictation Overlay ---" -ForegroundColor Yellow
adb shell am start -n com.voiceai.app/com.voiceai.app.RecognizeActivity `
    --es android.speech.extra.LANGUAGE "en-US" 2>&1 | Out-Null
Start-Sleep -Seconds 2
$window2 = adb shell dumpsys window windows 2>&1 | Select-String "RecognizeActivity"
if ($window2) {
    Write-Host "[PASS] RecognizeActivity overlay launched" -ForegroundColor Green
    
    # Check if native methods loaded
    $logcat = adb logcat -d -t 50 2>&1 | Select-String "VoiceAI"
    if ($logcat -match "Listening") {
        Write-Host "[PASS] Native recording started" -ForegroundColor Green
    }
    elseif ($logcat -match "Error") {
        Write-Host "[WARN] Errors detected in logcat" -ForegroundColor DarkYellow
    }
}
else {
    Write-Host "[WARN] RecognizeActivity may not have launched" -ForegroundColor DarkYellow
}

# Close the activity
adb shell input keyevent KEYCODE_BACK 2>&1 | Out-Null
Start-Sleep -Seconds 1

# Test 7: Check SharedPreferences (Timer settings)
Write-Host ""
Write-Host "--- Test 7: Settings Persistence ---" -ForegroundColor Yellow
$prefs = adb shell "run-as com.voiceai.app cat /data/data/com.voiceai.app/shared_prefs/VoiceAIPrefs.xml" 2>&1
if ($prefs -match "transcription_time_limit") {
    $timeLimitMatch = [regex]::Match($prefs, 'transcription_time_limit.*?value="(\w+)"')
    if ($timeLimitMatch.Success) {
        Write-Host "[PASS] Timer setting found: $($timeLimitMatch.Groups[1].Value)" -ForegroundColor Green
    }
}
else {
    Write-Host "[INFO] Preferences not yet created (first run)" -ForegroundColor DarkYellow
}

# Test 8: Monitor Logcat for processing pipeline
Write-Host ""
Write-Host "--- Test 8: Processing Pipeline ---" -ForegroundColor Yellow
Write-Host "[INFO] Monitoring logcat for pipeline activity..."
$pipelineLog = adb logcat -d -t 100 2>&1 | Select-String "VoiceAI.Pipeline"
if ($pipelineLog) {
    Write-Host "[PASS] Processing pipeline is active" -ForegroundColor Green
    # Show last pipeline entry
    $lastEntry = ($pipelineLog | Select-Object -Last 1).ToString()
    Write-Host "       Last: $lastEntry" -ForegroundColor Gray
}
else {
    Write-Host "[INFO] No recent pipeline activity in logs" -ForegroundColor DarkYellow
}

# Summary
Write-Host ""
Write-Host "=== Test Summary ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Manual tests still required:" -ForegroundColor Magenta
Write-Host "1. Open text input with HeliBoard"
Write-Host "2. Tap mic button"
Write-Host "3. Speak 'twenty five' and verify '25' appears"
Write-Host "4. Check waveform animation is responsive"
Write-Host "5. Verify 30s countdown timer shows (if enabled)"
Write-Host ""
Write-Host "To watch live logs during testing:"
Write-Host "  adb logcat -s VoiceAI VoiceAI.Pipeline DictationController"
Write-Host ""
