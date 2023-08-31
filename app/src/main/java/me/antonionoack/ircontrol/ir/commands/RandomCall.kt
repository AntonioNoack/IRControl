package me.antonionoack.ircontrol.ir.commands

import me.antonionoack.ircontrol.ir.Command

class RandomCall(var names: List<String>) : Command() {
    override fun toString(): String {
        return "r${names.joinToString(";") { it.trim() }}"
    }
}