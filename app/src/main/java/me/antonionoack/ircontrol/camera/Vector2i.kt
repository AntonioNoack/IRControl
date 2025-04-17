package me.antonionoack.ircontrol.camera

data class Vector2i(var x: Int, var y: Int) {
    constructor() : this(0, 0)

    fun set(nx: Int, ny: Int): Vector2i {
        x = nx
        y = ny
        return this
    }
}