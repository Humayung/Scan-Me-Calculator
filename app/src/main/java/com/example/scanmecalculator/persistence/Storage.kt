package com.example.scanmecalculator.persistence

import android.content.Context
import android.util.Log
import com.example.scanmecalculator.TAG
import com.example.scanmecalculator.model.ResultItem

class Storage(context: Context) : StorageInterface{
    private var currentStorage : StorageInterface
    private var storage1 : NonEncryptedDatabase
    private var storage2 : EncryptedFile


    init {
        storage1 = NonEncryptedDatabase(context)
        storage2 = EncryptedFile(context)
        currentStorage = storage1
    }

    override fun addResult(item : ResultItem) {
        return currentStorage.addResult(item)
    }

    override fun getResultList(): ArrayList<ResultItem> {
        return currentStorage.getResultList()
    }

    fun switchStorage(type: StorageType){
        currentStorage = when(type) {
            StorageType.NON_ENCRYPTED_DATABASE -> {
                storage1
            }
            StorageType.ENCRYPTED_FILE -> {
                storage2
            }
        }
        Log.d(TAG, "SELECTED " + currentStorage.javaClass.name)
    }

}