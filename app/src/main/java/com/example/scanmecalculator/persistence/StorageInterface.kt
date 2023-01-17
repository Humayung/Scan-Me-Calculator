package com.example.scanmecalculator.persistence

import com.example.scanmecalculator.model.ResultItem

interface StorageInterface {
    fun getResultList(): ArrayList<ResultItem>
    fun addResult(item : ResultItem)
}