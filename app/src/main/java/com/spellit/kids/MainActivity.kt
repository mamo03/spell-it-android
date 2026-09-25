package com.spellit.kids

import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.util.Locale

/**
 * Hosts the Spell It! web app (app/src/main/assets/www/index.html) inside a full-screen
 * WebView, and bridges its speak() calls to Android's native TextToSpeech engine.
 *
 * Android's WebView does not implement the Web Speech Synthesis API used by desktop
 * browsers (window.speechSynthesis returns no voices), so the page's JavaScript calls
 * window.AndroidTTS.speak(text, queue, lang) when this bridge is present, and falls back
 * to speechSynthesis only when it isn't (e.g. when the same HTML is opened in a browser).
 * lang is a BCP-47 tag ("en-US" or "da-DK") matching the page's language selector.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var currentTtsLocale: Locale = Locale.US

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = currentTtsLocale
                tts.setPitch(1.1f)
                tts.setSpeechRate(0.9f)
                ttsReady = true
            }
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true

            // The app's palette is one deliberate bright design, not a light/dark pair.
            // Some Android versions and manufacturers (Samsung's One UI in particular)
            // otherwise auto-invert or re-tint page colors when the device is in system
            // dark mode, which fights with our own CSS and can make text unreadable.
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
            }

            webViewClient = WebViewClient()
            addJavascriptInterface(TtsBridge(), "AndroidTTS")
            loadUrl("file:///android_asset/www/index.html")
        }

        setContentView(webView)
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /** Exposed to the page's JavaScript as window.AndroidTTS. */
    inner class TtsBridge {
        /**
         * queue=false interrupts whatever is currently speaking (a new word, "hear it
         * again", or a freshly tapped letter). queue=true appends after it instead, used
         * for the word-complete message so it doesn't cut off the last letter's own
         * announcement, which is spoken an instant earlier by the same tap.
         *
         * lang is a BCP-47 tag ("en-US" or "da-DK"). The engine's voice is switched to
         * match it before speaking, and only re-set when it actually changes, so rapid
         * taps in the same language don't repeatedly touch the TTS engine's language.
         * If the requested language's voice data isn't installed on the device, we still
         * attempt to speak in the best voice the engine falls back to, rather than
         * silently dropping the word.
         */
        @JavascriptInterface
        fun speak(text: String, queue: Boolean, lang: String) {
            if (!ttsReady) return
            runOnUiThread {
                val locale = try {
                    Locale.forLanguageTag(lang)
                } catch (e: Exception) {
                    Locale.US
                }
                if (locale != currentTtsLocale) {
                    val result = tts.setLanguage(locale)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        currentTtsLocale = locale
                    }
                    // On missing/unsupported data we deliberately leave the engine on its
                    // current language rather than force an unsupported one, so speech
                    // keeps working (just not in the requested language) instead of going
                    // silent or throwing.
                }
                val mode = if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
                tts.speak(text, mode, null, "spellItUtterance")
            }
        }
    }
}
