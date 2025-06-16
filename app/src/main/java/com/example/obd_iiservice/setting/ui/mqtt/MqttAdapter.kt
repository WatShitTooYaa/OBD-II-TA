package com.example.obd_iiservice.setting.ui.mqtt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.databinding.ItemMqttBinding
import com.example.obd_iiservice.databinding.ItemSettingBinding
import com.example.obd_iiservice.ui.setting.SettingAdapter
import com.example.obd_iiservice.ui.setting.SettingItem

class MqttAdapter(
    private val list : List<MqttItem>,
    private val onItemClick: (MqttItem) -> Unit
) : RecyclerView.Adapter<MqttAdapter.ListViewHolder>() {
    class ListViewHolder(var binding: ItemMqttBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewHolder {
        val binding = ItemMqttBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val items = list[position]
        holder.binding.apply {
//            ivIconSetting.setImageResource(items.icon)
            tvSettingLabel.text = items.title
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