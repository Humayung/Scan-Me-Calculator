package com.example.scanmecalculator

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.coroutineScope
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream

class CameraActivity : AppCompatActivity(), KoinComponent {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var imageCapture: ImageCapture

    private val memoryDb: MemoryDb by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        startCamera()
        shutterBtn.setOnClickListener {
            takePicture()
        }
        initLoadingIndicator()
    }


    private fun initLoadingIndicator() {
        val circularProgressDrawable = CircularProgressDrawable(applicationContext)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 100f
        circularProgressDrawable.setColorSchemeColors(Color.WHITE, Color.WHITE, Color.WHITE)
        circularProgressDrawable.start()
        loadingIndicator.background = circularProgressDrawable
    }

    private fun attachTorchControl(camera: Camera) {
        if (!camera.cameraInfo.hasFlashUnit()) {
            torchBtn.visibility = View.GONE
        } else {
            torchBtn.setOnClickListener {
                val newState = camera.cameraInfo.torchState.value == 1
                camera.cameraControl.enableTorch(!newState)
            }
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = buildImagePreviewUseCase()
                .also {
                    it.setSurfaceProvider(livePreview.surfaceProvider)
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCapture = buildImageCaptureUseCase()
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                attachTorchControl(camera)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(applicationContext))
    }


    private fun setLoading(status: Boolean) {
        if (status) {
            overlayBar.visibility = View.VISIBLE
            shutterBtn.isEnabled = false
            torchBtn.isEnabled = false
        } else {
            overlayBar.visibility = View.GONE
            shutterBtn.isEnabled = true
            torchBtn.isEnabled = true
        }
    }


    private fun takePicture() {
        // Get a stable reference of the modifiable image capture use case
        setLoading(true)
        val outputFile = File(applicationContext?.filesDir, "image.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()


        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    setLoading(false)
                    val msg = "Photo capture failed"
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(TAG, msg)
                    lifecycle.coroutineScope.launch {
                        rotateImageCorrectly(outputFile.absoluteFile)
                        memoryDb.imageUri.value = outputFile.absoluteFile.toUri()
                        setLoading(false)
                        finish()
                    }

                }
            }
        )
    }

    suspend fun rotateImageCorrectly(photoFile: File) = withContext(Dispatchers.IO) {
        val sourceBitmap =
            MediaStore.Images.Media.getBitmap(
                applicationContext?.contentResolver,
                photoFile.toUri()
            )

        val exif = ExifInterface(photoFile.inputStream())
        val rotation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationInDegrees = when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            ExifInterface.ORIENTATION_TRANSVERSE -> -90
            ExifInterface.ORIENTATION_TRANSPOSE -> -270
            else -> 0
        }
        val matrix = Matrix().apply {
            if (rotation != 0) preRotate(rotationInDegrees.toFloat())
        }

        val rotatedBitmap = Bitmap.createBitmap(
            sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true
        )

        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(photoFile))

        sourceBitmap.recycle()
        rotatedBitmap.recycle()
    }

    private fun buildImagePreviewUseCase(): Preview {
        return Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
    }

    private fun buildImageCaptureUseCase(): ImageCapture {
        return ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

}