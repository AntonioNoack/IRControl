package me.antonionoack.ircontrol.ir

import androidx.core.math.MathUtils.clamp
import me.antonionoack.ircontrol.ir.CommandLogic.BREAK_THEN_FLOAT
import me.antonionoack.ircontrol.ir.CommandLogic.FLOAT
import kotlin.math.max

enum class Motor {

    R1, B1, R2, B2, R3, B3, R4, B4;

    var active = 0
    private var targetSpeed = FLOAT
        set(value) {
            // active for a few more frames
            field = if (value == -1 || value == +1) 0 else clamp(value, -7, +7)
            if (field == FLOAT || field == BREAK_THEN_FLOAT) active = 7
        }

    var speed = FLOAT
        private set

    fun setSpeed(i: Int) {
        targetSpeed = i
    }

    fun tick() {
        active = max(active - 1, 0)
        if (speed in -7..7 && speed != 0) active = 7 // running
        speed = clamp(targetSpeed, speed - 1, speed + 1)
    }
}