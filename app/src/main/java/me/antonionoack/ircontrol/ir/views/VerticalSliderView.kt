package me.antonionoack.ircontrol.ir.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import java.lang.Math.clamp
import kotlin.math.min

class VerticalSliderView(context: Context, attrs: AttributeSet) : SliderView(context, attrs) {

    private val paint = Paint()
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val padding = width * 0.3f

        val radius = min(height, width) * 0.2f
        val cy = width * 0.5f

        val dx1 = padding * 0.35f
        val dx2 = dx1 * 0.5f
        val split = padding * 2f + (1f - value) * (height - 4 * padding)

        canvas.save()
        paint.color = barBackground
        val clipPath1 = Path()
        clipPath1.addRect(0f, 0f, width.toFloat(), split - dx1, Path.Direction.CW)
        canvas.clipPath(clipPath1)
        drawBar(canvas, radius, cy, padding)
        canvas.restore()

        canvas.save()
        paint.color = color
        val clipPath2 = Path()
        clipPath2.addRect(0f, split + dx1, width.toFloat(), height.toFloat(), Path.Direction.CW)
        canvas.clipPath(clipPath2)
        drawBar(canvas, radius, cy, padding)
        canvas.restore()

        // draw tiny circle at the top side...
        paint.color = color
        canvas.drawCircle(width * 0.5f, (padding + radius), 0.05f * width, paint)

        paint.color = color
        canvas.drawRect(
            0f, split - dx2,
            width.toFloat(), split + dx2,
            paint
        )

        paint.color = white
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = width * 0.2f
        canvas.drawText("%.2f".format(value), width - width * 0.07f, height - width * 0.09f, paint)
    }

    private fun drawBar(canvas: Canvas, radius: Float, cy: Float, padding: Float) {

        val top = cy - radius
        val bottom = cy + radius

        canvas.drawArc(
            padding, top,
            bottom, padding + radius * 2f,
            180f, 180f, true, paint
        )

        canvas.drawRect(
            top, padding + radius - 0.5f,
            bottom, height - (padding + radius) + 0.5f,
            paint
        )

        canvas.drawArc(
            top, height - (padding + radius * 2f),
            bottom, height - padding,
            0f, 180f, true, paint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateValueFromTouch(event.y)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateValueFromTouch(y: Float) {
        val padding = width * 0.3f
        val normalized = 1f - (y - 2 * padding) / (height - 4 * padding)
        value = clamp(normalized, 0f, 1f)
        onChangeListener?.invoke(this, value, Unit)
    }

}