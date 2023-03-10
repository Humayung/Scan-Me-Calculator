package com.example.scanmecalculator

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.example.scanmecalculator.Helper.adjustSafeAreaPaddingBottom
import com.example.scanmecalculator.Helper.adjustSafeAreaPaddingTop
import com.example.scanmecalculator.Helper.transparentStatusBar
import com.example.scanmecalculator.adapter.ResultAdapter
import com.example.scanmecalculator.databinding.ActivityMainBinding
import com.example.scanmecalculator.model.ResultItem
import com.example.scanmecalculator.persistence.Storage
import com.example.scanmecalculator.persistence.StorageType
import com.googlecode.tesseract.android.TessBaseAPI
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.*
import mathjs.niltonvasques.com.mathjs.MathJS
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), KoinComponent, CoroutineScope {
    private lateinit var resultAdapter: ResultAdapter
    private var resultItem = ArrayList<ResultItem>()
    private val memoryDb: MemoryDb by inject()
    private var fileChooserLauncher: ActivityResultLauncher<Intent>? = null
    private val math: MathJS by inject()
    private val storage: Storage by inject()
    private val tesseract: TessBaseAPI by inject()

    data class EvalResult(var success: Boolean = false, var result: String = "")

    private lateinit var binding: ActivityMainBinding
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        stopKoin()
        startKoin {
            modules(listOf(koinModules(applicationContext)))
        }


        resultAdapter = ResultAdapter(resultItem)
        binding.resultsRv.adapter = resultAdapter
        binding.addInputBtn.setOnClickListener {
            getPermissionThenRun(
                android.Manifest.permission.CAMERA,
                onGranted = { startCamera() },
                onDenied = { disableCamera() })
        }

        initLoadingIndicator()
        initTesseract()

        binding.storageSelector.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.useDatabaseStorage -> {
                    memoryDb.selectedStorage.value =
                        StorageType.NON_ENCRYPTED_DATABASE
                    binding.storageLabel.text =
                        getString(R.string.storage_used_non_encrypted_shared_preferences)
                }
                R.id.useFileStorage -> {
                    memoryDb.selectedStorage.value = StorageType.ENCRYPTED_FILE
                    val filePath = File(filesDir, ENCRYPTED_FILE_NAME).absoluteFile
                    binding.storageLabel.text =
                        getString(R.string.storage_used_encrypted_file, filePath)
                }
            }
        }



        fileChooserLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data: Intent? = result.data
                    Log.d(TAG, data?.data.toString())
                    memoryDb.imageUri.value = data?.data
                }
            }

        memoryDb.imageToProcess.observe(this@MainActivity) {
            Log.d(TAG, "image is being processed")
            processImage()
        }

        memoryDb.imageUri.observe(this@MainActivity) {
            val intent = Intent(applicationContext, PreviewActivity::class.java)
            startActivity(intent)
        }

        memoryDb.selectedStorage.observe(this@MainActivity) {
            storage.switchStorage(it)
            loadResults()
        }

        initLoadingIndicator()

        transparentStatusBar(this)
//        adjustSafeAreaPaddingBottom(binding.coordinatorLayout, binding.storageSelector)
        adjustSafeAreaPaddingTop(binding.coordinatorLayout, binding.resultsRv)
    }



    private val job = Job()


    @SuppressLint("SetTextI18n")
    private fun initLoadingIndicator() = with(binding) {
        val circularProgressDrawable = CircularProgressDrawable(this@MainActivity)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 50f
        circularProgressDrawable.setColorSchemeColors(Color.BLACK)
        circularProgressDrawable.start()
        loadingIndicator.background = circularProgressDrawable
    }

    private fun processImage() = with(binding) {
        initLoadingIndicator()
        setLoading(true)
        val photoFile = memoryDb.imageToProcess.value
        var text = ""
        launch(Dispatchers.IO) {
            tesseract.setImage(photoFile)
            text = tesseract.utF8Text
            val firstExpression = text.split("\n")[0]
            Log.d(TAG, text)
            consumeExpression(firstExpression)
            withContext(Dispatchers.Main) {
                loadResults()
                setLoading(false)
            }
        }
    }


    private suspend fun consumeExpression(expression: String) {
        val evalResult = checkAndEvalExpression(expression)
        if (evalResult.success) {
            val item = ResultItem()
            item.input = expression
            item.output = math.eval(expression)
            resultItem.add(item)
            storage.addResult(item)
            Log.d(TAG, "result" + item.output)
            return
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(
                this@MainActivity,
                evalResult.result,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun checkAndEvalExpression(expression: String): EvalResult {
        val result = EvalResult()
        val simpleExpression = onlySimpleMathExpression(expression)
        if (simpleExpression) {
            return try {
                result.success = true
                result.result = math.eval(expression)
                result
            } catch (e: java.lang.Exception) {
                result.success = false
                result.result = "\"$expression\" is not a math expression!"
                result
            }
        }
        result.success = false
        result.result = "$expression is not a simple 2 argument operation!"
        return result
    }

    private fun onlySimpleMathExpression(expression: String): Boolean {
        val pattern = """^\s*\d+\s*[+\-*/]\s*\d+\s*$"""
        val regex = Regex(pattern)
        return expression.matches(regex)
    }

    private fun setLoading(status: Boolean) = with(binding) {
        if (status) {
            addInputBtn.isEnabled = false
            resultsRv.isEnabled = false
            useDatabaseStorage.isEnabled = false
            useFileStorage.isEnabled = false
            overlayBar.visibility = View.VISIBLE
        } else {
            addInputBtn.isEnabled = true
            resultsRv.isEnabled = true
            parent.isEnabled = true
            useDatabaseStorage.isEnabled = true
            useFileStorage.isEnabled = true
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

    private fun startCamera() {
        val intent = Intent(applicationContext, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun disableCamera() = with(binding) {
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

    }


    @SuppressLint("NotifyDataSetChanged")
    private fun loadResults() = with(binding) {
        val savedResult = storage.getResultList()
        Log.d(TAG, "SIZE " + savedResult.size)
        resultItem.clear()
        resultItem.addAll(savedResult)
        resultItem.ifEmpty {
            resultsRv.visibility = View.GONE
            emptyResultText.visibility = View.VISIBLE
            return
        }
        resultAdapter.notifyDataSetChanged()
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











