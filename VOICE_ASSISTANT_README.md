# Healthcare Voice Assistant - HealthForge

## Overview
The Healthcare Voice Assistant is a fully integrated AI-powered voice interface that provides natural conversations about health-related topics using advanced speech recognition, natural language processing, and text-to-speech technologies.

## Features

### 🎤 **Advanced Voice Pipeline**
- **Voice Activity Detection (VAD)**: Automatically detects when you start and stop speaking
- **Speech-to-Text (STT)**: Converts your speech to text using Android's built-in speech recognition
- **AI Processing**: Uses Cerebras AI for intelligent healthcare responses
- **Text-to-Speech (TTS)**: Natural voice responses with healthcare-optimized speech settings

### 🔄 **Natural Conversation Flow**
- **Real-time Feedback**: See your speech being transcribed in real-time
- **Voice Level Indicator**: Visual feedback showing your voice input level
- **Continuous Listening**: After responding, automatically ready for your next question
- **Smart State Management**: Clear visual indicators of what the assistant is doing

### 🏥 **Healthcare-Focused Responses**
- **Professional Medical Guidance**: Provides accurate health information
- **Safety First**: Always recommends consulting healthcare professionals for serious concerns
- **Conversational**: Natural, easy-to-understand language
- **Concise**: Clear, brief responses optimized for voice interaction

## How to Use

### 1. **Grant Permissions**
- The app will request microphone permission when you first try to use voice features
- This is required for speech recognition to work

### 2. **Start Conversation**
- Tap the microphone button to start listening
- The interface will show "Listening..." with animated visual feedback
- Speak clearly into your device's microphone

### 3. **Voice Interaction**
- The app shows real-time transcription as you speak
- After you finish speaking, it processes your question
- You'll see "Thinking..." while the AI generates a response
- The assistant will speak the response back to you
- Automatically ready for your next question

### 4. **Example Questions**
Try asking things like:
- "What should I eat for breakfast?"
- "How much water should I drink daily?"
- "What are the symptoms of fever?"
- "Tell me about healthy sleep habits"
- "How can I reduce stress?"
- "What exercises are good for back pain?"

## Technical Architecture

### Components
1. **VoiceActivityDetector**: Monitors audio input for speech detection
2. **HealthcareSTT**: Handles speech-to-text conversion
3. **HealthcareVoicePipeline**: Orchestrates the entire voice interaction flow
4. **CerebrasApi**: AI service for generating healthcare responses
5. **HealthcareTTS**: Converts AI responses back to speech
6. **VoiceAgentViewModel**: Manages UI state and user interactions

### Pipeline States
- **IDLE**: Ready to start listening
- **LISTENING**: Actively listening for your voice
- **PROCESSING**: Analyzing your question and generating response
- **SPEAKING**: Playing back the AI response

## Privacy & Safety

### Data Handling
- Voice data is processed locally and through secure APIs
- No persistent storage of voice recordings
- Conversations are not permanently stored

### Medical Disclaimer
- This assistant provides general health information only
- Always consult qualified healthcare professionals for medical advice
- Not intended for emergency situations or medical diagnosis
- Cannot prescribe medications or provide specific medical treatments

## Troubleshooting

### Common Issues

**"Microphone permission required"**
- Go to your device settings → Apps → HealthForge → Permissions
- Enable "Microphone" permission

**Not detecting speech**
- Ensure you're speaking clearly and loudly enough
- Check your device's microphone isn't blocked
- Try speaking closer to the device

**No voice response**
- Check device volume settings
- Ensure text-to-speech is enabled in system settings
- Try restarting the app

**Slow responses**
- Ensure good internet connection for AI processing
- Complex questions may take longer to process

## Support
For technical issues or questions about the healthcare voice assistant, please contact the HealthForge support team.

---
*Built with modern Android technologies: Jetpack Compose, Hilt DI, Kotlin Coroutines, and Cerebras AI*