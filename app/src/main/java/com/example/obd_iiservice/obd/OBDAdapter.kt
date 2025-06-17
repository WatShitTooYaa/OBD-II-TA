package com.example.obd_iiservice.obd

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
            if (item.label == "Fuel"){
                tvItemObdType.text = "Fuel Con."
            }
            else {
                tvItemObdType.text = item.label
            }
            tvItemObdUnit.text = item.unit
            tvItemObdValue.text = item.value


            gauge1.apply {
                // Pastikan tipe data sesuai (toFloat atau toInt)
                startValue = item.startValue.toInt()
                endValue = item.endValue.toInt()
                value = item.value.toInt()
                when(item.value.toFloat() > item.threshold.toFloat()){
                    true -> {
                        pointStartColor = ContextCompat.getColor(context, R.color.gauge_point_end)
                        Log.d("${item.label} true", "threshold : ${item.threshold}  val : ${item.value}")
                    }
                    false -> {
                        pointStartColor = ContextCompat.getColor(context, R.color.green_second)
                        Log.d("${item.label} false", "threshold : ${item.threshold}  val : ${item.value}")
//                        Log.d("Adapter", "False")
                    }
                }
//                if (item.label == "Temperature"){
//                    pointSize = 30
//                }
//                pointSize = 100
            }
//            Log.d("Adapter item", "${item.label} : ${item.threshold}")
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