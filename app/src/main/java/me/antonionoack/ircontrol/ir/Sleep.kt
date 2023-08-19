package me.antonionoack.ircontrol.ir

class Sleep(var duration: Float): Command() {
    override fun toString(): String {
        return "s$duration"
    }
}