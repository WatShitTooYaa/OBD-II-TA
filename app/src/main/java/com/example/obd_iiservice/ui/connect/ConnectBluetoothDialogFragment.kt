package com.example.obd_iiservice.ui.connect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.FragmentDialogConnectBluetoothBinding

class ConnectBluetoothDialogFragment : DialogFragment() {
    private var _binding: FragmentDialogConnectBluetoothBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDialogConnectBluetoothBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Contoh inisialisasi awal
        binding.textStatusConnection.text = "Tidak Terhubung"
        binding.textStatusConnection.setTextColor(resources.getColor(R.color.green_bg, null)) // Ganti dengan warna Anda

        // Atur listener untuk tombol scan
        binding.buttonScan.setOnClickListener {
            startScan()
        }
    }

    private fun startScan() {
        // Tampilkan progress bar dan sembunyikan tombol
        binding.progressBarScan.visibility = View.VISIBLE
        binding.buttonScan.isEnabled = false // Nonaktifkan tombol saat scan

        // ... panggil logika scan bluetooth Anda di sini ...
        // Setelah scan selesai, di dalam callback-nya:
        // binding.progressBarScan.visibility = View.GONE
        // binding.buttonScan.isEnabled = true
        // update RecyclerView dengan hasil scan
    }

    // ... sisa logika untuk RecyclerView, dll ...

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}