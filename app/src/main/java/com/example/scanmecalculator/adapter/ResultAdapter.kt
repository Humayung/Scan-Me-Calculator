package com.example.scanmecalculator.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scanmecalculator.databinding.ItemResultBinding
import com.example.scanmecalculator.model.ResultItem
import java.util.ArrayList

class ResultAdapter(private var list: ArrayList<ResultItem>) : RecyclerView.Adapter<ResultAdapter.Holder>() {

    lateinit var binding : ItemResultBinding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        Log.d("hey", list.toString())

        binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(
            binding
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
        holder.bind(item)
    }

     class Holder(private val itemBinding: ItemResultBinding) : RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: ResultItem) {
            itemBinding.textInput.text = item.input
            itemBinding.textOutput.text = item.output

        }
    }
}