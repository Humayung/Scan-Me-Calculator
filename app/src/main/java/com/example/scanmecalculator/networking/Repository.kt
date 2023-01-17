package com.example.scanmecalculator.networking


import com.example.scanmecalculator.API_KEY
import com.example.scanmecalculator.model.OcrResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.component.KoinComponent


class Repository(private val api: Api, private val responseHandler: ResponseHandler) :
    KoinComponent {
    suspend fun getOcr(image: MultipartBody.Part): Resource<OcrResponse> {
        return try {
            val bar = api.getOCR(image = image, apikey = API_KEY)
            responseHandler.handleResponse(bar)
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }
}

//package com.example.scanmecalculator.networking
//
//import com.example.scanmecalculator.API_KEY
//import com.example.scanmecalculator.model.OcrResponse
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.koin.core.component.KoinComponent
//
//
//class Repository(private val api: Api, private val responseHandler: ResponseHandler) :
//    KoinComponent {
//
//    suspend fun getOcr(image: MultipartBody.Part): Resource<OcrResponse> {
//        return try {
//            val apiKey = API_KEY
//            val conversionType = "pdfocr3".toRequestBody("text/plain".toMediaTypeOrNull())
//            val bar = api.getOCR(image, conversionType, apiKey)
//            responseHandler.handleResponse(bar)
//        } catch (e: Exception) {
//            responseHandler.handleException(e)
//        }
//    }
//}