# Gemini AI Chat - Jetpack Compose

A Gemini-powered Android chat application built using **Kotlin** and **Jetpack Compose**. This project is based on the Gemini Jetpack Compose starter application and is being enhanced with a modern Material 3 interface, improved state management, voice input, persistent chat history, responsive UI, and secure API key handling.

## Features

- Gemini AI-powered conversations
- Jetpack Compose UI
- Material 3 design
- Conversation displayed using `LazyColumn`
- User and Gemini chat bubbles
- Automatic scrolling to the latest message
- Loading and error states
- Voice input using Speech-to-Text
- Persistent chat history using Room
- User preferences using DataStore
- Responsive layouts for different screen sizes
- Dark mode support
- Secure API key management
- Android Keystore-based API key encryption
- R8 release obfuscation
- Unit and Compose UI testing

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Design:** Material 3
- **Architecture:** MVVM
- **AI:** Google Gemini API
- **Database:** Room
- **Preferences:** DataStore
- **Security:** Android Keystore + AES-256-GCM
- **Testing:** JUnit, Kotlin Coroutines Test, Compose UI Testing
- **Build:** Gradle
- **Version Control:** Git & GitHub

## Project Architecture

```text
MainActivity
      |
      v
  ChatScreen
      |
      v
 ChatViewModel
      |
      v
  ChatUiState
      |
      v
 Repository Layer
      |
      +----------------+
      |                |
      v                v
Gemini API          Room DB
                       |
                       v
                 Chat History

DataStore
   |
   v
User Preferences
