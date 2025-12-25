# 🎤 VoiceAI

<div align="center">

![VoiceAI Hero Banner](docs/images/hero_banner.png)

**A Wispr Flow offline alternative for Android**

Fully local voice dictation with advanced AI post-processing

[![Release](https://img.shields.io/badge/Release-v1.2.1-brightgreen?style=for-the-badge)](../../releases/latest)
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

## 🆕 What's New in v1.2.1

| Feature | Description |
|---------|-------------|
| 🔢 **Sequential Digit Conversion** | "one two three four five" → `12345` (Wispr Flow-style) |
| 💰 **Enhanced Currency Formatting** | "thirty million US dollars" → `$30 million USD` |
| 🧠 **Offline LLM Activation Fixed** | Settings toggle now properly activates offline processing |
| 📥 **Fixed Model Download URL** | Qwen3 ~405MB model downloads correctly from Hugging Face |
| 🔄 **Improved Post-Processing** | Filler removal, grammar fixes, smart punctuation |

---

## ✨ Features

<div align="center">

| Feature | Description |
|---------|-------------|
| 🔒 **100% Offline** | No internet required — all processing on-device |
| ⚡ **Fast Transcription** | NVIDIA Parakeet TDT 0.6B model (int8 quantized) |
| 🧠 **Offline LLM** | Qwen3 0.6B (~405MB) for AI post-processing without internet |
| 🔢 **Smart Numbers** | "one two three four" → `1234` (phone numbers, IDs) |
| 💰 **Currency Formatting** | "$30 million USD", "25%", "$100" |
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

1. **Download** [`VoiceAI-v1.2.1.apk`](../../releases/latest) from Releases
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
| "thirty million US dollars" | **$30 million USD** |
| "one hundred dollars" | **$100** |
| "microphone testing one two three four" | **Microphone testing 1234** |
| "twenty twenty four" | **2024** |
| "four twenty pm" | **4:20 PM** |
| "uh so i was thinking um" | **So, I was thinking** |

---

## ⚙️ Settings

Access via **VoiceAI app → Open Settings**:

| Setting | Description |
|---------|-------------|
| ⏱️ **30-Second Limit** | Auto-stop dictation after 30 seconds |
| 🔇 **Silence Detection** | Auto-stop when you stop speaking |
| 📖 **Personal Dictionary** | Add custom words (e.g., `@Groq, ChatGPT, Anthropic`) |
| 🧠 **Offline LLM** | Enable on-device AI post-processing |
| 🔑 **Groq API Key** | Optional cloud LLM for enhanced formatting |

---

## 🧠 AI Processing Options

VoiceAI offers **two AI processing modes** for intelligent text formatting:

### Option 1: Offline LLM (Recommended) 🔒

**Fully private, no internet required**

| Model | Size | Source |
|-------|------|--------|
| **Qwen3 0.6B Q4** | ~405 MB | [Hugging Face](https://huggingface.co/unsloth/Qwen3-0.6B-GGUF) |

**Features:**
- ✅ Filler word removal ("um", "uh", "like")
- ✅ Grammar corrections (contractions, "i" → "I")
- ✅ Smart punctuation and question detection
- ✅ Sequential digit conversion ("one two three" → "123")
- ✅ Currency formatting ("$30 million USD")

**Setup:** Settings → Offline Processing → Download (~405 MB, one-time)

### Option 2: Groq API (Cloud) ☁️

**Faster, more accurate, requires internet**

| Provider | Model | Speed |
|----------|-------|-------|
| [Groq](https://console.groq.com/keys) | Llama 3.1 70B | ~500ms |

**Setup:** 
1. Get free API key at [console.groq.com/keys](https://console.groq.com/keys)
2. Paste in Settings → API Key

> 💡 **Tip:** Use Offline LLM for privacy, Groq API for best quality

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

### AI Models
| Component | Model | Size |
|-----------|-------|------|
| **Speech-to-Text** | NVIDIA Parakeet TDT 0.6B (int8) | ~470 MB |
| **Offline LLM** | Qwen3 0.6B Q4_K_XL | ~405 MB |
| **Cloud LLM** | Groq Llama 3.1 70B | API |

### Build
- **Cargo** — Rust package manager
- **Android SDK/NDK** — Native compilation
- **ONNX Runtime** — Neural network inference

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
adb install -r VoiceAI-v1.2.1.apk
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
