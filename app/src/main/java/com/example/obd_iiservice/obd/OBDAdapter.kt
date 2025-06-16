package com.example.obd_iiservice.obd

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.ItemLayoutObdBinding
import com.example.obd_iiservice.databinding.ItemObdDataBinding

class OBDAdapter(
    private val itemHeight: Int
): ListAdapter<OBDItem, OBDAdapter.ListViewHolder>(OBDItemDiffCallback){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemLayoutObdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.layoutParams.height = itemHeight
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        // Gunakan getItem(position) untuk mendapatkan item saat ini
        val item = getItem(position)

        holder.binding.apply {
            tvItemObdUnit.text = item.unit
            tvItemObdType.text = item.label
            tvItemObdValue.text = item.currValue

            gauge1.apply {
                // Pastikan tipe data sesuai (toFloat atau toInt)
                startValue = item.startValue.toInt()
                endValue = item.endValue.toInt()
                value = item.currValue.toInt() // Konversi aman
            }
        }
    }

    class ListViewHolder(var binding: ItemLayoutObdBinding) : RecyclerView.ViewHolder(binding.root)

}
// DiffUtil akan menggunakan ini untuk mendeteksi perubahan
object OBDItemDiffCallback : DiffUtil.ItemCallback<OBDItem>() {

    // Dipanggil untuk memeriksa apakah dua item merepresentasikan objek yang sama.
    // Gunakan ID unik, dalam kasus ini 'label' adalah kandidat yang sempurna.
    override fun areItemsTheSame(oldItem: OBDItem, newItem: OBDItem): Boolean {
        return oldItem.label == newItem.label
    }

    // Dipanggil jika areItemsTheSame() mengembalikan true.
    // Ini memeriksa apakah konten item telah berubah.
    // Karena OBDItem adalah data class, '==' akan membandingkan semua propertinya.
    override fun areContentsTheSame(oldItem: OBDItem, newItem: OBDItem): Boolean {
        return oldItem == newItem
    }
}

//class OBDAdapter(
//    private val obdItems: MutableList<OBDItem>,
//    private val itemHeight: Int
//) : RecyclerView.Adapter<OBDAdapter.ListViewHolder>() {
//
//    // Definisikan object konstanta untuk payload
//    companion object {
//        const val PAYLOAD_VALUE_UPDATE = "PAYLOAD_VALUE_UPDATE"
//    }
//
//    override fun onCreateViewHolder(
//        parent: ViewGroup,
//        viewType: Int
//    ): ListViewHolder {
//        val binding = ItemLayoutObdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        binding.root.layoutParams.height = itemHeight
//        return ListViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(
//        holder: ListViewHolder,
//        position: Int
//    ) {
//        val item = obdItems[position]
//        holder.binding.apply {
//            tvItemObdUnit.text = item.unit
//            tvItemObdType.text = item.label
//            tvItemObdValue.text = item.value
//            gauge1.apply {
//                startValue = item.startValue.toInt()
//                endValue = item.endValue.toInt()
//                value = item.currValue.toInt()
//            }
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return obdItems.size
//    }
//
//    class ListViewHolder(var binding: ItemLayoutObdBinding) : RecyclerView.ViewHolder(binding.root)
//    // Fungsi untuk mengupdate data di adapter
//    fun updateData(newList: List<OBDItem>) {
//        obdItems.clear()
//        obdItems.addAll(newList)
//        notifyDataSetChanged() // Beritahu RecyclerView untuk menggambar ulang semua item
//    }
//
//
//}