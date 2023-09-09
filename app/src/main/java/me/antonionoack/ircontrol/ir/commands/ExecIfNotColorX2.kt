package me.antonionoack.ircontrol.ir.commands

class ExecIfNotColorX2 : ExecIfColorX2 {

    constructor(
        wfc0: WaitForColor, wfc1: WaitForColor, duration: Float,
        ifNames: List<String>, elseNames: List<String>
    ) : super(wfc0, wfc1, duration, ifNames, elseNames)

    constructor() : super(WaitForColor(), WaitForColor(), 0.2f, emptyList(), emptyList())

    override fun toString(): String {
        return "Y${wfc0.toString("")};${wfc1.toString("")};" +
                "$duration;${ifNames.joinToString(";")};;${elseNames.joinToString(";")}"
    }

    fun set(src: ExecIfNotColorX2) {
        wfc0.set(src.wfc0)
        wfc1.set(src.wfc1)
    }
}