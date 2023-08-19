package me.antonionoack.ircontrol.ir

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonionoack.ircontrol.camera.CameraSensor.target
import kotlin.math.min

class CrosshairView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint()
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        paint.color = 0xff0000 or (255).shl(24)

        val minSize = min(width, height)
        val ax = x + (target.rx * minSize + width * 0.5f)
        val ay = y + (target.ry * minSize + height * 0.5f)

        val halfLen = 10f
        canvas.drawLine(ax - halfLen, ay, ax + halfLen, ay, paint)
        canvas.drawLine(ax, ay - halfLen, ax, ay + halfLen, paint)

    }
}