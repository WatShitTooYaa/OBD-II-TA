package com.example.obd_iiservice.ui.dtc

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.FragmentDtcBinding
import com.example.obd_iiservice.databinding.FragmentHomeBinding
import com.example.obd_iiservice.databinding.FragmentSettingBinding
import com.example.obd_iiservice.dtc.DTCAdapter
import com.example.obd_iiservice.dtc.OBDManager
import com.example.obd_iiservice.helper.makeToast
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.obd.OBDJobState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DTCFragment : Fragment() {
    private var _binding: FragmentDtcBinding? = null
    private lateinit var rvDTC: RecyclerView
    private lateinit var dtcAdapter: DTCAdapter

    private val binding get() = _binding!!

    companion object {
        fun newInstance() = DTCFragment()
    }

    private val dtcViewModel: DTCViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        initUI()

        _binding = FragmentDtcBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initUI()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initUI(){
        showDTCRecycleView()
        observeViewModel()

        binding.btnCheckDtc.setOnClickListener {
            lifecycleScope.launch {
//                obdRepository.updateDoingJob(true)
            }
            connectAndFetchDTC()
        }
    }

    private fun showDTCRecycleView(){
        rvDTC = binding.rvDtc
        rvDTC.setHasFixedSize(true)
        rvDTC.layoutManager = LinearLayoutManager(activity)
        dtcAdapter = DTCAdapter(
//            dtcViewModel.listDTC
        )
        rvDTC.adapter = dtcAdapter
    }

//    private fun setupRecyclerView() {
//        binding.recyclerViewDtc.layoutManager = LinearLayoutManager(this)
//        binding.recyclerViewDtc.adapter = adapter
//    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            dtcViewModel.listDTC.collect {dtc ->
//                dtcAdapter.notifyItemInserted(listDTC.size - 1)
                dtcAdapter.submitList(dtc)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectAndFetchDTC() {
        val socket = dtcViewModel.socket.value

        if (socket == null){
            makeToast(requireContext(), "Sotket Bluetooth tidak ditemukan")
            return
        }
//        val socket = bluetoothRepository.bluetoothSocket.value
        lifecycleScope.launch {
            try {
                // LANGKAH A: Beri tahu service untuk berhenti polling
                Log.d("DTC_CHECK", "Mengatur state ke CHECK_ENGINE untuk menghentikan polling service.")
                dtcViewModel.updateOBDJobState(OBDJobState.CHECK_ENGINE)
                delay(2000) // Beri sedikit waktu agar service sempat membatalkan job-nya

                // Sekarang aman untuk memulai komunikasi eksklusif
                val obdManager = OBDManager(socket)
                val response = obdManager.getDTCs()

                saveLogToFile(requireContext(), "DTC response", "res", response)
                dtcViewModel.parseAndSetDTC(response)

            } catch (e: Exception) {
                e.printStackTrace()
                makeToast(requireContext(), "Gagal mengambil data DTC: ${e.message}")
            } finally {
                // LANGKAH C: Beri tahu service untuk melanjutkan polling
                Log.d("DTC_CHECK", "Pengecekan DTC selesai. Mengembalikan state ke FREE.")
                dtcViewModel.updateOBDJobState(OBDJobState.FREE)
            }
        }
    }
}