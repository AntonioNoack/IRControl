package me.antonionoack.ircontrol.camera

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Vector2f(var x: Float, var y: Float) {
    fun set(nx: Float, ny: Float): Vector2f {
        x = nx
        y = ny
        return this
    }

    fun rotate(degrees: Int): Vector2f {
        val a = degrees * PI / 180
        val c = cos(a).toFloat()
        val s = sin(a).toFloat()
        return set(c * x - s * y, c * y + s * x)
    }
}