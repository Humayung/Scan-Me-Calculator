package com.example.scanmecalculator.persistence

import android.content.Context
import com.example.scanmecalculator.ENCRYPTED_FILE_NAME
import com.example.scanmecalculator.model.ResultItem
import com.example.scanmecalculator.utils.AESUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type

class EncryptedFile(context: Context) : StorageInterface {

    private var context : Context? = null
    init {
        this.context= context
    }

    override fun addResult(item : ResultItem){
        val savedResults = getResultList()
        savedResults.add(item)
        val file = File(context?.filesDir, ENCRYPTED_FILE_NAME)

        val gson = Gson()
        val jsonString = gson.toJson(savedResults)
        AESUtils.saveArrayToEncryptedFile(jsonString.toByteArray(), file)
    }

    override fun getResultList(): ArrayList<ResultItem> {
        val file = File(context?.filesDir, ENCRYPTED_FILE_NAME)
        val decrypted =  String(AESUtils.readArrayFromEncryptedFile(file))
        decrypted.ifEmpty { return arrayListOf() }
        val gson = Gson()
        val listType: Type = object : TypeToken<ArrayList<ResultItem?>?>() {}.type
        return gson.fromJson(decrypted, listType)
    }


}