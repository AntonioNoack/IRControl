package me.antonionoack.ircontrol.ir

enum class Channel(val id: Int, val RED: Motor, val BLUE: Motor) {
    C1(0, Motor.R1, Motor.B1),
    C2(1, Motor.R2, Motor.B2),
    C3(2, Motor.R3, Motor.B3),
    C4(3, Motor.R4, Motor.B4);
}