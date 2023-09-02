package me.antonionoack.ircontrol

import android.content.SharedPreferences
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import me.antonionoack.ircontrol.Projects.setupProjectList
import me.antonionoack.ircontrol.Voice.ASR_PERMISSION_REQUEST_CODE
import me.antonionoack.ircontrol.Voice.handleSpeechBegin
import me.antonionoack.ircontrol.Voice.handleSpeechEnd
import me.antonionoack.ircontrol.Voice.setupVoiceButton
import me.antonionoack.ircontrol.camera.CameraSensor.CAMERA_PERMISSIONS_ID
import me.antonionoack.ircontrol.camera.CameraSensor.allPermissionsGranted
import me.antonionoack.ircontrol.camera.CameraSensor.startCamera
import me.antonionoack.ircontrol.ir.CommandLogic.addAddListeners
import me.antonionoack.ircontrol.ir.CommandLogic.loadCurrentProject
import me.antonionoack.ircontrol.ir.CommandLogic.loadProjects
import me.antonionoack.ircontrol.ir.CommandLogic.runOnce
import me.antonionoack.ircontrol.ir.CommandLogic.runRepeatedly
import me.antonionoack.ircontrol.ir.CommandLogic.setupSeekBar
import me.antonionoack.ircontrol.ir.CommandLogic.startMotorController
import me.antonionoack.ircontrol.ir.CommandLogic.stopMotorController
import me.antonionoack.ircontrol.ir.CommandLogic.stopRunning
import me.antonionoack.ircontrol.ir.Motor
import me.antonionoack.ircontrol.ir.commands.WaitForColor
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    lateinit var sequenceView: LinearLayout
    lateinit var preferences: SharedPreferences
    lateinit var flipper: ViewFlipper
    var speechRecognizer: SpeechRecognizer? = null
    var voiceTestView: TextView? = null

    var waitForColorCommand: List<WaitForColor> = emptyList()
    var waitForColorCallback: ((WaitForColor?) -> Boolean)? = null
    var runCameraInBackground = false
    var hasBackgroundCamera = false

    var unbindCamera: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all)

        Thread.sleep(1000)

        preferences = getPreferences(MODE_PRIVATE)
        sequenceView = findViewById(R.id.sequence)
        flipper = findViewById(R.id.flipper)

        findViewById<View>(R.id.delete).visibility = View.GONE

        addAddListeners(findViewById<View>(android.R.id.content).rootView, null)

        findViewById<View>(R.id.run).setOnClickListener { runOnce() }
        findViewById<View>(R.id.repeat).setOnClickListener { runRepeatedly() }
        findViewById<View>(R.id.stopSequence).setOnClickListener { stopRunning() }

        setupSeekBar(R.id.sliderR1, Motor.R1)
        setupSeekBar(R.id.sliderR2, Motor.R2)
        setupSeekBar(R.id.sliderR3, Motor.R3)
        setupSeekBar(R.id.sliderR4, Motor.R4)

        setupSeekBar(R.id.sliderB1, Motor.B1)
        setupSeekBar(R.id.sliderB2, Motor.B2)
        setupSeekBar(R.id.sliderB3, Motor.B3)
        setupSeekBar(R.id.sliderB4, Motor.B4)

        loadProjects()
        loadCurrentProject()
        startMotorController()

        setupVoiceButton()
        setupProjectList()

        findViewById<View>(R.id.manualControl).setOnClickListener {
            flipper.displayedChild = 1
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ASR_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    // audio permission granted
                    toast("You can now use 'stop' as a voice command!", true)
                    handleSpeechBegin("perms granted")
                } else {
                    // audio permission denied
                    toast("'Stop' voice command will be unavailable.", false)
                    // handleSpeechEnd(false)
                }
            }

            CAMERA_PERMISSIONS_ID -> {
                if (allPermissionsGranted()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val cmd = waitForColorCommand
                        if (cmd.isNotEmpty()) startCamera(cmd)
                    } else toast("Android API too old :/", true)
                } else if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    toast("Granted but missing camera permissions", true)
                } else toast("Missing camera permissions", true)
            }
        }
    }

    private var lastToast = ""
    private var lastToastTime = 0L
    fun toast(text: String, long: Boolean) {
        if (text == lastToast && abs(lastToastTime - System.nanoTime()) < 5e9) {
            return
        }
        lastToast = text
        lastToastTime = System.nanoTime()
        Toast.makeText(
            this, text, if (long) Toast.LENGTH_LONG
            else Toast.LENGTH_SHORT
        ).show()
    }

    override fun onPause() {
        super.onPause()
        stopMotorController()
        handleSpeechEnd("pause", false)
    }

    override fun onResume() {
        super.onResume()
        startMotorController()
        // handleSpeechBegin("resume")
    }

    override fun onStop() {
        super.onStop()
        stopMotorController()
        handleSpeechEnd("stop", false)
    }

    override fun onStart() {
        super.onStart()
        startMotorController()
        // handleSpeechBegin("start")
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (flipper.displayedChild > 0) {
            flipper.displayedChild = 0
        } else super.onBackPressed()
    }

}