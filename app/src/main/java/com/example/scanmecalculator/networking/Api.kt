package com.example.scanmecalculator.networking

import com.example.scanmecalculator.model.OcrResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
interface Api {

    @Multipart
    @POST("parse/image")
    suspend fun getOCR(
        @Part image: MultipartBody.Part,
        @Part("url") url: RequestBody = "".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("language") language: RequestBody = "eng".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("isOverlayRequired") isOverlayRequired: RequestBody = "true".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("FileType") FileType: RequestBody = ".Auto".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("IsCreateSearchablePDF") IsCreateSearchablePDF: RequestBody = "false".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("isSearchablePdfHideTextLayer") isSearchablePdfHideTextLayer: RequestBody = "true".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("detectOrientation") detectOrientation: RequestBody = "false".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("isTable") isTable: RequestBody =  "false".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("scale") scale: RequestBody = "true".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Part("OCREngine") OCREngine: RequestBody = "2".toRequestBody("text/plain".toMediaTypeOrNull()),
        @Header("apikey") apikey: String
        ): OcrResponse
}

//interface Api {
//
//    @Multipart
//    @POST("/textract/csvconvert")
//    suspend fun getOCR(
//        @Part image: MultipartBody.Part,
//        @Part("conversion_type") conversionType: RequestBody,
//        @Header("authorization") apikey: String
//    ): OcrResponse
//}
