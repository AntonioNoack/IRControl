package me.antonionoack.ircontrol.ir

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AlertDialog.Builder
import android.app.Dialog
import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.math.MathUtils
import me.antonionoack.ircontrol.MainActivity
import me.antonionoack.ircontrol.Projects.projectName
import me.antonionoack.ircontrol.Projects.projectNames
import me.antonionoack.ircontrol.R
import me.antonionoack.ircontrol.camera.CameraSensor
import me.antonionoack.ircontrol.camera.CameraSensor.black
import me.antonionoack.ircontrol.camera.CameraSensor.tryStartCamera
import kotlin.concurrent.thread

object CommandLogic {

    const val FLOAT = 0
    const val BREAK_THEN_FLOAT = -8

    private var runId = 0
    val sequence = ArrayList<Command>()

    private var controller: LegoIRController? = null

    private val speedNames =
        "-100%,-86%,-71%,-57%,-43%,-29%,14%,stop,14%,29%,43%,57%,71%,86%,100%".split(',')

    fun MainActivity.save() {
        save(projectName)
    }

    fun MainActivity.save(projectName: String) {
        val v = sequence.filter { it.isManual }.joinToString("|")
        preferences.edit()
            .putString("sequence:$projectName", v)
            .putString("projects", projectNames.joinToString("\n"))
            .putString("project", projectName)
            .apply()
    }

    fun MainActivity.loadProjects() {
        projectName = preferences.getString("project", "") ?: ""
        projectNames.clear()
        projectNames.addAll((preferences.getString("projects", "") ?: "")
            .split("\n")
            .filter { it.isNotEmpty() })
    }

    fun MainActivity.loadCurrentProject() {
        val key = if (projectName.isEmpty()) "sequence" else "sequence:$projectName"
        val data = preferences.getString(key, "") ?: ""
        sequence.clear()
        sequenceView.removeAllViews()
        val (cmd, views) = loadProject(data)
        sequence.addAll(cmd)
        for (v in views) sequenceView.addView(v)
        if (projectName.isEmpty() && sequence.isNotEmpty()) {
            projectName = "Project"
            projectNames.add(projectName)
            preferences.edit().remove(key).apply()
            save()
        }
    }

    fun MainActivity.loadProject(data: String): Pair<List<Command>, List<View>> {
        val commands = data.split('|')
        val sequence = ArrayList<Command>(commands.size)
        val views = ArrayList<View>(commands.size)
        try {
            for (cmd in commands) {
                if (cmd.isNotEmpty()) {
                    val n: Command
                    val v: View
                    when (cmd[0]) {
                        'm' -> {
                            val id = cmd[1].code - '0'.code
                            val speed = cmd.substring(2, 3).toIntOrNull(16) ?: continue
                            n = MotorSpeed(
                                id.and(1) == 0,
                                id.shr(1) and 3,
                                MathUtils.clamp(speed - 7, -7, +7)
                            )
                            v = motorSpeed(n)
                        }

                        's' -> {
                            n = Sleep(cmd.substring(1).toFloatOrNull() ?: continue)
                            v = sleep(n)
                        }

                        'r' -> {
                            val names = cmd.substring(1).split(';')
                            n = RandomCall(names)
                            v = randomCall(n)
                        }

                        'c' -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                val vs = cmd.substring(1).split(';')
                                n = WaitForColor(
                                    vs[0].toFloat(),
                                    vs[1].toFloat(),
                                    vs[2].toInt(16),
                                    vs[3].toFloat()
                                )
                                v = waitForColor(n)
                            } else continue
                        }

                        'x' -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                val vs = cmd.substring(1).split(';')
                                val names = vs.subList(5, vs.size)
                                var elseIdx = names.indexOf("")
                                if (elseIdx < 0) elseIdx = names.size
                                n = ExecIfColor(
                                    vs[0].toFloat(),
                                    vs[1].toFloat(),
                                    vs[2].toInt(16),
                                    vs[3].toFloat(),
                                    vs[4].toFloat(),
                                    names.subList(0, elseIdx).filter { it.isNotBlank() },
                                    names.subList(elseIdx, names.size).filter { it.isNotBlank() }
                                )
                                v = execIfColor(n)
                            } else continue
                        }

                        else -> continue
                    }
                    sequence.add(n)
                    views.add(v)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(sequence, views)
    }

    fun MainActivity.addAddListeners(v: View, n: Any?) {
        v.findViewById<TextView>(R.id.addMotor).apply {
            setOnClickListener {
                addMotor(n)
            }
            setOnLongClickListener {
                toast("Add a motor modifier", false)
                true
            }
        }
        v.findViewById<TextView>(R.id.addTimer).apply {
            setOnClickListener {
                addTimer(n)
            }
            setOnLongClickListener {
                toast("Add a time delay", false)
                true
            }
        }
        v.findViewById<TextView>(R.id.addRandomCall).apply {
            setOnClickListener {
                addRandomCall(n)
            }
            setOnLongClickListener {
                toast("Add random call list", false)
                true
            }
        }
        v.findViewById<TextView>(R.id.addWaitForColor).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setOnClickListener {
                    addWaitForColor(n)
                }
                setOnLongClickListener {
                    toast("Add wait-for-color", false)
                    true
                }
            } else visibility = View.GONE
        }
        v.findViewById<TextView>(R.id.addExecIfColor).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setOnClickListener {
                    addExecIfColor(n)
                }
                setOnLongClickListener {
                    toast("Add exec-if-color", false)
                    true
                }
            } else visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    fun MainActivity.motorSpeed(n: MotorSpeed): View {
        @SuppressLint("InflateParams")
        val v = layoutInflater.inflate(R.layout.set_motor, null)
        v.findViewById<TextView>(R.id.channelId)
            .apply {
                text = (n.id + 1).toString()
                setOnClickListener {
                    n.id = (n.id + 1) and 3
                    (it as TextView).text = (n.id + 1).toString()
                    save()
                }
            }
        v.findViewById<TextView>(R.id.colorId)
            .apply {
                text = if (n.red) "R" else "B"
                setBackgroundColor((if (n.red) 0xff5555 else 0x5555ff) or (255 shl 24))
                setOnClickListener {
                    n.red = !n.red
                    (it as TextView).text = if (n.red) "R" else "B"
                    it.setBackgroundColor((if (n.red) 0xff5555 else 0x5555ff) or (255 shl 24))
                    save()
                }
            }
        v.findViewById<TextView>(R.id.speed)
            .apply {
                text = speedNames[n.speed + 7]
                setOnClickListener {
                    var dialog: AlertDialog? = null
                    val builder = Builder(this@motorSpeed)
                    val w = layoutInflater.inflate(R.layout.dialog_speed, null)
                    fun show(id: Int, speed: Int) {
                        val b = w.findViewById<Button>(id)
                        b.text = speedNames[speed]
                        b.setOnClickListener {
                            n.speed = speed - 7
                            text = speedNames[speed]
                            dialog?.dismiss()
                        }
                    }
                    show(R.id.b14, 14)
                    show(R.id.b13, 13)
                    show(R.id.b12, 12)
                    show(R.id.b11, 11)
                    show(R.id.b10, 10)
                    show(R.id.b9, 9)
                    show(R.id.b7, 7)
                    show(R.id.b5, 5)
                    show(R.id.b4, 4)
                    show(R.id.b3, 3)
                    show(R.id.b2, 2)
                    show(R.id.b1, 1)
                    show(R.id.b0, 0)
                    builder.setView(w)
                    dialog = builder.show()
                    /*n.speed = ((n.speed + 8) % 15) - 7
                    (it as TextView).text = speedNames[n.speed + 7]
                    save()*/
                }
            }
        finishSetup(v, n)
        return v
    }

    fun MainActivity.sleep(n: Sleep): View {
        @SuppressLint("InflateParams")
        val v = layoutInflater.inflate(R.layout.set_sleep, null)
        setupDuration(v.findViewById(R.id.duration), n.duration) { n.duration = it }
        finishSetup(v, n)
        return v
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    fun MainActivity.setupDuration(v: TextView, time0: Float, setTime: (Float) -> Unit) {
        val ctx = this
        var time = time0
        v.apply {
            text = "${time0}s"
            setOnClickListener {
                val dia = Builder(ctx)
                dia.setTitle("Sleep Duration in Seconds")
                val et = EditText(ctx)
                et.setText(time.toString())
                et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                dia.setView(et)
                dia.setPositiveButton("OK") { _, _ ->
                    val w = et.text.toString()
                        .replace(" ", "")
                        .replace(',', '.')
                        .toFloatOrNull()
                    if (w != null && w >= 0f) {
                        time = w
                        setTime(w)
                        (it as TextView).text = "${w}s"
                    }
                    save()
                }
                dia.show()
            }
        }
    }

    private fun MainActivity.randomCall(n: RandomCall): View {
        @SuppressLint("InflateParams")
        val v = layoutInflater.inflate(R.layout.set_randomcall, null)
        setupRandomCallList(v.findViewById(R.id.numCallsId), n.names) { n.names = it }
        finishSetup(v, n)
        return v
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    private fun MainActivity.setupRandomCallList(
        v: TextView,
        names0: List<String>,
        setNames: (List<String>) -> Unit
    ) {
        val ctx = this
        var names = names0
        v.apply {
            text = "#${names0.size}"
            setOnClickListener { it ->
                val dia = Dialog(ctx)
                dia.setContentView(R.layout.dialog_randomcall)
                dia.setTitle("List of called functions; separate them by linebreaks")
                val et = dia.findViewById<EditText>(R.id.randomCallsInput)
                et.setText(names.joinToString("\n"))
                dia.findViewById<View>(R.id.cancelButton)
                    .setOnClickListener {
                        dia.dismiss()
                    }
                dia.findViewById<View>(R.id.okButton)
                    .setOnClickListener { _ ->
                        names = et.text.toString()
                            .split('\n')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        setNames(names)
                        (it as TextView).text = "#${names.size}"
                        save()
                        dia.dismiss()
                    }
                dia.show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n", "MissingInflatedId")
    fun MainActivity.waitForColor(n: WaitForColor): View {
        @SuppressLint("InflateParams")
        val v = layoutInflater.inflate(R.layout.set_waitforcolor, null)
        if (n is ExecIfColor) v.findViewById<TextView>(R.id.title).text = "Color-IfElse"
        val colorView = v.findViewById<View>(R.id.colorId)
        colorView.setBackgroundColor(n.color or black)
        v.findViewById<View>(R.id.settingsButton)
            .setOnClickListener {
                tryStartCamera(n, false) {
                    if (it != null) {
                        colorView.setBackgroundColor((it.color) or black)
                    }
                    false
                }
            }
        if (n is ExecIfColor) {
            setupRandomCallList(v.findViewById(R.id.numCallsId1), n.ifNames) { n.ifNames = it }
            setupRandomCallList(v.findViewById(R.id.numCallsId2), n.elseNames) { n.elseNames = it }
            setupDuration(v.findViewById(R.id.duration), n.duration) { n.duration = it }
        } else {
            v.findViewById<View>(R.id.numCallsId1).visibility = View.GONE
            v.findViewById<View>(R.id.numCallsId2).visibility = View.GONE
            v.findViewById<View>(R.id.duration).visibility = View.GONE
        }
        finishSetup(v, n)
        return v
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n", "MissingInflatedId")
    fun MainActivity.execIfColor(n: ExecIfColor): View {
        return waitForColor(n)
    }

    private fun MainActivity.finishSetup(v: View, n: Command) {
        v.findViewById<TextView>(R.id.delete)
            .setOnClickListener {
                // remove this element
                sequence.remove(n)
                sequenceView.removeView(v)
                save()
            }
        addAddListeners(v, n)
    }

    fun MainActivity.addMotor(item: Any?) {
        val i = sequence.indexOf(item) + 1
        val n = MotorSpeed(true, 0, 0)
        val v = motorSpeed(n)
        sequence.add(i, n)
        sequenceView.addView(v, i)
        save()
    }

    fun MainActivity.addTimer(item: Any?) {
        val i = sequence.indexOf(item) + 1
        val n = Sleep(1f)
        val v = sleep(n)
        sequence.add(i, n)
        sequenceView.addView(v, i)
        save()
    }

    fun MainActivity.addRandomCall(item: Any?) {
        val i = sequence.indexOf(item) + 1
        val n = RandomCall(emptyList())
        val v = randomCall(n)
        sequence.add(i, n)
        sequenceView.addView(v, i)
        save()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun MainActivity.addWaitForColor(item: Any?) {
        val i = sequence.indexOf(item) + 1
        val n = WaitForColor()
        val v = waitForColor(n)
        sequence.add(i, n)
        sequenceView.addView(v, i)
        save()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun MainActivity.addExecIfColor(item: Any?) {
        val i = sequence.indexOf(item) + 1
        val n = ExecIfColor(0f, 0f, 0, 0.5f, 0.2f, emptyList(), emptyList())
        val v = execIfColor(n)
        sequence.add(i, n)
        sequenceView.addView(v, i)
        save()
    }

    private fun MainActivity.runSequence(id: Int) {
        var i = 0
        var li = -1
        var lastView: TextView? = null
        var slept = false
        fun MainActivity.update() {
            runOnUiThread {
                val j = i
                if (j != li) {
                    lastView?.setTextColor(0)
                    lastView = sequenceView.getChildAt(j - 1)?.findViewById(R.id.active)
                    lastView?.setTextColor(-1)
                    li = j
                }
            }
        }
        while (runId == id) {
            val command = sequence.getOrNull(i++) ?: break
            update()
            when (command) {
                is Sleep -> {
                    var sleep = (command.duration * 1000).toLong()
                    while (sleep > 0 && runId == id) {
                        Thread.sleep(1)
                        sleep--
                    }
                    slept = true
                }

                is MotorSpeed -> {
                    // R1, B1, R2, B2, R3, B3, R4, B4
                    val motorId = command.id * 2 + (if (command.red) 0 else 1)
                    Motor.values()[motorId].setSpeed(command.speed)
                }

                is RandomCall -> {
                    // try to find function
                    val name = command.names.randomOrNull()
                    if (name != null) tryExecFunction(name, i)
                }

                is ExecIfColor -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        var shallRun = true
                        var isCloseEnough = false

                        tryStartCamera(command, true) {
                            if (it != null && CameraSensor.target.color == command.color && CameraSensor.isCloseEnough) isCloseEnough =
                                true
                            if (it == null || isCloseEnough) shallRun = false
                            !shallRun
                        }

                        val startTime = System.nanoTime()
                        val timeout = (command.duration * 1e9).toLong()
                        while (
                            runId == id && shallRun &&
                            System.nanoTime() - startTime < timeout
                        ) Thread.sleep(1)

                        val names = if (CameraSensor.isCloseEnough) command.ifNames
                        else command.elseNames
                        val name = names.randomOrNull()
                        if (name != null) tryExecFunction(name, i)

                        shallRun = false
                        waitForColorCallback = null
                    }
                }

                is WaitForColor -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        var shallRun = true
                        var isCloseEnough = false
                        tryStartCamera(command, true) {
                            if (it != null && CameraSensor.target.color == command.color && CameraSensor.isCloseEnough) isCloseEnough =
                                true
                            if (it == null || isCloseEnough) shallRun = false
                            !shallRun
                        }
                        while (runId == id && shallRun) {
                            Thread.sleep(1)
                        }
                        shallRun = false
                        waitForColorCallback = null
                    }
                }
            }
        }
        // delete non-manual nodes
        runOnUiThread {
            for (j in sequence.size - 1 downTo 0) {
                if (!sequence[j].isManual) {
                    sequenceView.removeView(sequenceView.getChildAt(j))
                    sequence.removeAt(j)
                }
            }
        }
        if (!slept) Thread.sleep(10)
        i = 0
        update()
    }

    private fun MainActivity.tryExecFunction(name: String, i: Int) {
        val function = projectNames.firstOrNull { it == name }
            ?: projectNames.firstOrNull { it.equals(name, true) }
        if (function == null) {
            runOnUiThread { toast("$name is unknown", false) }
            Thread.sleep(500)
        } else {
            // then execute it:
            // by inlining it, and later deleting the commands
            val data = preferences.getString("sequence:$function", "") ?: ""
            val (commands, views) = loadProject(data)
            for (cmd in commands) cmd.isManual = false
            sequence.addAll(i, commands)
            runOnUiThread {
                for (v in views.reversed()) {
                    v.isEnabled = false
                    sequenceView.addView(v, i)
                }
            }
        }
    }

    private fun stopMotors() {
        for (motor in Motor.values()) {
            motor.setSpeed(0)
        }
    }

    fun MainActivity.startMotorController() {
        val service = getSystemService(Context.CONSUMER_IR_SERVICE) as ConsumerIrManager
        val controller = LegoIRController(service)
        controller.start()
        CommandLogic.controller = controller
    }

    fun stopMotorController() {
        controller?.requestStop()
        controller = null
        runId++
    }

    fun MainActivity.runRepeatedly() {
        thread {
            val id = ++runId
            while (runId == id) {
                runSequence(id)
            }
            stopRunning()
            stopMotors()
        }
    }

    fun MainActivity.runOnce() {
        thread {
            runSequence(++runId)
            stopRunning()
            stopMotors()
        }
    }

    fun MainActivity.stopRunning() {
        runOnUiThread {
            unbindCamera?.invoke()
            unbindCamera = null
        }
        runId++
    }

    fun MainActivity.setupSeekBar(seekBar: Int, motor: Motor) {
        findViewById<SeekBar>(seekBar)
            .setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    motor.setSpeed(
                        when {
                            progress < 6 -> progress - 7
                            progress == 6 -> 0
                            else -> progress - 5
                        }
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            })
    }

}