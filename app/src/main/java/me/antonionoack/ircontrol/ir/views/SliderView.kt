package me.antonionoack.ircontrol.ir.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import me.antonionoack.ircontrol.camera.CameraSensor.BLACK

abstract class SliderView(
    context: Context, attrs: AttributeSet
) : View(context, attrs) {
    var color = 0x40b0ec or BLACK
    var barBackground = 0x4a4458 or BLACK
    var value = 0.3f
    val white = -1

    var onChangeListener: ((SliderView, Float, Unit) -> Unit)? = null

    fun addOnChangeListener(listener: (SliderView, Float, Unit) -> Unit) {
        val prevListener = onChangeListener
        onChangeListener = { view, value, ignored ->
            prevListener?.invoke(view, value, ignored)
            listener(view, value, ignored)
        }
    }

}