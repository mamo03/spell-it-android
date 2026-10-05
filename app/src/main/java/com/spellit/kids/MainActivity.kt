package com.spellit.kids

import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.util.Locale

/**
 * Hosts the Spell It! web app (app/src/main/assets/www/index.html) inside a full-screen
 * WebView, and bridges its speak() calls to Android's native TextToSpeech engine.
 *
 * Android's WebView does not implement the Web Speech Synthesis API used by desktop
 * browsers (window.speechSynthesis returns no voices), so the page's JavaScript calls
 * window.AndroidTTS.speak(text, queue, lang, slow, highlight) when this bridge is
 * present, and falls back to speechSynthesis only when it isn't (e.g. when the same
 * HTML is opened in a browser). lang is a BCP-47 tag ("en-US" or "da-DK") matching the
 * page's language selector.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var currentTtsLocale: Locale = Locale.US
    private var currentTtsRate: Float = NORMAL_RATE

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = currentTtsLocale
                tts.setPitch(1.1f)
                tts.setSpeechRate(currentTtsRate)
                // Forwards word-by-word progress back to the page while it's speaking
                // Add It!'s sentence, so it can highlight the word currently being
                // read (see index.html's window.__ttsRange / highlightWordAtCharIndex).
                // Only invoked by engines that support ranged progress (Android O+ and
                // engine-dependent) — on any other engine/device this callback simply
                // never fires and the sentence just doesn't highlight, same as any
                // other best-effort TTS feature in this app.
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {}

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) {}

                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        if (utteranceId != HIGHLIGHT_UTTERANCE_ID) return
                        runOnUiThread {
                            webView.evaluateJavascript(
                                "window.__ttsRange && window.__ttsRange($start)",
                                null
                            )
                        }
                    }
                })
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

        // Targeting Android 15+ forces edge-to-edge: the window draws behind the status and
        // navigation bars. Pad the WebView by the system-bar/cutout insets so the games never
        // sit under them, and keep the status-bar icons light against the sky-blue page.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(webView) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setContentView(webView)

        // Replaces the deprecated onBackPressed() override, which is no longer called when
        // targeting API 36 (predictive back).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
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
         *
         * slow=true reads at SLOW_RATE instead of NORMAL_RATE — used for Add It!/Build
         * It!'s full sentences, which are easier to follow a little slower than single
         * words and letters. highlight=true (Add It! only) tags the utterance so
         * onRangeStart forwards word-boundary progress back to the page for its
         * word-by-word highlight; other calls are untagged and never trigger it.
         */
        @JavascriptInterface
        fun speak(text: String, queue: Boolean, lang: String, slow: Boolean, highlight: Boolean) {
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
                val rate = if (slow) SLOW_RATE else NORMAL_RATE
                if (rate != currentTtsRate) {
                    tts.setSpeechRate(rate)
                    currentTtsRate = rate
                }
                val mode = if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
                val utteranceId = if (highlight) HIGHLIGHT_UTTERANCE_ID else "spellItUtterance"
                tts.speak(text, mode, null, utteranceId)
            }
        }
    }

    companion object {
        private const val NORMAL_RATE = 0.9f
        private const val SLOW_RATE = 0.68f
        private const val HIGHLIGHT_UTTERANCE_ID = "spellItSentenceHighlight"
    }
}
