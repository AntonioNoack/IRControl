package me.antonionoack.ircontrol.camera

import androidx.core.math.MathUtils.clamp

abstract class Image(val width: Int, val height: Int) {

    abstract fun getRGB(vx: Vector2f, tmp: Vector2i): Int
    abstract fun getRGB(vx: Vector2f, tmp: Vector2i, radius: Int): Int

    abstract fun fillRGB(dst: IntArray)

    fun getColor(vx: Vector2f, tmp: Vector2i, radius: Int, dst: Color): Color {
        return rgbToColor(getRGB(vx, tmp, radius), dst)
    }

    fun getColor(vx: Vector2f, tmp: Vector2i, dst: Color): Color {
        return rgbToColor(getRGB(vx, tmp), dst)
    }

    companion object {

        fun rgbToColor(rgb: Int, dst: Color): Color {
            return dst.set(getChannel(rgb, 16), getChannel(rgb, 8), getChannel(rgb, 0))
        }

        fun getChannel(rgb: Int, sh: Int): Float {
            return rgb.shr(sh).and(255) / 255f
        }

        fun relativeToAbsolute(vx: Vector2f, w: Int, h: Int, dst: Vector2i): Vector2i {
            // calculate sample coordinates
            val ax = ((vx.x + 0.5f) * w).toInt()
            val ay = ((vx.y + 0.5f) * h).toInt()
            val xi = clamp(ax, 0, w - 1)
            val yi = clamp(ay, 0, h - 1)
            return dst.set(xi, yi)
        }
    }
}