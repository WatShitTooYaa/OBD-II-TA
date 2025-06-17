package com.example.obd_iiservice.setting.ui.mqtt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.databinding.ItemMqttBinding
import com.example.obd_iiservice.databinding.ItemSettingBinding
import com.example.obd_iiservice.ui.setting.SettingAdapter
import com.example.obd_iiservice.ui.setting.SettingItem

class MqttAdapter(
    private val listener: OnMqttItemClickListener
) : ListAdapter<MqttItem, MqttAdapter.MqttViewHolder>(MqttDiffCallback) {


    interface OnMqttItemClickListener {
        fun onItemClicked(item: MqttItem)
    }

    // ... ViewHolder class ...
    class MqttViewHolder(private val binding: ItemMqttBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MqttItem) {
            binding.apply {
                tvSettingLabel.text = item.title
//                tvSettingDescription.text = item.description
                tvSettingValue.text = item.displayValue
            }
        }
    }

    override fun onBindViewHolder(holder: MqttViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener {
            listener.onItemClicked(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MqttViewHolder {
        // 1. Dapatkan LayoutInflater dari konteks parent.
        val inflater = LayoutInflater.from(parent.context)
        // 2. Inflate layout XML item menggunakan ViewBinding.
        val binding = ItemMqttBinding.inflate(inflater, parent, false)
        // 3. Buat dan kembalikan instance MqttViewHolder.
        return MqttViewHolder(binding)
    }

}
// DiffUtil untuk ListAdapter
object MqttDiffCallback : DiffUtil.ItemCallback<MqttItem>() {
    override fun areItemsTheSame(oldItem: MqttItem, newItem: MqttItem): Boolean {
        return oldItem.action == newItem.action
    }

    override fun areContentsTheSame(oldItem: MqttItem, newItem: MqttItem): Boolean {
        return oldItem == newItem
    }
}