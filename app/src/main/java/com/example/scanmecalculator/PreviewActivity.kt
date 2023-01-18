package com.example.scanmecalculator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        binding.processButton.setOnClickListener {
            finaliseImage()
        }
        binding.deleteBtn.setOnClickListener {
            deleteImage()
        }
        memoryDb.imageUri.observe(this@PreviewActivity) {
            setPreview(it)
        }
        binding.rotateBtn.setOnClickListener {
            rotatePreview()
        }
        OpenCVLoader.initDebug();

        Helper.transparentStatusBar(this)
        Helper.adjustSafeAreaPaddingTop(binding.coordinatorLayout, binding.tools)
//        Helper.adjustSafeAreaPaddingBottom(binding.coordinatorLayout, binding.processButton)
    }


    private fun preProcessImg(bitmap: Bitmap): Bitmap? {
        Log.d(TAG, "preprocessing")
        val tempMat = Mat()
        val source = Mat()
        Utils.bitmapToMat(bitmap, tempMat)
        Imgproc.cvtColor(tempMat, source, Imgproc.COLOR_BGR2GRAY)
        val result = Mat()
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

        val w: Int = result.width()
        val h: Int = result.height()

        val conf = Bitmap.Config.ARGB_8888 // see other conf types

        val bmp = Bitmap.createBitmap(w, h, conf) // this creates a MUTABLE bitmap

        Utils.matToBitmap(result, bmp)
        return bmp
    }

    private fun setPreview(imageUri: Uri) = with(binding) {
        try {
            launch(Dispatchers.IO) {
                val bmp = prepareBmpFromImagePath(imageUri)
                withContext(Dispatchers.Main) {
                    imagePreview.setImageBitmap(bmp)
                }
            }
        } catch (e: java.lang.Exception) {
            Toast.makeText(this@PreviewActivity, "An error occurred!", Toast.LENGTH_LONG)
                .show()
        }

    }

    private fun deleteImage() = with(binding) {
        imagePreview.setImageBitmap(null)
        finish()
    }

    private fun finaliseImage() = with(binding) {
        val bmp = (imagePreview.drawable as BitmapDrawable).bitmap
        val photoFile = File(filesDir, "post-processed.jpeg")
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(photoFile))
        memoryDb.imageToProcess.value = photoFile
        finish()
    }

    private fun scaleDownBmp(bmp: Bitmap): Bitmap {
        val originalWidth = bmp.width
        val originalHeight = bmp.height
        val desiredWidth = (originalWidth * 0.8).toInt()
        val desiredHeight = (originalHeight * 0.8).toInt()
        return Bitmap.createScaledBitmap(bmp, desiredWidth, desiredHeight, true)
    }


    private fun prepareBmpFromImagePath(imageUri: Uri): Bitmap {
        val iStream = applicationContext.contentResolver?.openInputStream(imageUri)
        val options = BitmapFactory.Options()
        var bmp = BitmapFactory.decodeStream(iStream, null, options)
        iStream?.close()
        bmp = scaleDownBmp(bmp!!)
        bmp = preProcessImg(bmp)
        return bmp!!
    }

    private fun rotatePreview() = with(binding) {
        val bitmap = (imagePreview.drawable as BitmapDrawable).bitmap
        val rotation = -90f
        val matrix = Matrix().apply {
            if (rotation != 0f) preRotate(rotation)
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        imagePreview.setImageBitmap(rotatedBitmap)
    }


    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}