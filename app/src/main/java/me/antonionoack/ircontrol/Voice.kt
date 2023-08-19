package me.antonionoack.ircontrol

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import androidx.core.app.ActivityCompat
import me.antonionoack.ircontrol.ir.CommandLogic.runOnce
import me.antonionoack.ircontrol.ir.CommandLogic.runRepeatedly
import me.antonionoack.ircontrol.ir.CommandLogic.stopRunning
import kotlin.concurrent.thread
import kotlin.math.abs

object Voice {

    const val ASR_PERMISSION_REQUEST_CODE = 1700

    private var isListening = false
    private var shallListen = false

    fun MainActivity.setupVoiceButton() {

        createSpeechRecognizer1()

        findViewById<Button>(R.id.voiceTest).apply {
            voiceTestView = this
            setOnClickListener {
                if (isListening) {
                    shallListen = false
                    handleSpeechEnd("click")
                } else {
                    verifyAudioPermissions()
                }
            }
        }
    }

    fun MainActivity.handleSpeechBegin(reason: String) {
        if (isListening) return
        if (!shallListen) return
        println("start ($reason)")
        // start audio session
        isListening = true
        speechRecognizer?.startListening(createIntent())
    }

    fun MainActivity.handleSpeechEnd(reason: String, restart: Boolean = true) {
        if (!isListening) return
        println("stop ($reason)")
        // end audio session
        isListening = false
        voiceTestView?.text = getString(R.string.enableVoiceRecognition)
        speechRecognizer?.cancel()
        if (restart) thread {
            Thread.sleep(100)
            runOnUiThread {
                handleSpeechBegin(reason)
            }
        }
    }

    private fun createIntent(): Intent {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
        // i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 50)
        return i
    }

    private var lastStop = 0L
    private var lastStart = 0L
    private var lastRun = 0L
    fun MainActivity.parseCommands(commands: List<String>) {
        println(commands)
        val time = System.nanoTime()
        if (commands.any { it.contains("stop", true) || it.contains("halt", true) }) {
            if (abs(time - lastStop) > 1e8) {
                stopRunning()
                toast("Stopped", false)
            }
            lastStop = time
        } else if (commands.any { it.contains("start", true) }) {
            if (abs(time - lastStart) > 1e8) {
                runRepeatedly()
                toast("Started", false)
            }
            lastStart = time
        } else if (commands.any { it.contains("einmal", true) }) {
            if (abs(time - lastRun) > 1e8) {
                runOnce()
                toast("Started", false)
            }
            lastRun = time
        }
    }

    private fun MainActivity.createSpeechRecognizer1() {
        println("Voice created")
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle) {
                println("Voice - ready")
                voiceTestView?.text = getString(R.string.disableVoiceRecognition)
            }

            override fun onBeginningOfSpeech() {
                println("Voice - begin")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // println("- rms $rmsdB")
            }

            override fun onBufferReceived(buffer: ByteArray) {
                println("Voice - buffer")
            }

            override fun onEndOfSpeech() {
                println("Voice - end of speech")
                handleSpeechEnd("endOfSpeech")
            }

            @SuppressLint("SetTextI18n")
            override fun onError(error: Int) {
                val name = when (error) {
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "network timeout"
                    SpeechRecognizer.ERROR_NETWORK -> "network"
                    SpeechRecognizer.ERROR_AUDIO -> "audio"
                    SpeechRecognizer.ERROR_SERVER -> "server"
                    SpeechRecognizer.ERROR_CLIENT -> "client"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "speech timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "no match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        println("Voice recognizer busy")
                        voiceTestView?.text = "Internal Error (Recognizer busy)"
                        thread {
                            Thread.sleep(500)
                            runOnUiThread {
                                handleSpeechEnd("error")
                            }
                        }
                        return
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "permissions missing"
                    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "too many requests"
                    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "server disconnected"
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "language not supported"
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "language unavailable"
                    else -> error.toString()
                }
                /*Toast.makeText(
                    this@MainActivity,
                    "Error: $name", Toast.LENGTH_LONG
                ).show()*/
                println(name)
                handleSpeechEnd("error")
            }

            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    parseCommands(matches)
                } else println("Voice null results")
            }

            override fun onPartialResults(results: Bundle) {
                // Called when partial recognition results are available, this callback will be
                // called each time a partial text result is ready while the user is speaking.
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    parseCommands(matches)
                } else println("Voice null partial results")
            }

            override fun onEvent(eventType: Int, params: Bundle) {
                println("Voice - event $eventType")
            }
        })
    }

    private fun MainActivity.verifyAudioPermissions() {
        shallListen = true
        if (checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Voice Recognition Permission")
            dialog.setMessage(
                "Voice Recognition uses your microphone to pick up commands like 'stop' and 'start'.\n" +
                        "The voice recognition typically uses Google, and may occur on out-of-state servers.\n" +
                        "Click 'OK' to grant the permission in the next step."
            )
            dialog.setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    ASR_PERMISSION_REQUEST_CODE
                )
            }
            dialog.setNegativeButton("Cancel") { _, _ ->
                // idc
            }
            dialog.show()
        } else {
            handleSpeechBegin("click")
        }
    }

}