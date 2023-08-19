package me.antonionoack.ircontrol.ir

open class ExecIfColor(
    rx: Float, ry: Float, color: Int, sensitivity: Float,
    var duration: Float,
    var ifNames: List<String>,
    var elseNames: List<String>
) : WaitForColor(rx, ry, color, sensitivity) {
    override fun toString(): String {
        return "${toString("x")};$duration;${ifNames.joinToString(";") { it.trim() }};;${elseNames.joinToString(";") { it.trim() }}"
    }
}