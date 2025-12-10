# Changelog

All notable changes to VoiceAI will be documented in this file.

## [1.0.1] - 2025-12-10

### Added
- **Percentage formatting**: "10 percent" → "10%"
- **Currency formatting**: "30 dollars" → "$30"
- **Dictionary integration**: @ symbol support ("at grok" → "@grok")
- **Native Settings Activity** with proper keyboard support

### Changed
- Settings button now larger and more prominent
- Improved Settings UI with iOS-style tile layout

### Removed
- Live Translate/Subtitles feature (not in use case)
- Legacy dev/notune code

### Fixed
- Personal dictionary auto-save now works correctly
- Toggle switches for 30-second limit and silence detection

## [1.0.0] - 2025-12-09

### Added
- **Voice Dictation Overlay** - Dark transparent overlay UI for voice input
  - Works with SwiftKey, Gboard, HeliBoard, and other keyboards
  - Animated sound wave visualization
  - Silence detection auto-stop (~2 seconds)
  - Live transcription display during processing
  
- **Settings**
  - 30-second time limit toggle
  - Auto-stop on silence toggle
  - Personal dictionary (comma-separated words)

- **Post-Processing**
  - AI model name corrections (Groq, Gemini, ChatGPT, OpenAI, Claude, Anthropic, Llama, Mistral, Qwen)
  - Personal dictionary word preservation

- **Core Features**
  - Bundled Parakeet TDT 0.6B v3 INT8 model (~670MB)
  - Fast offline transcription via ONNX Runtime
  - Android RecognitionService integration
  - InputMethodService (IME) support

### Technical
- Built with Rust (eframe/egui) + Java
- Cross-compiled for aarch64-linux-android
- ONNX Runtime optimized for ARM64

## Credits

- **transcribe-rs** by [cjpais](https://github.com/cjpais) - Rust transcription engine
- **Parakeet TDT** by NVIDIA - Speech recognition model
- **ONNX Runtime** by Microsoft - Inference engine
- **notune** by FUTO - Original Android implementation reference
