package com.example.scanmecalculator

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class PreviewActivity : AppCompatActivity(), KoinComponent {

    private val memoryDb: MemoryDb by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        brightnessSlider.addOnChangeListener { _, value, _ ->
            memoryDb.brightness.value = value
        }
        contrastSlider.addOnChangeListener { _, value, _ ->
            memoryDb.contrast.value = value
        }
        memoryDb.contrast.observe(this) {
            setContrastAndBrightness()
        }
        memoryDb.brightness.observe(this) {
            setContrastAndBrightness()
        }
        processButton.setOnClickListener {
            finaliseImage()
        }
        deleteBtn.setOnClickListener {
            deleteImage()
        }

        rotateBtn.setOnClickListener {
            rotateImage()
        }

        memoryDb.imageUri.observe(this) {
            setPreview(it)
        }
        imagePreview.isDrawingCacheEnabled = true;
    }


    private fun rotateImage() {
//        val bmp = imagePreview.drawingCache
//        val matrix = Matrix()
//        matrix.postRotate(90f)
//        val scaledBitmap = Bitmap.createScaledBitmap(
//            bmp,
//            bmp.width,
//            bmp.height,
//            true
//        ) //BitmapOrg- is origanl bitmap
//        val rotatedBitmap = Bitmap.createBitmap(
//            scaledBitmap,
//            0,
//            0,
//            scaledBitmap.width,
//            scaledBitmap.height,
//            matrix,
//            true
//        )
//        imagePreview.setImageBitmap(rotatedBitmap)
    }

    private fun setPreview(imageUri: Uri) {
        lifecycle.coroutineScope.launch {
            contrastSlider.value = 1f
            brightnessSlider.value = 0f
            withContext(Dispatchers.Main) {
                val bmp = prepareBmpFromImagePath(imageUri)

                imagePreview.setImageBitmap(bmp)
            }
        }

    }

    private fun deleteImage() {
        imagePreview.setImageBitmap(null)
        finish()
    }


    private fun finaliseImage() {
        val bmp = imagePreview.drawingCache
        memoryDb.imageToProcess.value = bmp
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
        bmp = turnIntoGrayscale(bmp!!)
        return bmp
    }

    private fun turnIntoGrayscale(original: Bitmap): Bitmap {
        val grayBitmap = Bitmap.createScaledBitmap(
            original,
            original.width,
            original.height,
            true
        )
        grayBitmap.config = Bitmap.Config.ARGB_8888

        for (i in 0 until grayBitmap.width) {
            for (j in 0 until grayBitmap.height) {
                val pixel = grayBitmap.getPixel(i, j)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                val gray = (red + green + blue) / 3
                grayBitmap.setPixel(i, j, Color.rgb(gray, gray, gray))
            }
        }
        return grayBitmap
    }

    private fun setContrastAndBrightness() {

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
}