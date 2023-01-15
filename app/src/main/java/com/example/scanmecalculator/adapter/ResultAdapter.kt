package com.example.scanmecalculator.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scanmecalculator.R
import com.example.scanmecalculator.model.ResultItem
import kotlinx.android.synthetic.main.item_result.view.*
import java.util.ArrayList

class ResultAdapter(private var list: ArrayList<ResultItem>) : RecyclerView.Adapter<ResultAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        Log.d("hey", list.toString())
        return Holder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_result,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun clear() {
        list.clear()
        notifyDataSetChanged()
    }

    fun getData(): ArrayList<ResultItem> {
        return list
    }

    fun replace(list: ArrayList<ResultItem>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = list[position]
        holder.itemView.textInput.text = item.input
        holder.itemView.textOutput.text = item.output
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView){}
}