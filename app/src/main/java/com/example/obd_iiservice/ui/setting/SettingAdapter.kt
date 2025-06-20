package com.example.obd_iiservice.ui.setting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.databinding.ItemSettingBinding

class SettingAdapter (
    private val list : List<SettingItem>,
    private val onItemClick: (SettingItem) -> Unit
) : RecyclerView.Adapter<SettingAdapter.ListViewHolder>() {
    class ListViewHolder(var binding: ItemSettingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewHolder {
        val binding = ItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val items = list[position]
        holder.binding.apply {
            ivIconSetting.setImageResource(items.icon)
            tvSettingLabel.text = items.label
            tvSettingDescription.text = items.description
        }
        holder.itemView.setOnClickListener {
            onItemClick(items)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}