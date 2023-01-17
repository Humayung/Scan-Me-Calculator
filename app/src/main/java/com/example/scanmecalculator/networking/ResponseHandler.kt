package com.example.scanmecalculator.networking

import android.util.Log
import org.koin.core.component.KoinComponent

open class ResponseHandler : KoinComponent {

    fun <T : Any> handleResponse(data: T): Resource<T> {
        return Resource.success(data)
    }

    fun <T : Any> handleException(e: Exception): Resource<T> {
        e.printStackTrace()
        Log.d("exception", "asaa" + e.message)
        return Resource.error(e.message, null)
    }
}

class CustomException(message: String?) : java.lang.Exception(message)
