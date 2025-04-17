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

class HorizontalSliderView(context: Context, attrs: AttributeSet) : SliderView(context, attrs) {

    private val paint = Paint()
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val padding = height * 0.3f

        val radius = min(height, width) * 0.2f
        val cy = height * 0.5f

        val dx1 = padding * 0.35f
        val dx2 = dx1 * 0.5f
        val split = padding * 2f + value * (width - 4 * padding)


        canvas.save()
        paint.color = color
        val clipPath1 = Path()
        clipPath1.addRect(0f, 0f, split - dx1, height.toFloat(), Path.Direction.CW)
        canvas.clipPath(clipPath1)
        drawBar(canvas, radius, cy, padding)
        canvas.restore()

        canvas.save()
        paint.color = barBackground
        val clipPath2 = Path()
        clipPath2.addRect(split + dx1, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        canvas.clipPath(clipPath2)
        drawBar(canvas, radius, cy, padding)
        canvas.restore()

        // draw tiny circle at the right side...
        paint.color = color
        canvas.drawCircle(width - (padding + radius), height * 0.5f, 0.05f * height, paint)

        paint.color = color
        canvas.drawRect(
            split - dx2, 0f,
            split + dx2, height.toFloat(),
            paint
        )

        paint.color = white
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = height * 0.2f
        canvas.drawText(
            "%.2f".format(value),
            width - height * 0.07f,
            height - height * 0.07f,
            paint
        )
    }

    private fun drawBar(canvas: Canvas, radius: Float, cy: Float, padding: Float) {

        val top = cy - radius
        val bottom = cy + radius

        canvas.drawArc(
            padding, top,
            padding + radius * 2f, bottom,
            90f, 180f, true, paint
        )

        canvas.drawRect(
            padding + radius - 0.5f, top,
            width - (padding + radius) + 0.5f, bottom,
            paint
        )

        canvas.drawArc(
            width - (padding + radius * 2f), top,
            width - padding, bottom,
            -90f, 180f, true, paint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateValueFromTouch(event.x)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateValueFromTouch(x: Float) {
        val padding = height * 0.3f
        val normalized = (x - 2 * padding) / (width - 4 * padding)
        value = clamp(normalized, 0f, 1f)
        onChangeListener?.invoke(this, value, Unit)
    }

}