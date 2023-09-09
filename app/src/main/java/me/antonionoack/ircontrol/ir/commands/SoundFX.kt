package me.antonionoack.ircontrol.ir.commands

import me.antonionoack.ircontrol.ir.Command
import java.io.File

class SoundFX(var volume: Float, var file: File) : Command() {
    override fun toString(): String {
        return "S$volume;$file"
    }
}