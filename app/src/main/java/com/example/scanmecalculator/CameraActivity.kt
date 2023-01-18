package com.example.scanmecalculator

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.coroutineScope
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.example.scanmecalculator.databinding.ActivityCameraBinding
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class CameraActivity : AppCompatActivity(), KoinComponent {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private val memoryDb: MemoryDb by inject()
    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        startCamera()
        binding.shutterBtn.setOnClickListener {
            takePicture()
        }
        initLoadingIndicator()
        Helper.transparentStatusBar(this)
        Helper.adjustSafeAreaPadding(view, binding.topBar)
    }




    private fun initLoadingIndicator() = with(binding) {
        val circularProgressDrawable = CircularProgressDrawable(this@CameraActivity)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 50f
        circularProgressDrawable.setColorSchemeColors(Color.BLACK)
        circularProgressDrawable.start()
        loadingIndicator.background = circularProgressDrawable
    }

    private fun attachTorchControl(camera: Camera) = with(binding) {
        if (!camera.cameraInfo.hasFlashUnit()) {
            torchBtn.visibility = View.GONE
        } else {
            torchBtn.setOnClickListener {
                val newState = camera.cameraInfo.torchState.value == 1
                camera.cameraControl.enableTorch(!newState)
            }
        }
    }

    private fun startCamera() = with(binding) {
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
                    this@CameraActivity, cameraSelector, preview, imageCapture
                )
                attachTorchControl(camera)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(applicationContext))
    }


    private fun setLoading(status: Boolean) = with(binding) {
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
                        memoryDb.imageUri.value = outputFile.absoluteFile.toUri()
                        setLoading(false)
                        finish()
                    }

                }
            }
        )
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