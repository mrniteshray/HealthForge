# HealthForge-AiNity - Mobile App

Android app for health task management, assistant chat, analytics, and care connectivity.

## Architecture Notes

- Authentication and user profile are Firebase-first (Firebase Auth + Firestore).
- LLM features use Google Gemini through `generativeai` SDK.
- Legacy custom backend and Cerebras integrations were removed.

## Required Local Setup

1. Add Firebase config file at `app/google-services.json`.
2. Add your Gemini API key to `local.properties`:

```properties
GEMINI_API_KEY=your_actual_gemini_api_key
```

If `GEMINI_API_KEY` is missing, app startup will fail fast with a clear error.

## Build

```bash
./gradlew :app:assembleDebug
```