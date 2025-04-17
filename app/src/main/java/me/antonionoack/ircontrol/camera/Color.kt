package me.antonionoack.ircontrol.camera

import me.antonionoack.ircontrol.camera.Image.Companion.getChannel
import java.lang.Math.clamp
import kotlin.math.sqrt

class Color(var r: Float, var g: Float, var b: Float) {
    constructor() : this(0f, 0f, 0f)

    fun set(nr: Float, ng: Float, nb: Float): Color {
        r = nr
        g = ng
        b = nb
        return this
    }

    fun setRGB(rgb: Int): Color {
        return set(
            getChannel(rgb, 16),
            getChannel(rgb, 8),
            getChannel(rgb, 0),
        )
    }

    fun mix(other: Color, f: Float, dst: Color): Color {
        return dst.set(
            mix(r, other.r, f),
            mix(g, other.g, f),
            mix(b, other.b, f)
        )
    }

    fun mixWith(other: Color, f: Float): Color {
        return mix(other, f, this)
    }

    fun normalizedDistanceTo(other: Color): Float {
        val maxDistance = sqrt(3f)
        val dx = r - other.r
        val dy = g - other.g
        val dz = b - other.b
        return sqrt(dx * dx + dy * dy + dz * dz) / maxDistance
    }

    fun toRGB(): Int {
        val max = 255f
        val ri = clamp(r * max, 0f, max).toInt()
        val gi = clamp(g * max, 0f, max).toInt()
        val bi = clamp(b * max, 0f, max).toInt()
        return ri.shl(16) or gi.shl(8) or bi
    }

    companion object {
        fun mix(a: Float, b: Float, f: Float): Float {
            return a + (b - a) * f
        }
    }

}