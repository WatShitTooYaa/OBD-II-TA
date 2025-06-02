package com.example.obd_iiservice.dtc

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.databinding.ItemDtcBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DTCAdapter(
//    private val listDTCItem: MutableList<DTCItem>,
) : RecyclerView.Adapter<DTCAdapter.ListViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewHolder {
        val binding : ItemDtcBinding = ItemDtcBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    private var listDTCItem : List<DTCItem> = emptyList()

//    init {
//        CoroutineScope(Dispatchers.Main).launch {
//            listDTCFlow.collect {items ->
//                listDTCItem = items
//                notifyDataSetChanged()
//            }
//        }
//    }
    fun submitList(newList: List<DTCItem>) {
        listDTCItem = newList
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(
        holder: ListViewHolder,
        position: Int
    ) {
        val (dtcCode, dtcDescription) = listDTCItem[position]
        holder.binding.tvDtcCode.text = dtcCode
        holder.binding.tvDtcDesc.text = dtcDescription
    }

    override fun getItemCount(): Int {
        return listDTCItem.size
    }

    class ListViewHolder(var binding : ItemDtcBinding) : RecyclerView.ViewHolder(binding.root)


}