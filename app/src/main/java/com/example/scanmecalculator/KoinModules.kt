package com.example.scanmecalculator

import android.content.Context
import com.example.scanmecalculator.networking.ApiReq
import com.example.scanmecalculator.networking.*
import com.example.scanmecalculator.persistence.Storage
import mathjs.niltonvasques.com.mathjs.MathJS
import org.koin.core.module.Module
import org.koin.dsl.module


fun koinModules(context: Context): Module {
    return module {
        single { MemoryDb() }
        single { MathJS() }
        single { Repository(get(), get()) }
        single { ApiReq() }
        factory { provideOkHttpClient(get()) }
        factory { provideForecastApi(get()) }
        factory { provideLoggingInterceptor() }
        single { provideRetrofit(get()) }
        factory { ResponseHandler() }
        single { Storage(context) }

    }
}


