package com.example.scanmecalculator

import android.Manifest
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
import com.example.scanmecalculator.model.OcrResponse
import com.example.scanmecalculator.model.ResultItem
import com.example.scanmecalculator.networking.ApiReq
import com.example.scanmecalculator.networking.Resource
import com.example.scanmecalculator.networking.Status
import com.example.scanmecalculator.persistence.Storage
import com.example.scanmecalculator.persistence.StorageType
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.*
import mathjs.niltonvasques.com.mathjs.MathJS
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), KoinComponent, CoroutineScope {
    private lateinit var resultAdapter: ResultAdapter
    private var resultItem = ArrayList<ResultItem>()
    private val memoryDb: MemoryDb by inject()
    private var fileChooserLauncher: ActivityResultLauncher<Intent>? = null
    private val math: MathJS by inject()
    private val apiReq: ApiReq by inject()
    private val storage: Storage by inject()

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
            if (BuildConfig.FLAVOR_input == "fileSystem") {
                showFileChooser()
            }
            if (BuildConfig.FLAVOR_input == "builtInCamera") {
                getPermissionThenRun(Manifest.permission.CAMERA,
                    onGranted = { startCamera() },
                    onDenied = { disableCamera() })
            }
        }

        initLoading()

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
                    binding.storageLabel.text = getString(R.string.storage_used_encrypted_file)
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
        Toast.makeText(this, com.example.scanmecalculator.BuildConfig.FLAVOR, Toast.LENGTH_LONG)
            .show()


    }

    private val job = Job()


    @SuppressLint("SetTextI18n")
    private fun initLoading() = with(binding) {
        textLoading.text = "Requesting $BASE_URL"
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
        val image = MultipartBody.Part.createFormData(
            "image", "my-image.png", photoFile!!.asRequestBody("image/png".toMediaType())
        )
        successOcr(null)

        // TODO: TEST DUMMY
        apiReq.getOcr(image).observe(this@MainActivity) {
            if (it.status == Status.SUCCESS) {
                successOcr(it)
            } else {
                failedOcr(it)
            }

        }
    }

    private fun failedOcr(response: Resource<OcrResponse>) {
        when (response.status) {
            Status.LOADING -> {
                setLoading(true)
            }
            Status.ERROR -> {
                Log.d(TAG, "FAILED " + response.message)
                Toast.makeText(
                    applicationContext,
                    "Failed " + response.status + " " + response.message,
                    Toast.LENGTH_LONG
                ).show()
                setLoading(false)
            }
            else -> {
                setLoading(false)
            }
        }


    }

    private fun successOcr(response: Resource<OcrResponse>?) {

        val text = response?.response!!.ParsedResults[0].ParsedText
        val firstExpression = text.split("\n")[0]

        // TODO: TEST DUMMYTEST DUMMYTEST DUMMYTEST DUMMYTEST DUMMYTEST DUMMYTEST DUMMYTEST DUMMYTEST DUMMY
//        val text = "3+4\n5+2"
//        val firstExpression = "3+4"

        Log.d(TAG, "BODY $text")
        consumeExpression(firstExpression)
        setLoading(false)
    }

    private fun consumeExpression(expression: String) {
        try {
            if (onlySimpleMathExpression(expression)) {
                val item = ResultItem("input1", "output1")
                item.input = expression
                item.output = math.eval(expression)
                resultItem.add(item)
                storage.addResult(item)
                loadResults()
                Log.d(TAG, "result" + item.output)
            } else {
                Toast.makeText(
                    applicationContext,
                    "$expression is not a simple 2 argument operation!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(
                applicationContext,
                "\"$expression\" is not a math expression!",
                Toast.LENGTH_SHORT
            ).show()
        }
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


    private fun loadResults() = with(binding) {
        val savedResult = storage.getResultList()
        Log.d(TAG, "SIZE " + savedResult.size)
        resultItem.clear()
        resultItem.addAll(savedResult)
        resultItem.ifEmpty {
            resultsRv.visibility = View.GONE
            emptyResultText.visibility = View.VISIBLE

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











