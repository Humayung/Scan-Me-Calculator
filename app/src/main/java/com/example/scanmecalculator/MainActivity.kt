package com.example.scanmecalculator

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.example.scanmecalculator.adapter.ResultAdapter
import com.example.scanmecalculator.model.ResultItem
import com.googlecode.tesseract.android.TessBaseAPI
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_preview.*

import kotlinx.coroutines.*
import mathjs.niltonvasques.com.mathjs.MathJS
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), KoinComponent, CoroutineScope{
    private val tesseract: TessBaseAPI by inject()
    private lateinit var resultAdapter: ResultAdapter
    private val resultItem = ArrayList<ResultItem>()
    private val memoryDb: MemoryDb by inject()
    private var fileChooserLauncher: ActivityResultLauncher<Intent>? = null
    private val math : MathJS by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        startKoin {
            modules(listOf(koinModules))
        }
        initTesseract()

        resultAdapter = ResultAdapter(resultItem)
        resultsRv.adapter = resultAdapter
        addInputBtn.setOnClickListener {
            getPermissionThenRun(
                Manifest.permission.CAMERA,
                onGranted = { startScanner() },
                onDenied = { disableScanner() })
        }

        loadResults()
        initLoadingDrawable()

        addInputFileBtn.setOnClickListener {
            showFileChooser()
        }

        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                Log.d(TAG, data?.data.toString())
                memoryDb.imageUri.value = data?.data
            }
        }

        memoryDb.imageToProcess.observe(this){
            processImage()
        }

        memoryDb.imageUri.observe(this){
            val intent = Intent(applicationContext, PreviewActivity::class.java)
            startActivity(intent)
        }


    }

    private val job = Job()

    private fun initTesseract() {
        val assetManager = applicationContext.assets

        val file = File(filesDir, "tessdata/eng.traineddata")
        if (!file.exists()) {
            val inputStream = assetManager.open("tessdata/eng.traineddata")
            file.parentFile?.mkdirs()
            file.writeBytes(inputStream.readBytes())
        }
        val pathToData = filesDir.absolutePath
        tesseract.init(pathToData, "eng")
        Log.d(TAG, "tesseract initialized")
        val files = fileList().joinToString("\n")
        Log.d(TAG, files)

        tesseract.setVariable("user_defined_dpi", "300")
//        tesseract.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890+-*/");
    }

    private fun initLoadingDrawable() {
        val circularProgressDrawable = CircularProgressDrawable(this)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.setColorSchemeColors(Color.WHITE, Color.WHITE, Color.WHITE)
        circularProgressDrawable.start()
        loadingIndicator.background = circularProgressDrawable
    }


    private fun processImage() {
        setLoading(true)
        val bmp = memoryDb.imageToProcess.value
        launch(Dispatchers.IO) {
            tesseract.setImage(bmp)
            val text = tesseract.utF8Text
            Log.d(TAG, text)
            val item = ResultItem()
            val expression = text.split("\n")[0]
            item.input = expression
            try {
                item.output = math.eval(expression)
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    resultItem.add(item)
                    loadResults()
                }
            } catch (e: java.lang.Exception){
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(applicationContext,"Cant read math expression!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(status: Boolean){

        if (status) {
            overlayBar.visibility = View.VISIBLE
        } else{
            overlayBar.visibility = View.GONE
        }
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            fileChooserLauncher?.launch(Intent.createChooser(intent, "Select a File to Upload"))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext, "Please install a File Manager.", Toast.LENGTH_SHORT
            ).show()
        }
    }




    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // cancel all coroutines when the activity is destroyed
    }

    private fun startScanner() {
        val intent = Intent(applicationContext, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun disableScanner() {
        addInputBtn.isEnabled = false
        errorPermissionTxt.visibility = View.VISIBLE
    }

    private fun getPermissionThenRun(
        permission: String, onGranted: () -> Unit, onDenied: () -> Unit
    ) {
        Dexter.withContext(applicationContext).withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    onGranted()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    onDenied()
                    Toast.makeText(
                        applicationContext,
                        "Enable Camera permission in the setting to use the app!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?, p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }

            }).check()
    }


    private fun loadResults() {
        resultItem.ifEmpty {
            resultsRv.visibility = View.GONE
            emptyResultText.visibility = View.VISIBLE
            return
        }
        resultsRv.visibility = View.VISIBLE
        emptyResultText.visibility = View.GONE
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


}

























//                                  5 * 4
//                                  10 * 2
//                                  90 * 23
//                                  5 * 4
//                                  5 * 4











