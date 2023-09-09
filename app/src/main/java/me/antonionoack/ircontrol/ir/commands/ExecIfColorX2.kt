package me.antonionoack.ircontrol.ir.commands

import me.antonionoack.ircontrol.ir.Command

open class ExecIfColorX2(
    val wfc0: WaitForColor,
    val wfc1: WaitForColor,
    var duration: Float,
    var ifNames: List<String>,
    var elseNames: List<String>
) : Command() {

    constructor() : this(WaitForColor(), WaitForColor(), 0.2f, emptyList(), emptyList())

    override fun toString(): String {
        return "X${wfc0.toString("")};${wfc1.toString("")};" +
                "$duration;${ifNames.joinToString(";")};;${elseNames.joinToString(";")}"
    }

    fun set(src: ExecIfColorX2) {
        wfc0.set(src.wfc0)
        wfc1.set(src.wfc1)
    }
}