# 🎤 VoiceAI

<div align="center">

![VoiceAI Logo](https://img.shields.io/badge/VoiceAI-Offline%20Voice%20Dictation-blueviolet?style=for-the-badge&logo=android)

**A Wispr Flow offline alternative for Android**

Fully local voice dictation with advanced AI post-processing

[![Built with Antigravity](https://img.shields.io/badge/Built%20with-Google%20Antigravity-4285F4?style=flat-square&logo=google)](https://developers.google.com/project-antigravity)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-min%20API%2026-3DDC84?style=flat-square&logo=android)](https://developer.android.com)

</div>

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔒 **100% Offline** | No internet required — all processing happens on-device |
| ⚡ **Fast Transcription** | NVIDIA Parakeet TDT 0.6B model (int8 quantized) |
| 🧠 **Smart Post-Processing** | Automatic punctuation, casing, number formatting |
| 📖 **Personal Dictionary** | FUTO-style custom word replacements |
| ⏱️ **30-Second Timer** | Optional auto-stop after 30 seconds |
| 🔇 **Silence Detection** | Auto-stop when you stop speaking |
| 🌐 **Universal Injection** | Works with any app via Accessibility Service |

---

## 📱 Tested Keyboards

| Keyboard | Status |
|----------|--------|
| [HeliBoard](https://github.com/Helium314/HeliBoard) | ✅ Working |
| [SwiftKey](https://www.microsoft.com/swiftkey) | ✅ Working |

---

## 🚀 Quick Start

### Installation

1. **Download** `VoiceAI-v1.0.2.apk` from [Releases](../../releases)
2. **Install** on your Android device
3. **Enable** VoiceAI in Settings → Language & Input → Keyboards
4. **Enable** Accessibility Service for text injection
5. **Grant** microphone permission

### Usage

1. Open any text field in any app
2. Tap the **microphone button** on your keyboard
3. Speak naturally
4. Tap screen or wait for auto-stop

---

## 🎯 Post-Processing Examples

| You Say | VoiceAI Outputs |
|---------|-----------------|
| "twenty five percent" | **25%** |
| "one hundred US dollars" | **$100 USD** |
| "twenty twenty four" | **2024** |
| "four twenty pm" | **4:20 PM** |
| "twenty first of december" | **21st of December** |
| "uh so i was thinking um" | **So, I was thinking** |

---

## ⚙️ Settings

Access via **VoiceAI app → Open Settings**:

- **⏱️ 30-Second Limit** — Auto-stop after 30 seconds
- **🔇 Silence Detection** — Auto-stop when you stop speaking  
- **📖 Personal Dictionary** — Add custom words (e.g., `@Groq, ChatGPT, Anthropic`)

---

## 🛠️ Tech Stack

### Frontend
- **Rust** + `egui` — Native Android UI
- **Java** — Activities, Services, Accessibility

### Backend / AI
- **ONNX Runtime** — Neural network inference
- **Parakeet TDT 0.6B** — NVIDIA's speech-to-text model

### Build
- **Cargo** — Rust package manager
- **Android SDK/NDK** — Native compilation
- **PowerShell** — Windows build script

---

## 🙏 Credits & Acknowledgments

This project is built upon and inspired by:

| Project | Contribution |
|---------|--------------|
| [**transcribe-rs**](https://github.com/handy-audio/transcribe-rs) by Handy Audio | Core ASR Rust library |
| [**FUTO Voice Input**](https://gitlab.futo.org/alex/voiceinput) | Personal dictionary UI inspiration |
| [**Wispr Flow**](https://wispr.com/flow) | The original desktop voice dictation |
| **NVIDIA NeMo** | Parakeet TDT speech model |
| **Google Antigravity** | AI coding assistant |

---

## 🏗️ Building from Source

```bash
# Clone
git clone https://github.com/ai-dev-2024/VoiceAI.git
cd VoiceAI/VoiceAI-v1

# Download model files (required - not included due to size)
# Download from: https://huggingface.co/nvidia/parakeet-tdt-0.6b
# Place in: assets/parakeet-tdt-0.6b-v3-int8/
#   - encoder-model.int8.onnx
#   - decoder_joint-model.int8.onnx  
#   - nemo128.onnx

# Build (Windows PowerShell)
./build.ps1

# Install
adb install -r VoiceAI-v1.0.2.apk
```

**Requirements:**
- Android SDK (API 36)
- Android NDK 28
- Rust toolchain with `aarch64-linux-android` target
- Parakeet TDT 0.6B model files (~600MB)

---

## 📄 License

MIT License — See [LICENSE](LICENSE) for details.

---

<div align="center">

**VoiceAI** — Voice dictation, reimagined for Android.

*Offline. Private. Fast.*

</div>
