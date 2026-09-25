# Spell It! — Android app

A native Android wrapper around a trio of word games for young kids. For Spell It! and
Pick It!, a picture is shown and the word is spoken aloud; Add It! reads a sentence aloud
instead. Then:

- **Spell It!** — tap letter tiles in order to spell the word.
- **Pick It!** — pick the matching word out of 5 options.
- **Add It!** — listen to a sentence with a missing word, then pick the missing word out
  of 5 options.

All three games share the same three difficulty levels (Easy/Medium/Hard) and word banks
(Add It!'s sentences reuse the same words), with their own star progress tracked
separately per game.

## How it's built

- `app/src/main/assets/www/index.html` — all three games (HTML/CSS/JS). A games screen
  picks Spell It!, Pick It!, or Add It!, a levels screen picks the difficulty, then the
  same play screen renders whichever mode is active.
- `MainActivity.kt` — loads that page in a full-screen `WebView` and injects a small
  JavaScript bridge (`window.AndroidTTS`) backed by Android's native `TextToSpeech` engine.
  Android's WebView doesn't implement the browser's Web Speech API, so the page calls this
  native bridge when running inside the app, and falls back to `speechSynthesis` only when
  opened directly in a browser (so the same HTML file still works as a web page too).

## Getting the APK

Every push to `main` triggers `.github/workflows/android-build.yml`, which compiles a debug
APK on GitHub's runners (this repo's own machine can't reach the Android SDK, so the actual
build happens in CI, not locally) and attaches it to a new GitHub Release. Grab
`app-debug.apk` from the repo's **Releases** page, or from the workflow run's **Artifacts**
under the **Actions** tab.

This is a debug build (signed with Android's auto-generated debug key), which installs fine
for personal use and testing. Publishing to the Play Store would need a proper release
signing key instead — ask if you'd like that set up.

## Installing on a phone

1. Download `app-debug.apk` from the Release/Artifact onto the phone.
2. Open it; Android will prompt to allow installing from this source if it's the first time.
3. Install and open — no internet connection required to play (it only reaches out to load
   the Google Fonts webfonts, and falls back to the system font if offline).

## Building locally instead

Open this folder in Android Studio (Giraffe or newer) and click Run, or from a terminal with
the Android SDK installed:

```
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.
