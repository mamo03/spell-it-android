# Spell It! — Android app

A native Android wrapper around six word games for young kids, playable in **English,
Danish, French, German, Spanish, Italian, Polish, or Arabic** via a language switch on the
games screen. For Spell It!, Pick It!,
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
- **Randomise It!** — mixes in a random pick from all five games above, round after
  round, so the mode keeps changing while you play.

All six games share the same three difficulty levels (Easy/Medium/Hard) and word banks
(Add It!'s and Build It!'s sentences reuse the same words). Build It! doesn't need its own
sentence bank — it takes each level's existing Add It! sentence, fills in the blank, and
splits it into the words the player taps back into order, so puzzle length naturally grows
from Easy to Hard along with the sentences themselves. Randomise It! doesn't need its own
content either — each round it draws a game and an item at random from a deck built out of
the other five games' own words and sentences for that level. Levels are endless — the deck
reshuffles and keeps going until the player backs out, rather than ending after a fixed
round.

## Language support

The games screen has an 8-language switch, shown as a dropdown (🇬🇧 English / 🇩🇰 Dansk /
🇫🇷 Français / 🇩🇪 Deutsch / 🇪🇸 Español / 🇮🇹 Italiano / 🇵🇱 Polski / 🇸🇦 العربية). The
choice is remembered between visits (`localStorage`) and applies to every game's UI text,
word banks, Add It! sentences, and spoken audio. Every non-English language has its own
independently curated word bank per difficulty level — bucketed by actual word length in
that language, not translated one-to-one from English — since word lengths differ between
languages. Current word bank sizes (easy/medium/hard): English 250/250/220, Danish
174/192/180, French 148/183/172, German 147/190/190, Spanish 151/217/230, Italian
120/188/173, Polish 134/233/205, Arabic 194/200/92 (undiacritized Modern Standard Arabic —
Arabic's root-and-pattern morphology means simple concrete nouns cluster at 3-6 letters, so
the 7+-letter "hard" bucket is real but genuinely smaller than the other languages'). Every
word bank and its matching Add It! sentence bank has been through a dedicated
spelling/grammar audit per language, on top of the size increase. The language switch only
appears on the games screen, so there's no need to change language mid-round.

Arabic also switches the whole page to right-to-left: the `<html>` element's `dir`
attribute flips between `"ltr"` and `"rtl"` together with `lang`, which — since the layout
is plain flexbox/grid with no hardcoded left/right positioning — automatically mirrors the
games list, level list, letter tiles, and slots without any script changes. Arabic renders
in **Baloo Bhaijaan 2** (the official Arabic-script sibling of the Baloo 2 font used
everywhere else) so headings and buttons keep the same rounded, playful look across every
language.

## How it's built

- `app/src/main/assets/www/index.html` — all six games (HTML/CSS/JS). A games screen
  picks Spell It!, Pick It!, Add It!, See It!, Build It!, or Randomise It! and the
  language, a levels screen picks the difficulty, then the same play screen renders
  whichever mode is active.
  All UI copy, word/sentence banks, and game names are organized per-language
  (`STRINGS`, `GAMES_BY_LANG`, `LEVELS_BY_LANG`) and looked up by the currently selected
  language throughout.
- `MainActivity.kt` — loads that page in a full-screen `WebView` and injects a small
  JavaScript bridge (`window.AndroidTTS`) backed by Android's native `TextToSpeech` engine.
  Android's WebView doesn't implement the browser's Web Speech API, so the page calls this
  native bridge when running inside the app, and falls back to `speechSynthesis` only when
  opened directly in a browser (so the same HTML file still works as a web page too). The
  bridge's `speak(text, queue, lang)` takes a BCP-47 language tag (e.g. "en-US"/"da-DK"/
  "fr-FR"/"de-DE"/"es-ES"/"it-IT"/"pl-PL"/"ar-SA") from the page and switches the native TTS
  engine's voice to match, falling back gracefully if a language's voice data isn't
  installed on the device — adding another language is a page-side change only, since the
  bridge just forwards whatever tag it's given.

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
