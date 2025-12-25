# Changelog

All notable changes to VoiceAI will be documented in this file.

## [1.2.1] - 2025-12-25

### Fixed - UI Polish 🎨
- **Ready status now truly centered** - Uses manual offset calculation for perfect centering
- **Glowing status icon** - Changed from ● to ◉ for better visibility
- **Larger toggle switches** - Increased from 100x50 to 130x60 with 52px thumb

### Added
- **Detailed accessibility instructions** - Shows "📋 Tap → Installed apps → VoiceAI → Enable"
- **Ko-fi support link** - https://ko-fi.com/ai_dev_2024 in app and README

### Changed
- Updated toggle button margins for proper thumb positioning
- All version references updated to 1.2.1

---

## [1.2.0] - 2025-12-25

### Added - Production Release 🎉
- **Clean UI Redesign** - Modern shadcn-style white interface with Inter-style fonts
  - Light gray background (#FAFAFA) with white cards
  - Card-based layout for main screen and settings
  - Green toggle switches (100x50), dark buttons, subtle borders
- **Offline LLM Model Download** - Qwen3 0.6B for fully offline AI post-processing
  - Download progress indicator with percentage
  - Model stored in app's private files directory (~400MB)
- **One-Click Groq API Setup** - "Get Free API Key" button opens console.groq.com/keys
- **Accessibility Status Indicator** - Green/red dot on main screen shows service status
- **Universal Text Injection via IME** - Uses `InputConnection.commitText()` like real keyboards
  - Works in ALL apps including Microsoft Word, Chrome, etc.
  - Same method FUTO Keyboard and SwiftKey use
- **Smart Clipboard Fallback** - For apps that don't support any injection method
  - Shows toast: "✓ Copied! Tap text field to paste"
- **Support Developer Link** - Ko-fi link embedded in Settings → About section

### Fixed
- Ready status now properly centered on main screen
- Word Android text injection (now uses clipboard + toast notification)

### Changed
- Removed accessibility section from Settings (now on main screen)
- Complete UI overhaul to clean white theme
- Reorganized main screen layout with Settings, Keyboard Setup, and Accessibility cards

---

## [1.1.1] - 2025-12-25

### Added
- **LocalLLMProcessor** - Fully offline ML post-processing (no internet required)
  - Enhanced rule-based fallback with grammar fixes
  - Contraction handling (dont → don't, cant → can't, etc.)
  - Question detection for proper punctuation
  - ONNX Runtime integration prepared for TinyLlama/Phi-2
- **`VoiceAIPipeline.createOffline()`** - New factory for offline-first operation
- **ProcessorTests.java** - 40+ comprehensive unit tests for all processors

### Fixed
- Android manifest version now matches CHANGELOG (1.0.1 → 1.1.0)

---

## [1.1.0] - 2025-12-14

### Added - Major Update 🚀
- **Modular Post-Processing Pipeline**: Complete refactor into 8 specialized processors
  - `CourseCorrector` - Wispr Flow's killer feature ("no wait" → clean output)
  - `CommandInterpreter` - Voice commands ("delete that", "new paragraph", punctuation)
  - `RepetitionCleaner` - Stutter and repetition removal
  - `FillerRemover` - Intelligent "uh", "um", "like" removal
  - `PersonalDictionaryApplicator` - FUTO-style custom words
  - `NumberNormalizer` - With fixed ordinal/currency edge cases
  - `PunctuationRestorer` - Question detection
  - `CasingApplicator` - Proper nouns, tech brands (ChatGPT, OpenAI, etc.)
- **Haptic Feedback** - Subtle vibration on dictation start/stop
- **Voice Commands** - "period", "comma", "new line", "new paragraph", "delete that"
- **LLMPostProcessor** - Phi-2 integration ready (future release)

### Fixed
- Ordinal over-matching ("first thing" no longer becomes "1st thing")
- Currency prefix doubling ("$100 dollars" → "$100")
- Punctuation cleanup issues

### Changed  
- Pipeline architecture: Chain of Responsibility pattern for clean separation
- Processor order optimized for best output quality
- Debug mode available for pipeline step logging

---

## [1.0.3] - 2025-12-11

### Added
- Settings UI with iOS-style toggles
- Version badge in Settings header
- Enhanced personal dictionary handling

---

## [1.0.2] - 2025-12-11

### Added
- Complete Stage 2 post-processing pipeline (`PostProcessor.java`)
- Number normalization: "twenty five" → "25"
- Time formatting: "four twenty pm" → "4:20 PM"
- Year detection: "twenty twenty four" → "2024"
- Ordinals: "twenty first" → "21st"
- Ranges: "one to five" → "1–5"
- Currency: "$100 USD", "$1,000,000"
- Percentages: "25%", "25.6%", "0.5%"
- Disfluency removal: "uh", "um", "you know", "like"
- Punctuation restoration
- Sentence casing with proper nouns
- `DictationController` for FUTO-style dictation management
- 30-second dictation limit toggle
- Silence detection auto-stop toggle
- Timer countdown display during dictation

### Fixed
- Preference key mismatch between Settings and DictationController
- Personal dictionary encoding issues (now uses `Pattern.quote()`)
- Text injection now works reliably with all apps

### Changed
- Unified preferences to use `VoiceAIPrefs`
- Enhanced README for GitHub with badges and tables

---

## [1.0.1] - 2025-12-10

### Added
- Personal dictionary feature
- Basic percentage and currency formatting
- Settings activity with tile-based UI

### Fixed
- Native library loading issues
- Package name consistency (`com.voiceai.app`)

---

## [1.0.0] - 2025-12-08

### Added
- Initial release
- Offline voice transcription with Parakeet TDT 0.6B
- Real-time waveform animation
- Accessibility service for text injection
- Support for HeliBoard and SwiftKey
