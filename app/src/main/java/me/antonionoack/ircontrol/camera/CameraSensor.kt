package me.antonionoack.ircontrol.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.antonionoack.ircontrol.MainActivity
import me.antonionoack.ircontrol.R
import me.antonionoack.ircontrol.ir.CommandLogic.save
import me.antonionoack.ircontrol.ir.commands.WaitForColor
import me.antonionoack.ircontrol.ir.views.SliderView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


object CameraSensor {

    const val BLACK = (255).shl(24)

    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    const val CAMERA_PERMISSIONS_ID = 10

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

    fun MainActivity.tryStartCamera(
        src: WaitForColor, inBackground: Boolean,
        onValuesChanged: (tmp: WaitForColor?) -> Boolean
    ) = tryStartCamera(listOf(src), inBackground, onValuesChanged)

    @SuppressLint("ClickableViewAccessibility")
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

            val slider = dialog.findViewById<SliderView>(R.id.sensitivitySlider)
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
    fun MainActivity.startCamera(
        view: View, dialog: Dialog?,
        addTouchListener: Boolean
    ): List<WaitForColor> {

        val previewView = view.findViewById<PreviewView>(R.id.preview)
        previewView.visibility = View.VISIBLE

        var queryColor = false

        val currColorView = view.findViewById<View>(R.id.colorPreview0)
        currColorView?.setBackgroundColor(0)

        val targetColorView = view.findViewById<View>(R.id.colorPreview1)
        targetColorView?.setBackgroundColor(targets.first().color or BLACK)

        val crosshair = view.findViewById<View>(R.id.crosshair)
        crosshair?.invalidate()

        val resultPreview = view.findViewById<TextView>(R.id.resultPreview)
        if (addTouchListener) previewView.setOnTouchListener { v, event ->
            if (v == previewView &&
                event.action == MotionEvent.ACTION_DOWN &&
                event.x.toInt() in 0 until v.width &&
                event.y.toInt() in 0 until v.height
            ) {
                val x = event.x
                val y = event.y
                // video is cropped -> crop this mathematically, too
                for (target in targets) {
                    target.rx = x / v.width - 0.5f // [-.5, +.5]
                    target.ry = y / v.height - 0.5f// [-.5, +.5]
                    waitForColorCallback?.invoke(target)
                }
                crosshair?.invalidate()
                queryColor = true
            }
            true
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider
            unbindCamera = {
                cameraProvider.unbindAll()
                previewView.visibility = View.GONE
                currColorView?.setBackgroundColor(0)
                hasBackgroundCamera = false
            }

            val analysis = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            val pixelEvents = PixelEvents()
            analysis.setAnalyzer(cameraExecutor) { img ->

                val image = YUVImage(
                    img.width, img.height,
                    img.planes[0], img.planes[1], img.planes[2]
                )

                val imageRotationDegrees = img.imageInfo.rotationDegrees

                updateImage(imageRotationDegrees, image)
                pixelEvents.update(imageRotationDegrees, image)

                val target = targets.first()
                // convert to rgb, and save it
                val color = target.currAverageSquare.toRGB()
                currColorView?.setBackgroundColor(color or BLACK)
                if (queryColor) {
                    // convert to rgb, and save it
                    target.color = color
                    targetColorView.setBackgroundColor(target.color or BLACK)
                    queryColor = false
                }

                if (waitForColorCallback?.invoke(target) == true) {
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

    var lastImageWidth = 0
    var lastImageHeight = 0
    var lastImageRotationDegrees = 0
    var lastImageData: IntArray = IntArray(0)

    private fun updateImage(imageRotationDegrees: Int, image: Image) {
        val requiredCapacity = image.width * image.height
        if (lastImageData.size < requiredCapacity) lastImageData = IntArray(requiredCapacity)
        image.fillRGB(lastImageData)
        lastImageWidth = image.width
        lastImageHeight = image.height
        lastImageRotationDegrees = imageRotationDegrees
    }

    fun MainActivity.allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

}