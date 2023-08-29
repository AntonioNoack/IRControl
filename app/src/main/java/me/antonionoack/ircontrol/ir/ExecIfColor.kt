package me.antonionoack.ircontrol.ir

open class ExecIfColor(
    rx: Float, ry: Float, color: Int, sensitivity: Float,
    var duration: Float,
    var ifNames: List<String>,
    var elseNames: List<String>
) : WaitForColor(rx, ry, color, sensitivity) {
    constructor() : this(0f, 0f, 0, 0.5f, 0.2f, emptyList(), emptyList())

    override fun toString(): String {
        return "${toString("x")};$duration;${ifNames.joinToString(";")};;${elseNames.joinToString(";")}"
    }
}