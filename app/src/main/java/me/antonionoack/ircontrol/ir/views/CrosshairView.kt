package me.antonionoack.ircontrol.ir.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.antonionoack.ircontrol.camera.CameraSensor.BLACK
import me.antonionoack.ircontrol.camera.CameraSensor.lastImageData
import me.antonionoack.ircontrol.camera.CameraSensor.lastImageHeight
import me.antonionoack.ircontrol.camera.CameraSensor.lastImageRotationDegrees
import me.antonionoack.ircontrol.camera.CameraSensor.lastImageWidth
import me.antonionoack.ircontrol.camera.CameraSensor.targets
import me.antonionoack.ircontrol.camera.Vector2f
import kotlin.math.max
import kotlin.math.min

class CrosshairView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val tmp = Vector2f()
    private val paint = Paint()
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        paint.color = 0xffff0000.toInt()
        val self = this
        val viewWidth = self.width
        val viewHeight = self.height
        val viewCenterX = self.x + viewWidth * 0.5f
        val viewCenterY = self.y + viewHeight * 0.5f

        for (target in targets) {

            val ax = viewCenterX + target.rx * viewWidth
            val ay = viewCenterY + target.ry * viewHeight

            val halfLen = 10f
            canvas.drawLine(ax - halfLen, ay, ax + halfLen, ay, paint)
            canvas.drawLine(ax, ay - halfLen, ax, ay + halfLen, paint)
        }

        // draw the last image as dots/lines over the original image
        if (false) renderDebugDots(viewCenterX, viewCenterY, viewWidth, viewHeight, canvas)
    }

    private fun renderDebugDots(
        viewCenterX: Float, viewCenterY: Float,
        viewWidth: Int, viewHeight: Int,
        canvas: Canvas
    ) {

        val imageWidth = lastImageWidth
        val imageHeight = lastImageHeight
        val imageData = lastImageData
        val imageRotation = lastImageRotationDegrees

        if (imageData.size < imageWidth * imageHeight) return
        if (imageWidth == 0 || imageHeight == 0) return

        val imageCenterX = imageWidth.shr(1)
        val imageCenterY = imageHeight.shr(1)

        val sizeX = min(viewWidth, imageWidth) / 3
        val sizeY = min(viewHeight, imageHeight) / 3
        val r = 1.5f
        val invImageWidth = 1f / imageWidth
        val invImageHeight = 1f / imageHeight
        for (yi in 0 until sizeY) {
            // src and dst positions
            val y0 = partition(yi, imageHeight, sizeY)
            val y1 = partition(yi + 1, imageHeight, sizeY)
            val yc = (y0 + y1).shr(1)
            val ry = (yc - imageCenterY) * invImageHeight

            for (xi in 0 until sizeX) {
                val x0 = partition(xi, imageWidth, sizeX)
                val x1 = partition(xi + 1, imageWidth, sizeX)
                val xc = (x0 + x1).shr(1)
                val rx = (xc - imageCenterX) * invImageWidth

                tmp.set(rx, ry).rotate(imageRotation)

                val viewX = viewCenterX + (tmp.x * viewWidth)
                val viewY = viewCenterY + (tmp.y * viewHeight)

                val pixelId = xc + yc * imageWidth
                if (pixelId !in imageData.indices) return
                paint.color = imageData[pixelId] or BLACK
                canvas.drawRect(viewX - r, viewY - r, viewX + r, viewY + r, paint)
            }
        }
    }

    private fun partition(x: Int, total: Int, steps: Int): Int {
        val rawValue = (x.toLong() * total.toLong()) / max(steps, 1).toLong()
        return rawValue.toInt()
    }
}