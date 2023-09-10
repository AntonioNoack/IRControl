package me.antonionoack.ircontrol.ir.commands

class WaitForColorChange(rx: Float, ry: Float, sensitivity: Float) :
    WaitForColor(rx, ry, -1, sensitivity) {

    constructor() : this(0f, 0f, 0.5f)

    override fun toString(): String {
        return toString("w")
    }
}