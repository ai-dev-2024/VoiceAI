$QwenUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
$TokenizerUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct/raw/main/tokenizer.json"
$TargetDir = "assets/qwen"

Write-Host "Creating target directory: $TargetDir"
New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

$ModelFile = "$TargetDir/qwen2.5-0.5b-instruct-q4_k_m.gguf"
$TokenizerFile = "$TargetDir/tokenizer.json"

if (-not (Test-Path $ModelFile)) {
    Write-Host "Downloading Qwen Model (approx 300MB)..."
    Invoke-WebRequest -Uri $QwenUrl -OutFile $ModelFile
    Write-Host "Model downloaded."
} else {
    Write-Host "Model already exists."
}

if (-not (Test-Path $TokenizerFile)) {
    Write-Host "Downloading Tokenizer..."
    Invoke-WebRequest -Uri $TokenizerUrl -OutFile $TokenizerFile
    Write-Host "Tokenizer downloaded."
} else {
    Write-Host "Tokenizer already exists."
}

Write-Host "Done. Assets are ready for build."
