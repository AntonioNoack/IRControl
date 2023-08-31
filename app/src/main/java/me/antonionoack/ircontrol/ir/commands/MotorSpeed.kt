package me.antonionoack.ircontrol.ir.commands

import me.antonionoack.ircontrol.ir.Command

class MotorSpeed(var red: Boolean, var id: Int, var speed: Int) : Command() {
    override fun toString(): String {
        return "m" +
                "${(if (red) 0 else 1) + id * 2}" +
                (speed + 7).toString(16)
    }
}