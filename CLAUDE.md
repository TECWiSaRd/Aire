# Aire — Context-Aware AI Assistant for Android

## Overview

Aire is a modern AI Assistant tailored for Android (Pixel-first) that acts as an intelligent companion. Unlike standard assistants, Aire is built around a **Memory Vault** and **Multimodal Awareness**, allowing it to explain the world through your camera, remember your past, and perform real-world actions like scheduling meetings or filing expenses.

**Core Experience:** The user interacts through a unified chat interface. Claude (the brain) has constant access to the user's current context (Location, Camera, Voice) and historical context (Saved Memories) to provide deeply personalized assistance.

## Architecture

### 1. Multimodal Input Layer
- **Unified Chat**: A single conversation interface for Text, Voice, and Visual queries.
- **Aire Lens**: Integrated **CameraX** implementation for "Google Lens-style" visual analysis of documents, receipts, and physical objects.
- **Voice Capture**: Native **Speech-to-Text** with real-time transcription feedback for hands-free interaction.
- **Context Preview**: Visual thumbnails and transcription bars provide immediate feedback before sending context to the AI.

### 2. Processing Layer (Assistant Brain)
- **AssistantService**: A unified AI pipeline using **Claude (Sonnet 4.6, Haiku, or Opus)**.
- **Multimodal Extraction**: Claude simultaneously explains visual inputs, answers natural language questions, and extracts structured data for memories or actions.
- **On-Device Configuration**: Secure storage of API keys (Anthropic & Google) using **Preference DataStore**, with a dynamic model switcher in Settings.

### 3. Recall & Integration Layer
- **Memory Vault**: Persistent **Room** database with **Full Text Search (FTS4)**. Records are automatically indexed by title, summary, tags, and **Location**.
- **Spatial Awareness**: Integrated **GPS (FusedLocationProvider)**. Aire understands the user's current neighborhood/city and attaches location context to memories.
- **IntegrationManager**: Executes proactive **Action Chips** (e.g., "Add to Calendar", "Maps Search", "Add to Contact") via native Android Intents.

## Capabilities & Use Cases

- **"Ask Aire" (Recall)**: "How much did I spend at that cafe in Palo Alto?" (Retrieves relevant receipt memories + Location context).
- **"Explain This" (Lens)**: Point the camera at a flyer → "Identified a local concert. Would you like me to add this to your calendar?" (Visual parsing → Intent execution).
- **"File It" (Memory)**: Speak a note → "Captured your reminder about the landlord. I've filed it in your Vault." (Voice → Structured storage).
- **"What's Nearby?" (Location)**: "Any good sushi around here?" → Suggests places based on GPS and offers a "Maps Search" action.

## Pixel-Specific Integration (Goals)

- **Material You**: Dynamic theming based on user wallpaper (implemented).
- **Edge-to-Edge**: Modern immersive UI drawing behind system bars (implemented).
- **Assistant Integration**: Voice-triggered capture and lock-screen widgets (Next steps).

## Status

Aire has successfully pivoted into a high-performance **AI Assistant**:
- **UI**: Polished, **Edge-to-Edge** chat interface with interactive action chips.
- **Capture**: Fully multimodal—Text, **Lens (Camera)**, and **Voice (Mic)** are all operational.
- **Intelligence**: Context-aware pipeline with location-stamped memories and system intent integration.
- **Control**: Comprehensive Settings for on-device API keys, models, and appearance.
- **Scalability**: Persistent storage with FTS indexing for efficient, long-term memory.

**Current Position**: Core Assistant foundation and multimodal capture are complete. Deep system integration (Intents) for Calendar, Contacts, and Maps is fully coded.

**Next Steps**:
- Enhance the voice experience with a full conversational/hands-free mode.
- Add advanced Places data (ratings, photos) once Google API key is provided.
- Explore on-device embeddings for privacy-first semantic retrieval.
- Expand system integrations to include Gmail (Summaries) and Tasks.
