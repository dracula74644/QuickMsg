# QuickMsg

**One-tap WhatsApp messages from your home screen.**

QuickMsg lets you send pre-configured WhatsApp messages to your partner or contacts with a single tap — no typing, no opening WhatsApp manually.

## Features

- **One-tap messaging** — Tap a home screen widget or in-app button to instantly send a WhatsApp message
- **30+ preset messages** — Status updates, quick replies, love messages, food plans, and more
- **Smart templates** — Use `{time}`, `{date}`, `{time_plus_15}` for auto-filled time-based messages
- **Multiple messages per widget** — Add several messages to one widget; tap to pick which to send
- **Customizable widgets** — Choose colors, sizes (small/medium/large), and labels
- **Contact picker** — Pick contacts from your phone's address book
- **Message history** — Track all sent messages with timestamps
- **Material 3 UI** — Clean, modern design with dynamic theming

## How It Works

1. Set up your partner's name and phone number on first launch
2. Tap any message card in the app to send it instantly
3. Add home screen widgets for your most-used messages
4. The app briefly opens WhatsApp, auto-taps send, and returns — all in under 2 seconds

## Download

📥 **[Download Latest APK](../../releases/latest)**

## Installation Guide

Since this app uses Android's Accessibility Service for auto-sending (the same mechanism used by Tasker, MacroDroid, and other automation apps), Google Play Protect may block the installation. This is normal for any app that uses accessibility services outside the Play Store.

### Step-by-step install:

1. **Download** the APK from the [Releases](../../releases/latest) page

2. **Temporarily pause Play Protect:**
   - Open the **Google Play Store** app
   - Tap your **profile icon** (top right)
   - Tap **Play Protect**
   - Tap the **⚙️ Settings gear** (top right)
   - Turn off **"Scan apps with Play Protect"**
   - Confirm by tapping **"Turn off"**

3. **Install the APK:**
   - Open the downloaded `QuickMsg.apk` file
   - Tap **"Install"**
   - If prompted about unknown sources, tap **"Settings"** and enable **"Allow from this source"**

4. **Re-enable Play Protect:**
   - Go back to Play Store → Profile → Play Protect → Settings
   - Turn **"Scan apps with Play Protect"** back on

5. **Enable the Accessibility Service:**
   - Open QuickMsg
   - Tap **"Enable"** on the auto-send card
   - Find **"QuickMsg"** in the accessibility list
   - Toggle it **on** and confirm

### Why does Play Protect block it?

Play Protect flags apps that use Android's Accessibility Service when installed outside the Play Store. This is a security precaution — accessibility services can interact with other apps, so Google is cautious. QuickMsg only uses this permission to tap WhatsApp's send button automatically. The app does not collect, store, or transmit any personal data.

## Permissions

| Permission | Why |
|---|---|
| **Accessibility Service** | Auto-taps WhatsApp's send button so you don't have to |
| **Read Contacts** | Optional — lets you pick contacts from your address book |
| **Internet** | Required by WhatsApp deep links |

## Building from Source

```bash
git clone https://github.com/YOUR_USERNAME/QuickMsg.git
cd QuickMsg
```

Open in Android Studio, or build from command line:

```bash
export ANDROID_HOME=~/Android/Sdk
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3 with dynamic theming
- Glance (home screen widgets)
- Room (local database)
- Accessibility Service API

## License

MIT License — free to use, modify, and distribute.
