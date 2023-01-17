package com.example.scanmecalculator.networking

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.example.scanmecalculator.model.OcrResponse
import com.example.scanmecalculator.networking.Repository
import com.example.scanmecalculator.networking.Resource
import kotlinx.coroutines.Dispatchers
import okhttp3.MultipartBody
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ApiReq : KoinComponent {
    private val repository: Repository by inject()

    fun getOcr(image: MultipartBody.Part): LiveData<Resource<OcrResponse>> {
        return liveData(Dispatchers.IO) {
            emit(Resource.loading(null))
            emit(repository.getOcr(image))
        }
    }
}