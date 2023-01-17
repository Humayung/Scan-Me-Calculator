package com.example.scanmecalculator

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.example.scanmecalculator.persistence.StorageType
import java.io.File


class MemoryDb  {
    val imageUri: MutableLiveData<Uri> by lazy {
        MutableLiveData<Uri>()
    }

    val imageToProcess: MutableLiveData<File> by lazy {
        MutableLiveData<File>()
    }

    val brightness: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }

    val contrast: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>().apply { postValue(1f) }
    }

    val selectedStorage: MutableLiveData<StorageType> by lazy {
        MutableLiveData<StorageType>().apply { postValue(StorageType.NON_ENCRYPTED_DATABASE) }
    }

}