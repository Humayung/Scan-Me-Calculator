package com.example.scanmecalculator.model

import java.io.Serializable

class OcrResponse(
    var ParsedResults: List<ParsedResult> = emptyList()
) : Serializable

class ParsedResult(
    var TextOverlay: TextOverlay,
    var TextOrientation: String,
    var FileParseExitCode: Int,
    var ParsedText: String,
    var ErrorMessage: String,
    var ErrorDetails: String
) : Serializable

class TextOverlay(
    var Lines: List<Line> = emptyList(),
    var HasOverlay: Boolean,
    var Message: String
) : Serializable

class Line(
    var LineText: String,
    var Words: List<Word> = emptyList(),
    var MaxHeight: Double,
    var MinTop: Double
) : Serializable

class Word(
    var WordText: String,
    var Left: Double,
    var Top: Double,
    var Height: Double,
    var Width: Double
) : Serializable

//class OcrResponse(
//    var status: String = "",
//    var message: String = "",
//    var result: Result = Result(),
//) : Serializable
//
//class Result(
//    var url: String = "",
//    var preview_link: String = "",
//    var image_path: String = "",
//) : Serializable



