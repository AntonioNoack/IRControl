package me.antonionoack.ircontrol.ir.commands

import me.antonionoack.ircontrol.ir.Command

class Sleep(var duration: Float): Command() {
    override fun toString(): String {
        return "s$duration"
    }
}