package com.example.scanmecalculator

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

    val selectedStorage: MutableLiveData<StorageType> by lazy {
        MutableLiveData<StorageType>().apply { postValue(StorageType.NON_ENCRYPTED_DATABASE) }
    }

}