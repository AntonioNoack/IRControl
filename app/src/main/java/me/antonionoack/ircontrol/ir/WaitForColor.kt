package me.antonionoack.ircontrol.ir

open class WaitForColor(
    var rx: Float,
    var ry: Float,
    var color: Int,
    var sensitivity: Float
) : Command() {

    constructor() : this(0f, 0f, 0, 0.5f)

    override fun toString(): String {
        return toString("c")
    }

    fun set(src: WaitForColor) {
        rx = src.rx
        ry = src.ry
        color = src.color
        sensitivity = src.sensitivity
    }

    fun toString(sym: String): String {
        return "$sym$rx;$ry;${color.and(0xffffff).toString(16)};$sensitivity"
    }
}