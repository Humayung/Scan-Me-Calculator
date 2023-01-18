package com.example.scanmecalculator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.example.scanmecalculator.databinding.ActivityPreviewBinding
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext


class PreviewActivity : AppCompatActivity(), KoinComponent, CoroutineScope {

    private val memoryDb: MemoryDb by inject()
    private lateinit var binding: ActivityPreviewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityPreviewBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        binding.brightnessSlider.addOnChangeListener { _, value, _ ->
            memoryDb.brightness.value = value
        }
        binding.contrastSlider.addOnChangeListener { _, value, _ ->
            memoryDb.contrast.value = value
        }
        memoryDb.contrast.observe(this@PreviewActivity) {
            setContrastAndBrightness()
        }
        memoryDb.brightness.observe(this@PreviewActivity) {
            setContrastAndBrightness()
        }
        binding.processButton.setOnClickListener {
            finaliseImage()
        }
        binding.deleteBtn.setOnClickListener {
            deleteImage()
        }
        memoryDb.imageUri.observe(this@PreviewActivity) {
            setPreview(it)
        }
        binding.imagePreview.isDrawingCacheEnabled = true;
        OpenCVLoader.initDebug();
    }


    fun preProcessImg(bitmap: Bitmap) {
        Log.d(TAG, "preprocessing")
        var tempMat = Mat()
        var source = Mat()
        Utils.bitmapToMat(bitmap, tempMat)
        Imgproc.cvtColor(tempMat, source, Imgproc.COLOR_BGR2GRAY)
        var result = Mat()
//        Imgproc.threshold(source, result, 150.0, 255.0, Imgproc.THRESH_BINARY_INV)
        Imgproc.adaptiveThreshold(
            source,
            result,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,
            101,
            40.0
        )
        val element = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(2.0, 2.0)
        )
        Imgproc.erode(result, result, element)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(result, result, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.dilate(result, result, Mat(), Point(-1.0, -1.0))
        Utils.matToBitmap(result, bitmap)
    }

    private fun setPreview(imageUri: Uri) = with(binding) {
        lifecycle.coroutineScope.launch {
            contrastSlider.value = 1f
            brightnessSlider.value = 0f
            withContext(Dispatchers.Main) {
                val bmp = prepareBmpFromImagePath(imageUri)

                imagePreview.setImageBitmap(bmp)
            }
        }
    }

    private fun deleteImage() = with(binding) {
        imagePreview.setImageBitmap(null)
        finish()
    }

    private fun finaliseImage() = with(binding) {
        val bmp = imagePreview.drawingCache
        val photoFile = File(filesDir, "post-processed.jpeg")
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(photoFile))
        memoryDb.imageToProcess.value = photoFile
        finish()
    }

    private fun scaleDownBmp(bmp: Bitmap): Bitmap? {
        val originalWidth = bmp.width
        val originalHeight = bmp.height
        val desiredWidth = (originalWidth * 0.5).toInt()
        val desiredHeight = (originalHeight * 0.5).toInt()
        return Bitmap.createScaledBitmap(bmp, desiredWidth, desiredHeight, true)
    }


    private fun prepareBmpFromImagePath(imageUri: Uri): Bitmap? {
        val iStream = applicationContext.contentResolver?.openInputStream(imageUri)

        val options = BitmapFactory.Options()

        var bmp = BitmapFactory.decodeStream(iStream, null, options)
        iStream?.close()
        bmp = scaleDownBmp(bmp!!)
        preProcessImg(bmp!!)

        return bmp
    }


    private fun setContrastAndBrightness() = with(binding) {
        val contrast = memoryDb.contrast.value ?: 1f
        val brightness = memoryDb.brightness.value ?: 0f

        val colorMatrix = ColorMatrix()
        colorMatrix.set(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val filter = ColorMatrixColorFilter(colorMatrix)

        imagePreview.colorFilter = filter

    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}