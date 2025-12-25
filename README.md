# 🎤 VoiceAI

<div align="center">

![VoiceAI Hero Banner](docs/images/hero_banner.png)

**A Wispr Flow offline alternative for Android**

Fully local voice dictation with advanced AI post-processing

[![Release](https://img.shields.io/badge/Release-v1.2.0-brightgreen?style=for-the-badge)](../../releases/latest)
[![Built with Antigravity](https://img.shields.io/badge/Built%20with-Google%20Antigravity-4285F4?style=for-the-badge&logo=google)](https://developers.google.com/project-antigravity)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-min%20API%2026-3DDC84?style=for-the-badge&logo=android)](https://developer.android.com)

</div>

---

## 🖼️ Screenshots

<div align="center">
<table>
<tr>
<td align="center"><img src="screenshots/main_screen.png" alt="Main Screen" width="280"/><br/><b>Main Screen</b></td>
<td align="center"><img src="screenshots/settings_screen.png" alt="Settings" width="280"/><br/><b>Settings</b></td>
</tr>
</table>
</div>

---

## 🆕 What's New in v1.2.0

| Feature | Description |
|---------|-------------|
| 🎨 **Clean UI Redesign** | Modern shadcn-style white interface with Inter-style fonts |
| 🧠 **Offline LLM Model** | Download Qwen3 0.6B for fully offline AI post-processing |
| 🔗 **One-Click API Setup** | "Get Free API Key" button opens Groq console directly |
| ✅ **Accessibility Status** | Green/red indicator shows Text Injection Service status |
| 🚀 **Universal Text Injection** | Uses `InputConnection.commitText()` like real keyboards |
| ⌨️ **Organized Layout** | Card-based main screen with clear sections |
| 📋 **Smart Clipboard** | "✓ Copied! Tap text field to paste" notification |

---

## ✨ Features

<div align="center">

| Feature | Description |
|---------|-------------|
| 🔒 **100% Offline** | No internet required — all processing on-device |
| ⚡ **Fast Transcription** | NVIDIA Parakeet TDT 0.6B model (int8 quantized) |
| 🧠 **Offline LLM** | Qwen3 0.6B for AI post-processing without internet |
| 🎯 **Course Correction** | "No wait, I mean..." → Clean, corrected output |
| 🗣️ **Voice Commands** | "Period", "comma", "new line", "delete that" |
| 📖 **Personal Dictionary** | FUTO-style custom word replacements |
| ⏱️ **30-Second Timer** | Optional auto-stop after 30 seconds |
| 🔇 **Silence Detection** | Auto-stop when you stop speaking |
| 🌐 **Universal Injection** | Works with any app via Accessibility Service |

</div>

---

## 📱 Compatible Keyboards

| Keyboard | Status |
|----------|--------|
| [HeliBoard](https://github.com/Helium314/HeliBoard) | ✅ Tested & Working |
| [SwiftKey](https://www.microsoft.com/swiftkey) | ✅ Tested & Working |
| [OpenBoard](https://github.com/openboard-team/openboard) | 🔄 Should work |
| [FlorisBoard](https://github.com/florisboard/florisboard) | 🔄 Should work |
| [AnySoftKeyboard](https://github.com/AnySoftKeyboard/AnySoftKeyboard) | 🔄 Should work |

> **Note:** Only HeliBoard and SwiftKey have been tested. Other open-source keyboards with voice input support should be compatible.

---

## 🚀 Quick Start

### Download & Install

1. **Download** [`VoiceAI-v1.2.0.apk`](../../releases/latest) from Releases
2. **Install** on your Android device
3. **Enable** in Settings → Language & Input → Keyboards
4. **Enable** Accessibility Service for text injection
5. **Grant** microphone permission

### Usage

1. Open any text field in any app
2. Tap the **microphone button** on your keyboard
3. Speak naturally — use voice commands if needed
4. Tap screen or wait for auto-stop

---

## 🎯 Post-Processing Examples

### Course Correction (NEW!)

| You Say | VoiceAI Outputs |
|---------|-----------------|
| "Let's meet tomorrow no wait let's do Friday" | **Let's do Friday.** |
| "I think um actually never mind I mean yes" | **Yes.** |
| "Send to John no sorry to Mike" | **Send to Mike.** |

### Voice Commands (NEW!)

| You Say | VoiceAI Does |
|---------|--------------|
| "Hello comma how are you question mark" | **Hello, how are you?** |
| "New paragraph" | Inserts paragraph break |
| "Delete that" | Removes last dictation |

### Smart Formatting

| You Say | VoiceAI Outputs |
|---------|-----------------|
| "twenty five percent" | **25%** |
| "one hundred US dollars" | **$100 USD** |
| "twenty twenty four" | **2024** |
| "four twenty pm" | **4:20 PM** |
| "uh so i was thinking um" | **So, I was thinking** |

---

## ⚙️ Settings

Access via **VoiceAI app → Open Settings**:

- **⏱️ 30-Second Limit** — Auto-stop after 30 seconds
- **🔇 Silence Detection** — Auto-stop when you stop speaking  
- **📖 Personal Dictionary** — Add custom words (e.g., `@Groq, ChatGPT, Anthropic`)
- **🔑 Groq API Key** — Optional LLM-powered post-processing

---

## 🏗️ Architecture

```
VoiceAIPipeline (Chain of Responsibility)
├── CommandInterpreter     # Voice commands first
├── CourseCorrector        # "No wait" handling
├── RepetitionCleaner      # Stutter removal
├── PersonalDictionary     # Custom words
├── FillerRemover          # "Uh", "um" removal
├── NumberNormalizer       # "25" from "twenty five"
├── PunctuationRestorer    # Add periods, commas
└── CasingApplicator       # Proper nouns
```

---

## 🛠️ Tech Stack

### Frontend
- **Rust** + `egui` — Native Android UI
- **Java** — Activities, Services, Accessibility

### Backend / AI
- **ONNX Runtime** — Neural network inference
- **Parakeet TDT 0.6B** — NVIDIA's speech-to-text model
- **Phi-2 (Planned)** — On-device LLM for AI commands

### Build
- **Cargo** — Rust package manager
- **Android SDK/NDK** — Native compilation

---

## 🏗️ Building from Source

```bash
# Clone
git clone https://github.com/ai-dev-2024/VoiceAI.git
cd VoiceAI

# Download model files (required)
# From: https://huggingface.co/nvidia/parakeet-tdt-0.6b
# Place in: assets/parakeet-tdt-0.6b-v3-int8/

# Build (Windows PowerShell)
./build.ps1

# Install
adb install -r VoiceAI-v1.2.0.apk
```

**Requirements:**
- Android SDK (API 36)
- Android NDK 28
- Rust toolchain with `aarch64-linux-android` target
- Parakeet TDT 0.6B model files (~600MB)

---

## 🙏 Credits & Acknowledgments

| Project | Contribution |
|---------|--------------|
| [**transcribe-rs**](https://github.com/handy-audio/transcribe-rs) | Core ASR Rust library |
| [**FUTO Voice Input**](https://gitlab.futo.org/alex/voiceinput) | Personal dictionary inspiration |
| [**Wispr Flow**](https://wispr.com/flow) | Course correction concept |
| **NVIDIA NeMo** | Parakeet TDT speech model |
| **Microsoft Phi-2** | On-device LLM (planned) |

---

## ☕ Support

If you find VoiceAI useful, consider supporting the development:

[![Ko-fi](https://img.shields.io/badge/Buy%20me%20a%20coffee-Ko--fi-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/ai_dev_2024)

---

## 📄 License

MIT License — See [LICENSE](LICENSE) for details.

---

<div align="center">

**VoiceAI** — Voice dictation, reimagined for Android.

*Offline. Private. Fast.*

⭐ **Star this repo if you find it useful!** ⭐

</div>
