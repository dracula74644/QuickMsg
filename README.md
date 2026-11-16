# QuickMsg

**One-tap WhatsApp messages from your home screen.**

QuickMsg lets you send pre-configured WhatsApp messages to your loved ones with a single tap — no typing, no opening WhatsApp manually.

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

1. Set up your loved one's name and phone number on first launch
2. Tap any message card in the app to send it instantly
3. Add home screen widgets for your most-used messages
4. The app briefly opens WhatsApp, auto-taps send, and returns — all in under 2 seconds

## Download

📥 **[Download Latest APK](../../releases/latest)**

## Installation and updates

Download the APK from [the latest release](https://github.com/dracula74644/QuickMsg/releases/latest) and install it over the existing app to keep your data. Do not uninstall first. Enable QuickMsg in Android Accessibility settings for automatic sending.

Starting with version 1.2, QuickMsg checks the latest GitHub release when opened (at most once per hour) and approximately every six hours in the background. A newer release shows a Download banner; background checks can also show a notification if notifications are allowed. Settings includes a manual update check. Android may delay background checks. The app opens the release APK in your browser; you approve downloading and installation. Versions before 1.2 need one manual update to gain this feature.

If Play Protect blocks an APK, record the exact warning and report it to the maintainer. A scan request, sensitive-permission block, and harmful-app classification have different remedies. See [Google’s guidance and appeal process](https://developers.google.com/android/play-protect/warning-dev-guidance). Disabling Play Protect is not the recommended installation procedure.

QuickMsg stores your selected contact, widget configurations, and message history locally. Its accessibility service inspects WhatsApp’s interface to find and activate Send. Update checks connect to GitHub; they do not upload contacts or message history.

## Permissions

| Permission | Why |
|---|---|
| **Accessibility Service** | Auto-taps WhatsApp's send button so you don't have to |
| **Read Contacts** | Optional — lets you pick contacts from your address book |
| **Internet** | Checks GitHub for app updates |
| **Notifications** | Alerts when accessibility turns off or an update is available |

## Building from Source

```bash
git clone https://github.com/dracula74644/QuickMsg.git
cd QuickMsg
```

Open in Android Studio, or build from command line:

```bash
export ANDROID_HOME=~/Android/Sdk
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

## Publishing an update

A source commit alone does not notify users. Publish a stable GitHub Release with both its signed APK and `update.json`:

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Keep the existing release signing key; a different key cannot update existing installations normally.
3. Run `./gradlew testDebugUnitTest assembleRelease`, then `python3 tools/prepare_release.py`.
4. Commit and push the source. Create a draft release tagged `v<version>` at that commit and upload both files from `build/release/v<version>/`.
5. Publish the draft as the latest stable release only after both assets finish uploading.

The app reads `https://github.com/dracula74644/QuickMsg/releases/latest/download/update.json`, compares the numeric `versionCode`, and offers only newer versions. Do not replace an old release with different contents or reuse its version code. The manifest includes the APK SHA-256 for external verification; the app delegates downloading and installation to the browser and Android installer.

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3 with dynamic theming
- Glance (home screen widgets)
- Room (local database)
- Accessibility Service API

## License

MIT License — free to use, modify, and distribute.
