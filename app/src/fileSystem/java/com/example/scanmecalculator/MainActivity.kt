package com.example.scanmecalculator

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.example.scanmecalculator.adapter.ResultAdapter
import com.example.scanmecalculator.databinding.ActivityMainBinding
import com.example.scanmecalculator.model.ResultItem
import com.example.scanmecalculator.persistence.Storage
import com.example.scanmecalculator.persistence.StorageType
import com.googlecode.tesseract.android.TessBaseAPI
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
            showFileChooser()
        }

        initLoading()
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


    }

    private val job = Job()


    @SuppressLint("SetTextI18n")
    private fun initLoading() = with(binding) {
        textLoading.text = "Feeding tesseract"
        val circularProgressDrawable = CircularProgressDrawable(applicationContext)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.setColorSchemeColors(Color.WHITE, Color.WHITE, Color.WHITE)
        circularProgressDrawable.start()
        loadingIndicator.background = circularProgressDrawable
    }


    private fun processImage() = with(binding) {
        initLoading()
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
            overlayBar.visibility = View.VISIBLE
        } else {
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











