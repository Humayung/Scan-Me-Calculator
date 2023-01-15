package com.example.scanmecalculator

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.MutableLiveData


class MemoryDb  {
    val imageUri: MutableLiveData<Uri> by lazy {
        MutableLiveData<Uri>()
    }

    val imageToProcess: MutableLiveData<Bitmap> by lazy {
        MutableLiveData<Bitmap>()
    }

    val brightness: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }

    val contrast: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>().apply { postValue(1f) }
    }

}