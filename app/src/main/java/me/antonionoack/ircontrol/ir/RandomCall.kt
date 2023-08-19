package me.antonionoack.ircontrol.ir

class RandomCall(var names: List<String>) : Command() {
    override fun toString(): String {
        return "r${names.joinToString(";") { it.trim() }}"
    }
}