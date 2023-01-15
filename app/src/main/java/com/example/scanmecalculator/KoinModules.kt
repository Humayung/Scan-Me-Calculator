package com.example.scanmecalculator

import com.googlecode.tesseract.android.TessBaseAPI
import mathjs.niltonvasques.com.mathjs.MathJS
import org.koin.dsl.module

val koinModules = module {
    single { TessBaseAPI() }
    single { MemoryDb() }
    single { MathJS() }

}
