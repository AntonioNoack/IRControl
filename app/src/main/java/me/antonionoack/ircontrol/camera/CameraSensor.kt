package me.antonionoack.ircontrol.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.core.ImageProxy.PlaneProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils.clamp
import com.google.android.material.slider.Slider
import me.antonionoack.ircontrol.MainActivity
import me.antonionoack.ircontrol.R
import me.antonionoack.ircontrol.ir.CommandLogic.save
import me.antonionoack.ircontrol.ir.commands.WaitForColor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


object CameraSensor {

    const val black = (255).shl(24)

    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    const val CAMERA_PERMISSIONS_ID = 10

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun MainActivity.tryStartCamera(
        src: List<WaitForColor>, inBackground: Boolean,
        onValuesChanged: (tmp: WaitForColor?) -> Boolean
    ) {
        isCloseEnough = false
        waitForColorCommand = src
        waitForColorCallback = onValuesChanged
        runCameraInBackground = inBackground
        // Request camera permissions
        if (allPermissionsGranted()) {
            runOnUiThread {
                startCamera(src)
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, CAMERA_PERMISSIONS_ID)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun MainActivity.tryStartCamera(
        src: WaitForColor, inBackground: Boolean,
        onValuesChanged: (tmp: WaitForColor?) -> Boolean
    ) = tryStartCamera(listOf(src), inBackground, onValuesChanged)

    private fun yuv2rgb(y: Int, u: Int, v: Int): Int {
        val r = y + (+91881 * v - 11698176).shr(16)
        val g = y + (-22544 * u - 46793 * v + 8840479).shr(16)
        val b = y + (+116130 * u - 14823260).shr(16)
        return clamp(r, 0, 255).shl(16) or
                clamp(g, 0, 255).shl(8) or
                clamp(b, 0, 255)
    }

    private fun rgb2yuv(rgb: Int): Int {
        val r = rgb.shr(16).and(255)
        val g = rgb.shr(8).and(255)
        val b = rgb.and(255)
        val y = (+19595 * r + 38470 * g + 7471 * b).shr(16)
        val u = (-11076 * r - 21692 * g + 32768 * b + 8355840).shr(16)
        val v = (+32768 * r - 27460 * g - 5308 * b + 8355840).shr(16)
        return clamp(y, 0, 255).shl(16) or
                clamp(u, 0, 255).shl(8) or
                clamp(v, 0, 255)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun sample(p: PlaneProxy, x: Int, y: Int): Int {
        return p.buffer[p.buffer.position() + x * p.pixelStride + y * p.rowStride].toInt().and(255)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun sampleSquare(p: PlaneProxy, x0: Int, y0: Int, ri: Int, w: Int, h: Int): Int {
        var s = 0
        val minX = max(x0 - ri, 0)
        val minY = max(y0 - ri, 0)
        val maxX = min(x0 + ri + 1, w)
        val maxY = min(y0 + ri + 1, h)
        val buffer = p.buffer
        val stride = p.pixelStride
        for (y in minY until maxY) {
            var i = p.buffer.position() + minX * stride + y * p.rowStride
            for (x in minX until maxX) {
                s += buffer[i].toInt().and(255)
                i += stride
            }
        }
        val c = (maxY - minY) * (maxX - minX)
        return s / c
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun MainActivity.startCamera(src: List<WaitForColor>) {
        targets = src.map { WaitForColor().apply { set(it) } }
        if (runCameraInBackground) {
            if (!hasBackgroundCamera) {
                startCamera(
                    findViewById(R.id.sequencer), null, addTouchListener = false
                )
                hasBackgroundCamera = true
            }
        } else {

            val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.dialog_camera)

            val dst = startCamera(
                dialog.findViewById(R.id.primaryGroup), dialog, addTouchListener = true
            )
            // this ruins the background camera :/
            hasBackgroundCamera = false

            val slider = dialog.findViewById<Slider>(R.id.sensitivitySlider)
            slider.value = dst.first().sensitivity
            slider.addOnChangeListener { _, value, _ ->
                for (dstI in dst) dstI.sensitivity = value
            }

            dialog.findViewById<View>(R.id.okButton).setOnClickListener {
                for (i in src.indices) {
                    src[i].set(dst[i])
                }
                save()
                waitForColorCallback?.invoke(null)
                dialog.dismiss()
            }
            dialog.findViewById<View>(R.id.cancelButton).setOnClickListener {
                waitForColorCallback?.invoke(null)
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    var targets = listOf<WaitForColor>()
    var isCloseEnough = false
    var isDifferentEnough = false

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun MainActivity.startCamera(
        view: View, dialog: Dialog?,
        addTouchListener: Boolean
    ): List<WaitForColor> {

        val preview1 = view.findViewById<PreviewView>(R.id.preview)
        preview1.visibility = View.VISIBLE

        var queryColor = false

        val color0 = view.findViewById<View>(R.id.colorPreview0)
        color0?.setBackgroundColor(0)

        val color1 = view.findViewById<View>(R.id.colorPreview1)
        color1?.setBackgroundColor(targets.first().color or black)

        val crosshair = view.findViewById<View>(R.id.crosshair)
        crosshair?.invalidate()

        val resultPreview = view.findViewById<TextView>(R.id.resultPreview)
        if (addTouchListener) preview1.setOnTouchListener { v, event ->
            if (v == preview1 &&
                event.action == MotionEvent.ACTION_DOWN &&
                event.x.toInt() in 0 until v.width &&
                event.y.toInt() in 0 until v.height
            ) {
                val x = event.x
                val y = event.y
                // video is cropped -> crop this mathematically, too
                val minSize = min(v.width, v.height)
                for (target in targets) {
                    target.rx = (x - v.width * 0.5f) / minSize  // [-.5, +.5] or more
                    target.ry = (y - v.height * 0.5f) / minSize // [-.5, +.5] or more
                    waitForColorCallback?.invoke(target)
                }
                crosshair?.invalidate()
                queryColor = true
            }
            true
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val vx = Vector2f(0f, 0f)
        cameraProviderFuture.addListener({

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(preview1.surfaceProvider)
            unbindCamera = {
                cameraProvider.unbindAll()
                preview1.visibility = View.GONE
                color0?.setBackgroundColor(0)
                hasBackgroundCamera = false
            }

            val analysis = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
            analysis.setAnalyzer(cameraExecutor) { img ->

                var isCloseEnough = false
                var isDifferentEnough = false
                for (target in targets) {
                    vx.set(target.rx, target.ry).rotate(-img.imageInfo.rotationDegrees)

                    val w = img.width
                    val h = img.height

                    // calculate sample coordinates
                    val minSize = min(w, h)
                    val ax = (vx.x * minSize + w * 0.5f).toInt()
                    val ay = (vx.y * minSize + h * 0.5f).toInt()
                    val cx = clamp(ax, 0, w - 1)
                    val cy = clamp(ay, 0, h - 1)
                    val ux = cx ushr 1
                    val uy = cy ushr 1

                    // sample color there
                    val y = sample(img.planes[0], cx, cy)
                    val u = sample(img.planes[1], ux, uy)
                    val v = sample(img.planes[2], ux, uy)

                    // only sample if necessary?
                    val ry = 7 // 15x15
                    val ru = 3 //  7x7
                    val ys = sampleSquare(img.planes[0], cx, cy, ry, w, h)
                    val us = sampleSquare(img.planes[1], ux, uy, ru, w shr 1, h shr 1)
                    val vs = sampleSquare(img.planes[2], ux, uy, ru, w shr 1, h shr 1)
                    if (target.ctr < 8) {
                        val nc = target.ctr + 1
                        target.avgY0 = (target.avgY0 * target.ctr + ys) / nc
                        target.avgU0 = (target.avgU0 * target.ctr + us) / nc
                        target.avgV0 = (target.avgV0 * target.ctr + vs) / nc
                    } else {
                        target.avgY0 = (target.avgY0 * 7 + ys) shr 3
                        target.avgU0 = (target.avgU0 * 7 + us) shr 3
                        target.avgV0 = (target.avgV0 * 7 + vs) shr 3
                    }
                    target.ctr++

                    // convert to rgb, and save it
                    val color = yuv2rgb(y, u, v)
                    color0?.setBackgroundColor(color or black)

                    if (queryColor) {
                        // convert to rgb, and save it
                        target.color = color
                        color1.setBackgroundColor(target.color or black)
                        queryColor = false
                    }

                    // evaluate whether we're close enough
                    val maxDist = 255 * sqrt(3.0)
                    val dstColor = target.color
                    val dstColorYUV = rgb2yuv(dstColor)
                    val dx0 = y - dstColorYUV.shr(16).and(255)
                    val dy0 = u - dstColorYUV.shr(8).and(255)
                    val dz0 = v - dstColorYUV.and(255)
                    val dist0 = sqrt((dx0 * dx0 + dy0 * dy0 + dz0 * dz0).toDouble()) / maxDist

                    // check if value is different enough from avg
                    val dx1 = ys - target.avgY0
                    val dy1 = us - target.avgU0
                    val dz1 = vs - target.avgV0
                    val dist1 = sqrt((dx1 * dx1 + dy1 * dy1 + dz1 * dz1).toDouble()) / maxDist

                    isCloseEnough = isCloseEnough || dist0 < 1f - target.sensitivity
                    isDifferentEnough = isDifferentEnough || (dist1 > 1f - target.sensitivity && target.ctr > 8)

                }
                CameraSensor.isCloseEnough = isCloseEnough
                CameraSensor.isDifferentEnough = isDifferentEnough

                if (waitForColorCallback?.invoke(targets.first()) == true) {
                    dialog?.dismiss()
                } else if (resultPreview != null) {
                    runOnUiThread {
                        resultPreview.setText(
                            if (isCloseEnough) R.string.motor_icon
                            else R.string.timer_icon
                        )
                    }
                }

                img.close()
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
            } catch (e: Exception) {
                toast("Use case binding failed: $e", false)
            }

        }, ContextCompat.getMainExecutor(this))

        return targets
    }

    fun MainActivity.allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

}