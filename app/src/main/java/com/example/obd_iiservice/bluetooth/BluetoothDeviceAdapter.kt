package com.example.obd_iiservice.bluetooth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.databinding.ItemDeviceBinding



class BluetoothDeviceAdapter(
    private val listDevice : MutableList<BluetoothDeviceItem>,
    private val listener : OnDeviceConnectListener
    ) : RecyclerView.Adapter<BluetoothDeviceAdapter.ListViewHolder>() {

    class ListViewHolder(var binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return  listDevice.size
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val (name, address) = listDevice[position]
        holder.binding.tvItemName.text = name
        holder.binding.tvItemAddress.text = address
        holder.binding.btnSave.setOnClickListener {
//            listener.onConnect(address)
            listener.onSaveBluetoothDevice(address)
        }
    }

    interface OnDeviceConnectListener {
//        fun onConnect(address: String)
        fun onSaveBluetoothDevice(address: String)
    }
}