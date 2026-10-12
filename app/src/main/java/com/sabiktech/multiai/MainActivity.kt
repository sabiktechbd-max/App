package com.sabiktech.multiai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var heard: TextView
    private lateinit var answer: TextView
    private lateinit var tts: TextToSpeech
    private var torchOn = false
    private val speechRequest = 101
    private val audioPermissionRequest = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        status.text = "Ready • Tap the microphone to speak"
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 54, 36, 36)
            setBackgroundColor(0xFF090D18.toInt())
        }
        val title = TextView(this).apply {
            text = "✦  MULTI AI"
            textSize = 28f
            setTextColor(0xFFEAF2FF.toInt())
        }
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF9CB7E8.toInt())
            setPadding(0, 12, 0, 28)
        }
        val orb = TextView(this).apply {
            text = "◉"
            textSize = 76f
            gravity = 17
            setTextColor(0xFF79A9FF.toInt())
            setPadding(0, 18, 0, 18)
        }
        heard = TextView(this).apply {
            text = "You said: —"
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 12, 0, 12)
        }
        answer = TextView(this).apply {
            text = "Multi AI: Hello! How can I help?"
            textSize = 17f
            setTextColor(0xFFD3DEEF.toInt())
            setPadding(0, 8, 0, 24)
        }
        page.addView(title)
        page.addView(status)
        page.addView(orb)
        page.addView(heard)
        page.addView(answer)
        page.addView(makeButton("🎙  Speak to Multi AI") { startListening() })
        page.addView(makeButton("✨  Ask Online AI") { askOnlineAi() })
        page.addView(makeButton("🔑  Set / change AI API key") { setApiKey() })
        page.addView(makeButton("🔦  Flashlight on/off") { toggleFlashlight() })
        page.addView(makeButton("⚙  Open phone settings") {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        })
        page.addView(makeButton("🔊  Volume up") {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            respond("Media volume increased.")
        })
        page.addView(makeButton("ℹ  What can I do?") {
            respond("You can ask me to open settings, turn the flashlight on or off, or increase media volume. More AI features can be added in the next version.")
        })
        setContentView(page)
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setOnClickListener { action() }
        }
    }


    private fun setApiKey() {
        val field = EditText(this).apply {
            hint = "Paste your Gemini API key"
            setSingleLine(true)
            setText(getSharedPreferences("multiai", MODE_PRIVATE).getString("gemini_key", ""))
        }
        AlertDialog.Builder(this)
            .setTitle("Online AI API key")
            .setMessage("Create a key in Google AI Studio. Keep it private; this prototype stores it on this phone and does not hide it from someone who can inspect the app.")
            .setView(field)
            .setPositiveButton("Save") { _, _ ->
                val key = field.text.toString().trim()
                if (key.isNotEmpty()) {
                    getSharedPreferences("multiai", MODE_PRIVATE).edit().putString("gemini_key", key).apply()
                    respond("API key saved on this phone.")
                } else {
                    respond("No API key was saved.")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun askOnlineAi() {
        val key = getSharedPreferences("multiai", MODE_PRIVATE).getString("gemini_key", null)
        if (key.isNullOrBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Set up Online AI")
                .setMessage("First add a Gemini API key. Free-tier access and quotas depend on Google's current availability and terms.")
                .setPositiveButton("Set API key") { _, _ -> setApiKey() }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        val field = EditText(this).apply {
            hint = "Ask in Bangla or English"
            minLines = 2
        }
        AlertDialog.Builder(this)
            .setTitle("Ask Multi AI")
            .setView(field)
            .setPositiveButton("Ask") { _, _ ->
                val prompt = field.text.toString().trim()
                if (prompt.isNotEmpty()) sendToGemini(key, prompt)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendToGemini(key: String, prompt: String) {
        status.text = "Online AI is thinking…"
        answer.text = "Multi AI: Connecting to online AI…"
        Executors.newSingleThreadExecutor().execute {
            var connection: HttpURLConnection? = null
            try {
                val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + java.net.URLEncoder.encode(key, "UTF-8"))
                connection = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                val part = JSONObject().put("text", prompt)
                val parts = JSONArray().put(part)
                val content = JSONObject().put("parts", parts)
                val body = JSONObject().put("contents", JSONArray().put(content))
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (code !in 200..299) {
                    throw IllegalStateException("Online AI returned HTTP $code. Check API key, quota, and model access.")
                }
                val json = JSONObject(responseText)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0).getString("text")
                runOnUiThread { respond(text) }
            } catch (e: Exception) {
                runOnUiThread {
                    respond("Online AI could not respond. Check internet, API key, quota, and try again. Details: ${e.message ?: "unknown error"}")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), audioPermissionRequest)
            return
        }
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            respond("Speech recognition is not available. Check your Google app and language settings.")
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Multi AI")
        }
        try {
            startActivityForResult(intent, speechRequest)
        } catch (_: Exception) {
            respond("Could not start speech recognition. Try again.")
        }
    }

    @Deprecated("Deprecated by Android, retained for broad compatibility in this starter")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequest && resultCode == RESULT_OK) {
            val phrase = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (phrase.isNotBlank()) {
                heard.text = "You said: $phrase"
                handleCommand(phrase.lowercase(Locale.ROOT))
            }
        }
    }

    private fun handleCommand(command: String) {
        when {
            command.contains("flashlight") || command.contains("ফ্ল্যাশ") || command.contains("টর্চ") -> toggleFlashlight()
            command.contains("settings") || command.contains("সেটিং") -> {
                respond("Opening phone settings.")
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            command.contains("volume") || command.contains("ভলিউম") || command.contains("শব্দ বাড়াও") -> {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                respond("Media volume increased.")
            }
            command.contains("hello") || command.contains("হ্যালো") || command.contains("হাই") -> respond("Hello! আমি Multi AI. কীভাবে সাহায্য করতে পারি?")
            command.contains("time") || command.contains("সময়") -> {
                val time = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())
                respond("এখন সময় $time")
            }
            else -> respond("আমি শুনেছি: $command । এই কমান্ডটি এখনো শেখানো হয়নি। পরের সংস্করণে আরও কমান্ড ও AI যুক্ত করা যাবে।")
        }
    }

    private fun toggleFlashlight() {
        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                respond("This phone does not expose a flashlight to the app.")
                return
            }
            torchOn = !torchOn
            manager.setTorchMode(cameraId, torchOn)
            respond(if (torchOn) "Flashlight on." else "Flashlight off.")
        } catch (e: Exception) {
            respond("I could not change the flashlight. Check permissions and try again.")
        }
    }

    private fun respond(message: String) {
        answer.text = "Multi AI: $message"
        status.text = "Ready"
        if (::tts.isInitialized) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "multiai-response")
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("bn", "BD"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == audioPermissionRequest && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else if (requestCode == audioPermissionRequest) {
            Toast.makeText(this, "Microphone permission is needed for voice commands.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
