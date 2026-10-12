# Multi AI — Open-source Android assistant starter (v0.2)

**Package:** `com.sabiktech.multiai`  
**License:** MIT (you may reuse/modify this starter; see LICENSE)

## Included in this starter
- Android Kotlin source code
- Button-triggered speech recognition (Bangla preference)
- Text-to-speech replies
- Flashlight toggle, media volume up, Settings shortcut
- GitHub Actions workflow to compile a debug APK in the cloud

## Important: current capabilities
This is a starter prototype, **not yet a complete Bixby replacement**.
- No always-on “Hey Multi AI” wake-word service yet.
- Basic online AI chat using Gemini API is wired in; you must supply your own API key. Free-tier availability and quotas can change.
- No WhatsApp reading/auto-reply, Bixby Routines clone, or full system-level control.
- Android restricts ordinary apps from directly changing some settings (for example Wi-Fi/Bluetooth toggles on modern Android), reading private app data, or silently controlling calls.

## Build APK using only an Android phone (GitHub Actions)
1. In your phone browser, sign in to GitHub and create a **private repository** named `MultiAI`.
2. Upload the project files and folders from this ZIP into the repository's `main` branch. Keep the folder structure exactly as shown; upload the *contents* of this project, not the outer ZIP file.
3. Open the repository's **Actions** tab and select **Build Multi AI APK**.
4. Tap **Run workflow** (or wait for the workflow to run after a push).
5. Open the completed workflow run, scroll to **Artifacts**, and download `MultiAI-debug-apk`.
6. Extract the downloaded artifact ZIP. Install `app-debug.apk` on your Galaxy A17. Android may ask you to allow installation from that browser/files app. Only install your own build.

## Build requirements
- Java 17
- Android SDK Platform 35 / Build Tools 35
- Android Gradle Plugin 8.9.2
- Kotlin plugin 2.1.10

## Online AI (free-tier approach)
This prototype includes a simple online AI prompt dialog and lets the user enter a Gemini API key in the app. Free-tier availability and quotas vary by provider and can change. Do not commit API keys into source control or hard-code them in the APK. For a public release, route API calls through a small backend so the key is not extractable from the APK. Without Internet, online AI responses will not work; local phone commands remain available. The API key is stored in local app preferences for this prototype; it is not suitable for a publicly distributed production app.

## Suggested roadmap
1. Add online AI chat and a provider/model selector.
2. Add secure key handling (prefer backend for public distribution).
3. Add foreground wake-word mode with persistent notification and battery controls.
4. Add user-approved notification access for reading supported notifications aloud.
5. Add routine builder and more Android-supported device actions.
6. Test each feature on the target phone and sign release builds.

## License
MIT — see `LICENSE`.
