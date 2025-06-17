package com.example.obd_iiservice.dtc

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.bluetooth.BluetoothRepository
import com.example.obd_iiservice.databinding.ActivityDtcBinding
import com.example.obd_iiservice.helper.makeToast
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.obd.OBDJobState
import com.example.obd_iiservice.obd.OBDRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DTCActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDtcBinding
    private lateinit var rvDTC: RecyclerView
    private lateinit var dtcAdapter: DTCAdapter
    private val listDTC = mutableListOf<DTCItem>()
    private val dtcViewModel : DTCViewModel by viewModels()

    @Inject lateinit var bluetoothRepository: BluetoothRepository
    @Inject lateinit var obdRepository: OBDRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDtcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rvDTC = binding.rvDtc
        rvDTC.setHasFixedSize(true)
        initUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_dtc)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initUI(){
        showDTCRecycleView()
        observeViewModel()

        binding.btnCheckDtc.setOnClickListener {
//            lifecycleScope.launch {
//                obdRepository.updateDoingJob(true)
//            }
            connectAndFetchDTC()
        }
    }

    private fun showDTCRecycleView(){
        rvDTC.layoutManager = LinearLayoutManager(this)
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
        if (bluetoothRepository.bluetoothSocket.value == null){
            makeToast(this, "Sotket Bluetooth tidak ditemukan")
            return
        }
        val socket = bluetoothRepository.bluetoothSocket.value
        lifecycleScope.launch {
            try {
                if (socket == null){
                    makeToast(this@DTCActivity, //            Toast.makeText(this, "Perangkat OBD tidak ditemukan", Toast.LENGTH_SHORT).show()
                        "Perangkat OBD tidak ditemukan")
                    return@launch
                }
//                launch {
////                    obdRepository.updateOBDJobState(OBDJobState.READING)
//                    dtcViewModel.updateOBDJobState(OBDJobState.CHECK_ENGINE)
//                }
                val obdManager = OBDManager(socket)
//                val response = obdManager.sendCommand()
                val response = obdManager.getDTCs()
                saveLogToFile(this@DTCActivity, "DTC response", "res", response)
                dtcViewModel.parseAndSetDTC(response)
//                launch {
////                    obdRepository.updateOBDJobState(OBDJobState.READING)
//                    dtcViewModel.updateOBDJobState(OBDJobState.FREE)
//                }
//                obdRepository.updateDoingJob(false)
            } catch (e: Exception) {
                e.printStackTrace()
                makeToast(this@DTCActivity, "Gagal koneksi OBD")
//                Toast.makeText(this@DTCActivity, "Gagal koneksi OBD", Toast.LENGTH_SHORT).show()
            }
        }
    }
}