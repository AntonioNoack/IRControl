package me.antonionoack.ircontrol.ir.commands

import me.antonionoack.ircontrol.camera.Color
import me.antonionoack.ircontrol.ir.Command

open class WaitForColor(
    var rx: Float,
    var ry: Float,
    var color: Int,
    var sensitivity: Float,
) : Command() {

    constructor() : this(0f, 0f, 0, 0.5f)

    fun set(src: WaitForColor) {
        rx = src.rx
        ry = src.ry
        color = src.color
        sensitivity = src.sensitivity
        resetAverage()
    }

    private fun resetAverage() {
        currAverageSquare.set(0f, 0f, 0f)
        numSamples = 0
    }

    val currAverageSquare = Color()
    var numSamples = 0

    override fun toString(): String {
        return toString("c")
    }

    fun toString(sym: String): String {
        return "$sym$rx;$ry;${color.and(0xffffff).toString(16)};$sensitivity"
    }
}