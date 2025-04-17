package me.antonionoack.ircontrol.camera

import me.antonionoack.ircontrol.camera.CameraSensor.targets
import kotlin.math.max
import kotlin.math.sqrt

class PixelEvents {

    private val currPixel = Color()
    private val currSquare = Color()
    private val targetColor = Color()
    private val tmpVector2i = Vector2i()
    private val relativePos = Vector2f(0f, 0f)

    fun update(imageRotationDegrees: Int, image: Image) {
        var isCloseEnough = false
        var isDifferentEnough = false
        for (target in targets) {

            relativePos.set(target.rx, target.ry)
                .rotate(-imageRotationDegrees)

            image.getColor(relativePos, tmpVector2i, currPixel)
            image.getColor(relativePos, tmpVector2i, 7, currSquare)

            val factor = max(1f / (++target.numSamples), 0.33f)
            target.currAverageSquare.mixWith(currSquare, factor)

            // evaluate whether we're close enough
            val maxDist = sqrt(3f)
            targetColor.setRGB(target.color)
            val dist0 = currPixel.normalizedDistanceTo(targetColor)

            // check if value is different enough from avg
            val dist1 = currSquare.normalizedDistanceTo(target.currAverageSquare) / maxDist

            if (dist0 < 1f - target.sensitivity) {
                isCloseEnough = true
            }
            if (dist1 > 1f - target.sensitivity && target.numSamples > 8) {
                isDifferentEnough = true
            }
        }

        CameraSensor.isCloseEnough = isCloseEnough
        CameraSensor.isDifferentEnough = isDifferentEnough

    }
}