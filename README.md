# Aire — Context-Aware AI Assistant for Android

Aire is an intelligent Android companion designed to help you organize your life using the power of Claude AI. It seamlessly captures your world through text, photos, and voice, organizing it into a searchable "Memory Vault" that provides deep context for future assistance.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/compose-Material3-green.svg)](https://developer.android.com/jetpack/compose)

---

## ✨ Key Features

- **Unified AI Chat**: A single, polished interface to interact with Claude (Haiku, Sonnet, or Opus).
- **Aire Lens**: Use your camera to "explain" the physical world—parse receipts, extract business card info, or identify event flyers.
- **Voice Memories**: Hands-free capture with real-time transcription and automatic AI filing.
- **Memory Vault**: A persistent, local brain that uses Full Text Search (FTS) to recall your past notes, locations, and documents.
- **Spatial Awareness**: Automatically attaches location context to your memories and provides geographically relevant answers.
- **One-Tap Actions**: Claude proactively suggests real-world actions like "Add to Calendar," "Add to Contact," or "Search Maps."

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- A device running Android 8.0+ (API 26)
- An Anthropic API Key (obtain one at [console.anthropic.com](https://console.anthropic.com/))

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/TECWiSaRd/Aire.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and build the project.
4. Run the app on your physical device or emulator.

### Configuration
1. Open Aire on your device.
2. Tap the **Settings** gear icon in the top right.
3. Paste your **Anthropic API Key** and tap **Save Key**.
4. (Optional) Choose your preferred Claude model (Haiku is recommended for speed).
5. (Optional) Add a **Google API Key** for advanced Maps & Places features.

## 🛠 Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 & Dynamic Color.
- **AI**: [Anthropic Java SDK](https://github.com/anthropics/anthropic-sdk-java) powering Claude 3.5.
- **Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with SQLite FTS4 for indexing.
- **Camera**: [CameraX](https://developer.android.com/training/camerax) for high-performance visual capture.
- **Location**: [Google Play Services Location](https://developers.google.com/android/guides/setup) for GPS context.
- **Settings**: [Preference DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for general config and **EncryptedSharedPreferences** for secure API key storage.

## 🛡 Privacy & Security
- **Hardware-Backed Encryption**: Your API keys are stored using the Android Keystore system. They never leave the device and are not included in standard Android backups for maximum security.
- **On-Device Storage**: Your memories live in your device's private storage, not on our servers.
- **Intent-Based Actions**: Aire uses standard Android Intents for Calendar and Contacts, so you always review data before it's saved.
- **Optional GPS**: Location is only accessed with your permission and used to provide context to the AI.

## 🗺 Roadmap
- [ ] Full conversational voice mode.
- [ ] Advanced Places integration (photos, ratings).
- [ ] Gmail & Google Tasks sync.
- [ ] On-device semantic embeddings for offline retrieval.

---

Built with ❤️ for the Android community.
