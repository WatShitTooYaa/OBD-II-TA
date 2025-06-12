package com.example.obd_iiservice.obd

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.ItemLayoutObdBinding
import com.example.obd_iiservice.databinding.ItemObdDataBinding

class OBDAdapter(
    private val list: MutableList<OBDItem>,
    private val itemHeight: Int
) : RecyclerView.Adapter<OBDAdapter.ListViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewHolder {
        val binding = ItemLayoutObdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.layoutParams.height = itemHeight
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ListViewHolder,
        position: Int
    ) {
        val items = list[position]
        holder.binding.apply {
            tvItemObdUnit.text = items.unit
            tvItemObdType.text = items.label
            tvItemObdValue.text = items.value
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ListViewHolder(var binding: ItemLayoutObdBinding) : RecyclerView.ViewHolder(binding.root)
    // Fungsi untuk mengupdate data di adapter
    fun updateData(newList: List<OBDItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged() // Beritahu RecyclerView untuk menggambar ulang semua item
    }

}