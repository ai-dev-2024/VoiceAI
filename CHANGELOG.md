# Changelog

All notable changes to VoiceAI will be documented in this file.

## [1.0.2] - 2024-12-11

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
- Version badge in Settings (v1.0.2)

### Fixed
- Preference key mismatch between Settings and DictationController
- Personal dictionary encoding issues (now uses `Pattern.quote()`)
- Text injection now works reliably with all apps

### Changed
- Unified preferences to use `VoiceAIPrefs`
- Enhanced README for GitHub with badges and tables

## [1.0.1] - 2024-12-10

### Added
- Personal dictionary feature
- Basic percentage and currency formatting
- Settings activity with tile-based UI

### Fixed
- Native library loading issues
- Package name consistency (`com.voiceai.app`)

## [1.0.0] - 2024-12-08

### Added
- Initial release
- Offline voice transcription with Parakeet TDT 0.6B
- Real-time waveform animation
- Accessibility service for text injection
- Support for HeliBoard and SwiftKey
