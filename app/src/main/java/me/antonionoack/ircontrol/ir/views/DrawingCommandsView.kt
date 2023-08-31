package me.antonionoack.ircontrol.ir.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import me.antonionoack.ircontrol.ir.Motor
import me.antonionoack.ircontrol.ir.commands.DrawnControl
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class DrawingCommandsView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        val colors = intArrayOf(
            // 0x620000,
            0x930D0D,
            0xC13131,
            0xE25656,
            0xfa8c8c,
            // 0x093682,
            0x2256AE,
            0x3F75D1,
            0x558BE7,
            0x83acf2
        )
    }

    var selectedMotor = 0

    val speedChanges = ArrayList<DrawnControl.SpeedChange>()

    private val paint = Paint()

    var scroll = 0f
    var scale = 100f

    init {
        var downTime = 0L
        var motion = 0f
        var posX = 0f
        var posY = 0f
        var mx = 0f
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = System.nanoTime()
                    posX = event.x
                    posY = event.y
                    mx = 0f
                }

                MotionEvent.ACTION_UP -> {
                    val dt = (System.nanoTime() - downTime) / 1e9
                    val isClick = dt < 0.3 && motion < 10f
                    if (isClick) performClick()
                    if (abs(mx) < 10f) {
                        val time = (scroll + event.x) / scale
                        val speed = min(max((event.y * 15 / height).toInt() - 7, -7), 7)
                        val w = DrawnControl.SpeedChange(Motor.values()[selectedMotor], speed, time)
                        speedChanges.removeIf { it.motor == w.motor && abs(it.time - time) < 0.2f }
                        if (dt < 0.3f) {
                            speedChanges.add(w)
                            speedChanges.sortBy { it.time }
                        }
                        invalidate()
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = posX - event.x
                    val dy = posY - event.y
                    motion += sqrt(dx * dx + dy * dy)
                    mx += dx
                    scroll += dx
                    posX = event.x
                    posY = event.y
                    invalidate()
                }
            }
            true
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        scale = height / 15f

        var usedMotors = 0
        for (cmd in speedChanges) {
            usedMotors = usedMotors or (1 shl cmd.motor.ordinal)
        }

        // draw coordinate system
        val x0 = ceil(scroll / scale)
        val x1 = floor((scroll + width) / scale)

        fun x(time: Float): Float {
            return time * scale - scroll
        }

        fun y(speed: Int): Float {
            return height * (speed / 7.5f * .5f + .5f)
        }

        paint.color = 0xff555555.toInt()
        paint.strokeWidth = 1f
        for (i in x0.toInt()..x1.toInt()) {
            val xi = x(i.toFloat())
            canvas.drawLine(xi, 0f, xi, height.toFloat(), paint)
        }

        for (yi in -7..7) {
            val yf = y(yi)
            canvas.drawLine(0f, yf, width.toFloat(), yf, paint)
        }

        paint.color = 0xff888888.toInt()
        val yf = y(0)
        canvas.drawLine(0f, yf, width.toFloat(), yf, paint)

        val states = IntArray(8)
        val lastTime = FloatArray(8)
        lastTime.fill(Float.NaN)
        paint.strokeWidth = 3f
        for (change in speedChanges) {
            // draw motor line
            val i = change.motor.ordinal
            val lt = lastTime[i]
            if (lt.isFinite()) {
                paint.color = colors[i] or (255 shl 24)
                canvas.drawLine(
                    x(lt), y(states[i]),
                    x(change.time), y(change.speed), paint
                )
            }
            lastTime[i] = change.time
            states[i] = change.speed
        }

        for (change in speedChanges) {
            // draw motor point
            val i = change.motor.ordinal
            paint.color = colors[i] or (255 shl 24)
            val xi = x(change.time)
            val yi = y(change.speed)
            val s = 10f
            canvas.drawRect(xi - s, yi - s, xi + s, yi + s, paint)
        }

    }
}