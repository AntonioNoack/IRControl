package me.antonionoack.ircontrol.camera

import androidx.camera.core.ImageProxy.PlaneProxy
import androidx.core.math.MathUtils.clamp
import kotlin.math.max
import kotlin.math.min

class YUVImage(
    width: Int, height: Int,
    private val planeY: PlaneProxy,
    private val planeU: PlaneProxy,
    private val planeV: PlaneProxy
) : Image(width, height) {

    override fun getRGB(vx: Vector2f, tmp: Vector2i): Int {
        relativeToAbsolute(vx, width, height, tmp)
        val xi = clamp(tmp.x, 0, width - 1)
        val yi = clamp(tmp.y, 0, height - 1)
        val y = sample(planeY, xi, yi)
        val w1 = width shr 1
        val h1 = height shr 1
        relativeToAbsolute(vx, w1, h1, tmp)
        val xj = clamp(tmp.x, 0, w1 - 1)
        val yj = clamp(tmp.y, 0, h1 - 1)
        val u = sample(planeU, xj, yj)
        val v = sample(planeV, xj, yj)
        return yuv2rgb(y, u, v)
    }

    override fun getRGB(vx: Vector2f, tmp: Vector2i, radius: Int): Int {
        relativeToAbsolute(vx, width, height, tmp)
        val y = sampleSquare(planeY, tmp.x, tmp.y, radius, width, height)
        val w1 = width shr 1
        val h1 = height shr 1
        val r1 = radius shr 1
        relativeToAbsolute(vx, w1, h1, tmp)
        val u = sampleSquare(planeU, tmp.x, tmp.y, r1, w1, h1)
        val v = sampleSquare(planeV, tmp.x, tmp.y, r1, w1, h1)
        return yuv2rgb(y, u, v)
    }

    private fun sample(p: PlaneProxy, x: Int, y: Int): Int {
        val index = p.buffer.position() + x * p.pixelStride + y * p.rowStride
        return p.buffer[index].toInt().and(255)
    }

    private fun sampleSquare(p: PlaneProxy, x0: Int, y0: Int, ri: Int, w: Int, h: Int): Int {
        var colorSum = 0
        val minX = max(x0 - ri, 0)
        val minY = max(y0 - ri, 0)
        val maxX = min(x0 + ri + 1, w)
        val maxY = min(y0 + ri + 1, h)
        val buffer = p.buffer
        val stride = p.pixelStride
        for (y in minY until maxY) {
            var index = p.buffer.position() + minX * stride + y * p.rowStride
            for (x in minX until maxX) {
                colorSum += buffer[index].toInt().and(255)
                index += stride
            }
        }
        val totalWeight = (maxY - minY) * (maxX - minX)
        return colorSum / max(1, totalWeight)
    }

    override fun fillRGB(dst: IntArray) {
        var i = 0
        for (yi in 0 until height) {
            for (xi in 0 until width) {
                val y = sample(planeY, xi, yi)
                val xj = xi shr 1
                val yj = yi shr 1
                val u = sample(planeU, xj, yj)
                val v = sample(planeV, xj, yj)
                dst[i++] = yuv2rgb(y, u, v)
            }
        }
    }

    companion object {

        private fun yuv2rgb(y: Int, u: Int, v: Int): Int {
            val r = y + (+91881 * v - 11698176).shr(16)
            val g = y + (-22544 * u - 46793 * v + 8840479).shr(16)
            val b = y + (+116130 * u - 14823260).shr(16)
            return clamp(r, 0, 255).shl(16) or
                    clamp(g, 0, 255).shl(8) or
                    clamp(b, 0, 255)
        }

        private fun rgb2yuv(rgb: Int): Int {
            val r = rgb.shr(16).and(255)
            val g = rgb.shr(8).and(255)
            val b = rgb.and(255)
            val y = (+19595 * r + 38470 * g + 7471 * b).shr(16)
            val u = (-11076 * r - 21692 * g + 32768 * b + 8355840).shr(16)
            val v = (+32768 * r - 27460 * g - 5308 * b + 8355840).shr(16)
            return clamp(y, 0, 255).shl(16) or
                    clamp(u, 0, 255).shl(8) or
                    clamp(v, 0, 255)
        }

    }
}