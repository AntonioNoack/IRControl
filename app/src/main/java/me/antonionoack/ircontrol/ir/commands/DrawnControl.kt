package me.antonionoack.ircontrol.ir.commands

import me.antonionoack.ircontrol.ir.Command
import me.antonionoack.ircontrol.ir.Motor

class DrawnControl(
    var duration: Float,
    var speedChanges: List<SpeedChange>,
) : Command() {
    data class SpeedChange(val motor: Motor, val speed: Int, val time: Float) {
        fun apply() {
            motor.setSpeed(speed)
        }
    }

    override fun toString(): String {
        return "d$duration;${speedChanges.joinToString(";") { "${it.motor.ordinal};${it.speed};${it.time}" }}"
    }
}