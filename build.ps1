# VoiceAI v1.0 Windows Build Script
# Requires: Rust with aarch64-linux-android target, Android SDK, Android NDK, LLVM

$ErrorActionPreference = "Stop"

Write-Host "=== VoiceAI v1.0 Windows Build ==="

# --- Configuration ---
$ANDROID_HOME = $env:ANDROID_HOME
if (-not $ANDROID_HOME) {
    $ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
}

if (-not (Test-Path $ANDROID_HOME)) {
    Write-Error "Android SDK not found at $ANDROID_HOME. Set ANDROID_HOME."
    exit 1
}

Write-Host "Android SDK: $ANDROID_HOME"

# Find NDK
$NDK = $env:ANDROID_NDK_HOME
if (-not $NDK) {
    $ndkDir = Get-ChildItem "$ANDROID_HOME\ndk" -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if ($ndkDir) {
        $NDK = $ndkDir.FullName
    }
}

if (-not $NDK -or -not (Test-Path $NDK)) {
    Write-Error "Android NDK not found. Set ANDROID_NDK_HOME."
    exit 1
}

Write-Host "Android NDK: $NDK"

# Build Tools
$buildToolsDir = Get-ChildItem "$ANDROID_HOME\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1
$BUILD_TOOLS = $buildToolsDir.FullName
Write-Host "Build Tools: $BUILD_TOOLS"

# Platform
$platformDir = Get-ChildItem "$ANDROID_HOME\platforms" -Directory | Sort-Object Name -Descending | Select-Object -First 1
$PLATFORM = "$($platformDir.FullName)\android.jar"
Write-Host "Platform: $PLATFORM"

$AAPT2 = "$BUILD_TOOLS\aapt2.exe"
$D8 = "$BUILD_TOOLS\d8.bat"
$APKSIGNER = "$BUILD_TOOLS\apksigner.bat"
$ZIPALIGN = "$BUILD_TOOLS\zipalign.exe"

# Keystore
$KEYSTORE = "$env:USERPROFILE\.android\debug.keystore"
if (-not (Test-Path $KEYSTORE)) {
    Write-Host "Creating debug keystore..."
    keytool -genkey -v -keystore $KEYSTORE -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
}

# --- Build Steps ---

# Clean
Write-Host "--- Cleaning ---"
if (Test-Path "build_manual") { Remove-Item -Recurse -Force "build_manual" }
New-Item -ItemType Directory -Force -Path "build_manual\gen" | Out-Null
New-Item -ItemType Directory -Force -Path "build_manual\obj" | Out-Null
New-Item -ItemType Directory -Force -Path "build_manual\apk" | Out-Null
New-Item -ItemType Directory -Force -Path "build_manual\lib\arm64-v8a" | Out-Null

# Setup ONNX Runtime
Write-Host "--- Checking ONNX Runtime ---"
if (-not (Test-Path "libs\onnxruntime")) {
    Write-Host "Downloading ONNX Runtime..."
    New-Item -ItemType Directory -Force -Path "libs" | Out-Null
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.22.0/onnxruntime-android-1.22.0.aar" -OutFile "libs\onnxruntime.aar"
    Expand-Archive -Path "libs\onnxruntime.aar" -DestinationPath "libs\onnxruntime" -Force
    Write-Host "ONNX Runtime extracted."
}

# Setup Cargo config for Windows
Write-Host "--- Setting up Cargo config ---"
if (-not (Test-Path ".cargo\config.toml")) {
    New-Item -ItemType Directory -Force -Path ".cargo" | Out-Null
    
    $ndkToolchain = "$NDK\toolchains\llvm\prebuilt\windows-x86_64\bin"
    $ortPath = (Get-Location).Path + "\libs\onnxruntime"
    
    $configContent = @"
[target.aarch64-linux-android]
linker = "$ndkToolchain\aarch64-linux-android28-clang.cmd"

[env]
CC_aarch64_linux_android = "$ndkToolchain\aarch64-linux-android28-clang.cmd"
CXX_aarch64_linux_android = "$ndkToolchain\aarch64-linux-android28-clang++.cmd"
AR_aarch64_linux_android = "$ndkToolchain\llvm-ar.exe"
ORT_LIB_LOCATION = "$ortPath\jni\arm64-v8a"
ORT_INCLUDE_DIR = "$ortPath\headers"
ANDROID_NDK_HOME = "$NDK"
ANDROID_NDK = "$NDK"
"@
    $configContent = $configContent -replace '\\', '/'
    Set-Content -Path ".cargo\config.toml" -Value $configContent
    Write-Host "Cargo config created."
}

# Build Rust
Write-Host "--- Building Rust ---"
$env:ANDROID_NDK_ROOT = $NDK
cargo build --target aarch64-linux-android --release
if ($LASTEXITCODE -ne 0) { Write-Error "Rust build failed"; exit 1 }

# Compile Resources
Write-Host "--- Compiling Resources ---"
& $AAPT2 compile --dir res -o build_manual\resources.zip
& $AAPT2 link -I $PLATFORM --manifest AndroidManifest.xml -o build_manual\apk\unaligned.apk build_manual\resources.zip --java build_manual\gen --auto-add-overlay
if ($LASTEXITCODE -ne 0) { Write-Error "Resource compilation failed"; exit 1 }

# Compile Java
Write-Host "--- Compiling Java ---"
$javaSources = Get-ChildItem -Path "src\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$genSources = Get-ChildItem -Path "build_manual\gen" -Recurse -Filter "*.java" -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
$allSources = $javaSources + $genSources
$allSources | Out-File -FilePath "build_manual\sources.txt" -Encoding ASCII

javac -d build_manual\obj -source 1.8 -target 1.8 -classpath $PLATFORM "@build_manual\sources.txt"
if ($LASTEXITCODE -ne 0) { Write-Error "Java compilation failed"; exit 1 }

# Dex
Write-Host "--- Dexing ---"
$classFiles = Get-ChildItem -Path "build_manual\obj" -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }
$classFiles | Out-File -FilePath "build_manual\classes.txt" -Encoding ASCII

& $D8 --output build_manual\apk --lib $PLATFORM "@build_manual\classes.txt"
if ($LASTEXITCODE -ne 0) { Write-Error "Dex failed"; exit 1 }

# Package
Write-Host "--- Packaging ---"

# Copy native libraries
$rustLib = "target\aarch64-linux-android\release\libvoiceai.so"
if (-not (Test-Path $rustLib)) {
    $rustLib = "target\aarch64-linux-android\release\libandroid_transcribe_app.so"
}
if (Test-Path $rustLib) {
    Copy-Item $rustLib "build_manual\lib\arm64-v8a\"
}

# Copy ONNX Runtime
if (Test-Path "jniLibs\arm64-v8a\libonnxruntime.so") {
    Copy-Item "jniLibs\arm64-v8a\libonnxruntime.so" "build_manual\lib\arm64-v8a\"
}

# Copy libc++_shared.so
$libcpp = Get-ChildItem -Path $NDK -Recurse -Filter "libc++_shared.so" | Where-Object { $_.FullName -match "aarch64" } | Select-Object -First 1
if ($libcpp) {
    Copy-Item $libcpp.FullName "build_manual\lib\arm64-v8a\"
}

# Add to APK
Push-Location "build_manual\apk"
jar uf unaligned.apk classes.dex
Copy-Item -Recurse ..\lib .
jar uf unaligned.apk lib

# Add assets
if (Test-Path "..\..\assets") {
    Copy-Item -Recurse "..\..\assets" .
    jar uf unaligned.apk assets
}
Pop-Location

# Sign
Write-Host "--- Signing ---"
& $ZIPALIGN -f -v 4 build_manual\apk\unaligned.apk build_manual\apk\aligned.apk
& $APKSIGNER sign --ks $KEYSTORE --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --out VoiceAI-v1.0.1.apk build_manual\apk\aligned.apk
if ($LASTEXITCODE -ne 0) { Write-Error "Signing failed"; exit 1 }

Write-Host ""
Write-Host "SUCCESS: VoiceAI-v1.0.1.apk created!"
Write-Host "Install with: adb install -r VoiceAI-v1.0.1.apk"
