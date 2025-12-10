# VoiceAI 🎤

**A Wispr Flow offline alternative for Android** — Fully local voice dictation with advanced post-processing.

> Built with [Google Antigravity](https://developers.google.com/project-antigravity) AI coding assistant

---

## ✨ Features

- **100% Offline** — No internet required, all processing happens on-device
- **Fast Transcription** — Uses NVIDIA Parakeet TDT 0.6B model (int8 quantized)
- **Advanced Post-Processing** — Automatic punctuation, casing, number formatting
- **Personal Dictionary** — FUTO-style custom word replacements
- **Smart Dictation Controls** — 30-second limit toggle, silence detection auto-stop
- **Universal Text Injection** — Works with any app via Accessibility Service

## 📱 Tested Keyboards

- ✅ **HeliBoard** — Open-source keyboard
- ✅ **SwiftKey** — Microsoft keyboard

## 🛠️ Tech Stack

### Frontend
- **Rust** — Native Android app with `egui` for main UI
- **Java** — Android activities, services, and accessibility

### Backend / AI
- **ONNX Runtime** — Neural network inference
- **Parakeet TDT 0.6B** — NVIDIA's speech-to-text model (int8 quantized)
- **transcribe-rs** — Rust transcription library

### Build System
- **Cargo** — Rust package manager
- **Android SDK/NDK** — Native compilation
- **PowerShell** — Windows build script

## 🙏 Credits & Acknowledgments

This project is built upon and inspired by:

- **[transcribe-rs](https://github.com/handy-audio/transcribe-rs)** by Handy Audio — Rust transcription library that powers the core ASR functionality
- **[FUTO Voice Input](https://gitlab.futo.org/alex/voiceinput)** by FUTO — Inspiration for the personal dictionary UI and dictation settings design
- **[Wispr Flow](https://wispr.com/flow)** — The original desktop voice dictation app that inspired this Android alternative
- **NVIDIA NeMo** — For the Parakeet TDT speech recognition model

## 📦 Installation

### From APK
1. Download `VoiceAI-v1.0.1.apk` from Releases
2. Install on your Android device
3. Enable VoiceAI in Settings → Language & Input → Keyboards
4. Enable Accessibility Service for text injection
5. Grant microphone permission

### From Source
```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/VoiceAI.git
cd VoiceAI/VoiceAI-v1

# Build (Windows PowerShell)
./build.ps1

# Install
adb install -r VoiceAI-v1.0.1.apk
```

## 📖 Usage

1. Open any text field in any app
2. Tap the microphone button on your keyboard (HeliBoard, SwiftKey, etc.)
3. Speak naturally — VoiceAI will transcribe and insert text
4. Tap to stop or wait for silence detection / time limit

### Post-Processing Examples

| You Say | VoiceAI Outputs |
|---------|-----------------|
| "twenty five percent" | 25% |
| "one hundred US dollars" | $100 USD |
| "twenty twenty four" | 2024 |
| "the meeting is at four twenty pm" | the meeting is at 4:20 PM |
| "uh so i was thinking um" | So, I was thinking |

## ⚙️ Settings

Access via the main app → **Open Settings**:

- **30-Second Dictation Limit** — Auto-stop after 30 seconds
- **Silence Detection** — Auto-stop when you stop speaking
- **Personal Dictionary** — Add custom words (e.g., `@Groq, ChatGPT, Anthropic`)

## 📄 License

MIT License — See [LICENSE](LICENSE) for details.

---

**VoiceAI** — Voice dictation, reimagined for Android. Offline. Private. Fast.
