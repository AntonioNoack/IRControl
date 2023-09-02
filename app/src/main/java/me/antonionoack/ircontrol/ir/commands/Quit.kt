package me.antonionoack.ircontrol.ir.commands

import me.antonionoack.ircontrol.ir.Command

/**
 * Quits the current execution,
 * like pressing on the stop button
 * */
class Quit : Command() {
    override fun toString(): String {
        return "q"
    }
}