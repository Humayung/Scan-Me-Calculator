package com.example.scanmecalculator.persistence

import android.content.Context
import android.content.SharedPreferences
import com.example.scanmecalculator.model.ResultItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class NonEncryptedDatabase(context: Context) : StorageInterface {

    private val preferences: SharedPreferences =
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    override fun addResult(item : ResultItem){
        val savedResults = getResultList()
        savedResults.add(item)
        preferences.edit().putString("resultList", Gson().toJson(savedResults)).apply()
    }

    override fun getResultList(): ArrayList<ResultItem>{
        val string = preferences.getString("resultList", "").toString()
        string.ifEmpty { return arrayListOf() }
        val listType: Type = object : TypeToken<ArrayList<ResultItem?>?>() {}.type
        return Gson().fromJson(string, listType)
    }

}