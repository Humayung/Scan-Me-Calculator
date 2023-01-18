package com.example.scanmecalculator

import android.content.Context
import com.example.scanmecalculator.persistence.Storage
import com.googlecode.tesseract.android.TessBaseAPI
import mathjs.niltonvasques.com.mathjs.MathJS
import org.koin.core.module.Module
import org.koin.dsl.module


fun koinModules(context: Context): Module {
    return module {
        single { MemoryDb() }
        single { MathJS() }
        single { Storage(context) }
        single {TessBaseAPI()}

    }
}


