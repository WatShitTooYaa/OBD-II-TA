package com.example.obd_iiservice.obd

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.ItemObdDataBinding

class OBDAdapter(
    private val list: MutableList<OBDItem>
) : RecyclerView.Adapter<OBDAdapter.ListViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewHolder {
        val binding = ItemObdDataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ListViewHolder,
        position: Int
    ) {
        val items = list[position]
        val circleStatus = if (items.isNormal){
            R.drawable.circle_status_normal
        } else{
            R.drawable.circle_status_not_normal
        }
        holder.binding.apply {
            tvItemDataLabel.text = items.label
            iconObd.setImageResource(items.iconResId)
            tvItemDataValue.text = items.value
            viewStatusCircle.setBackgroundColor(circleStatus)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ListViewHolder(var binding: ItemObdDataBinding) : RecyclerView.ViewHolder(binding.root)

    // Optional: untuk update nilai dinamis jika dibutuhkan
    fun updateItemValue(position: Int, newValue: String, isNormal: Boolean) {
        (list as? MutableList)?.let {
            it[position] = it[position].copy(value = newValue, isNormal = isNormal)
            notifyItemChanged(position)
        }
    }
}