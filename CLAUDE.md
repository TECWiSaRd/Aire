# Aire — Android (Pixel-first)

## Overview

An Android app, tailored for Google Pixel devices, that uses AI (Claude) to accept
multimodal inputs — photos, voice, text, and shared content — and automatically
organizes them into a searchable personal memory. The user can later ask natural
language questions and get synthesized answers pulled from that memory, not just a
list of raw records.

**Core example:** User photographs a receipt. Claude parses it (vendor, date,
amount, category) and stores a condensed structured record. Later, the user asks
"How much did I spend on food?" and the app retrieves relevant records and has
Claude synthesize an answer.

This is one use case among several — see "Use Cases" below.

## Architecture

### Input layer (multimodal capture)
- Camera/gallery intent for images (receipts, whiteboards, business cards, product
  labels, screenshots)
- Voice input via speech-to-text for quick verbal notes/reminders
- Text quick-entry widget
- Android share-sheet target so other apps (Gmail, browser, Messages, etc.) can
  send content directly into the organizer

### Processing layer
- Each input is sent to the Claude API for parsing/extraction into structured JSON
  (e.g., vendor/date/amount/category for a receipt; sender/date/action-items for an
  email screenshot)
- Original media (e.g., the receipt photo) is retained for occasional "show me the
  source" lookups, but the condensed structured record is the primary object stored
  and queried
- Local storage: Room database (SQLite), plus embeddings/vector search for semantic
  recall (so "food" matches records tagged "restaurant," "grocery," "coffee," etc.
  without exact keyword match)

### Recall layer
- Natural-language query box (or voice) searches stored records (semantic +
  keyword), then Claude synthesizes a direct answer from the matches rather than
  returning a raw list

## Use Cases (brainstorm)

**Documents & paperwork**
- Bills/invoices → extract due date → optionally create a Calendar/Tasks reminder
- Warranty cards/manuals → store purchase date + warranty length → proactive expiry
  reminders
- Business cards → parse into a Contacts entry via the Contacts Provider API

**Email & communication (Gmail API)**
- Ask about past email threads ("did I get a reply from the landlord?") based on
  passively summarized/tagged emails (opt-in)
- Auto-draft replies to routine emails, saved as Gmail drafts for review
- Morning digest of actionable inbox items

**Calendar & reminders (Google Calendar API / Google Tasks API)**
- Photograph a flyer/poster/screenshot with event info → extract date/time/location
  → one-tap add to calendar
- Voice memo → parsed into a Google Task with due date
- Cross-reference calendar + related notes for a given day

**Shopping & finance**
- Aggregate receipts into spending categories over time
- Track subscriptions/spending from statement screenshots
- Save price-tag photos of items being considered; compare later
- Store loyalty card / gift card barcodes from a single photo

**Home & physical objects**
- Possible overlap with the separate NFC storage app project (physical item
  photo-parsing pipeline may be shared) — TBD whether these stay separate apps
- Photograph an appliance's model/serial plate → fetch manual link, store for later
  troubleshooting queries

**Health & personal tracking**
- Prescription label photos → refill/dosage reminders
- Note: health data should be treated carefully — default to fully on-device
  storage, no cloud sync, for this category

**Travel**
- Boarding passes, hotel confirmations, itineraries → build a trip timeline,
  surface relevant info contextually

## Pixel-Specific Integration

- App Actions / Google Assistant integration for voice-triggered capture or query
- At a Glance / lock screen widgets to surface next reminder or attention summary
- Persistent quick-capture notification/tile ("snap and file") without opening the
  app
- Material You dynamic theming for a native feel

## Open Design Questions

- **Storage scope:** fully on-device (Room/SQLite) vs. syncing to a backend for
  cross-device access
- **Privacy boundaries:** what's opt-in per data category (receipts vs. email vs.
  health), and what should never leave the device


## Status

Aire has evolved into a context-aware **AI Assistant** for Android:
- **Unified Chat UI**: A polished, **Edge-to-Edge** conversation interface with integrated multimodal entry points.
- **Multimodal capture**: Seamlessly capture and analyze photos via **Lens** (CameraX) and voice memos via native **Speech-to-Text**.
- **On-Device Settings**: Secure **API key storage** (DataStore), **AI Model Switcher** (Haiku, Sonnet, Opus), and appearance customization.
- **Proactive Intelligence**: Claude explains inputs and suggests interactive **Action Chips** (e.g., "Save to Memory", "Add to Calendar") in real-time.
- **Efficient Context**: Your "Memory Vault" (Room FTS) is automatically used as background context for every interaction, ensuring Aire remembers your history.
- **Robust Storage**: Persistent record-keeping with lightning-fast local Full Text Search.

**Next Steps**:
- Implement deep system integrations for suggested actions (Google Calendar, Contacts, etc.).
- Add location-aware capabilities to provide geographically relevant assistance.
- Enhance the voice experience with a full conversational mode.
- Explore on-device embeddings for privacy-first semantic retrieval.
