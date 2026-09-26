# Spell It! — Android app

A native Android wrapper around five word games for young kids, playable in **English,
Danish, or French** via a language switch on the games screen. For Spell It!, Pick It!,
and See It!, a word is spoken aloud (See It! shows the word as text instead of a picture);
Add It! and Build It! read a full sentence aloud instead. Then:

- **Spell It!** — tap letter tiles in order to spell the word.
- **Pick It!** — pick the matching word out of 6 options.
- **Add It!** — listen to a sentence with a missing word, then pick the missing word out
  of 6 options.
- **See It!** — read (and hear) the word, then pick the matching picture out of 4
  options.
- **Build It!** — listen to a full sentence, then tap its words back into the correct
  order.

All five games share the same three difficulty levels (Easy/Medium/Hard) and word banks
(Add It!'s and Build It!'s sentences reuse the same words). Build It! doesn't need its own
sentence bank — it takes each level's existing Add It! sentence, fills in the blank, and
splits it into the words the player taps back into order, so puzzle length naturally grows
from Easy to Hard along with the sentences themselves. Levels are endless — the deck
reshuffles and keeps going until the player backs out, rather than ending after a fixed
round.

## Language support

The games screen has an English/Danish/French switch, shown as a dropdown (🇬🇧 English /
🇩🇰 Dansk / 🇫🇷 Français). The choice is remembered between visits (`localStorage`) and
applies to every game's UI text, word banks, Add It! sentences, and spoken audio. Danish
and French each have their own independently curated word bank per difficulty level —
bucketed by actual word length in that language, not translated one-to-one from English —
since word lengths differ between languages. Current word bank sizes (easy/medium/hard):
English 198/200/170, Danish 121/147/135, French 101/134/121. The language switch only
appears on the games screen, so there's no need to change language mid-round.

## How it's built

- `app/src/main/assets/www/index.html` — all five games (HTML/CSS/JS). A games screen
  picks Spell It!, Pick It!, Add It!, See It!, or Build It! and the language, a levels
  screen picks the difficulty, then the same play screen renders whichever mode is active.
  All UI copy, word/sentence banks, and game names are organized per-language
  (`STRINGS`, `GAMES_BY_LANG`, `LEVELS_BY_LANG`) and looked up by the currently selected
  language throughout.
- `MainActivity.kt` — loads that page in a full-screen `WebView` and injects a small
  JavaScript bridge (`window.AndroidTTS`) backed by Android's native `TextToSpeech` engine.
  Android's WebView doesn't implement the browser's Web Speech API, so the page calls this
  native bridge when running inside the app, and falls back to `speechSynthesis` only when
  opened directly in a browser (so the same HTML file still works as a web page too). The
  bridge's `speak(text, queue, lang)` takes a BCP-47 language tag ("en-US"/"da-DK"/"fr-FR")
  from the page and switches the native TTS engine's voice to match, falling back gracefully
  if a language's voice data isn't installed on the device — adding another language is a
  page-side change only, since the bridge just forwards whatever tag it's given.

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
