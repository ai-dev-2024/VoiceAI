# Changelog

All notable changes to VoiceAI will be documented in this file.

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
