package com.example.obd_iiservice.ui.dtc

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.bluetooth.BluetoothConnectionState
import com.example.obd_iiservice.bluetooth.BluetoothRepository
import com.example.obd_iiservice.databinding.FragmentDtcBinding
import com.example.obd_iiservice.databinding.FragmentHomeBinding
import com.example.obd_iiservice.databinding.FragmentSettingBinding
import com.example.obd_iiservice.dtc.DTCAdapter
import com.example.obd_iiservice.dtc.OBDManager
import com.example.obd_iiservice.helper.makeToast
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.obd.OBDJobState
import com.example.obd_iiservice.obd.OBDRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.log

@AndroidEntryPoint
class DTCFragment : Fragment() {
    private var _binding: FragmentDtcBinding? = null
    private var dtcJob: Job? = null

    private lateinit var rvDTC: RecyclerView
    private lateinit var dtcAdapter: DTCAdapter

    private val binding get() = _binding!!
    @Inject
    lateinit var obdRepository: OBDRepository
    @Inject
    lateinit var bluetoothRepository: BluetoothRepository

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
        val input = socket.inputStream
        val output = socket.outputStream
//        val socket = bluetoothRepository.bluetoothSocket.value
        lifecycleScope.launch {
            try {
                // LANGKAH A: Beri tahu service untuk berhenti polling
                Log.d("DTC_CHECK", "Mengatur state ke CHECK_ENGINE untuk menghentikan polling serviceyahahaha.")
                dtcViewModel.updateOBDJobState(OBDJobState.CHECK_ENGINE)
                delay(1000) // Beri sedikit waktu agar service sempat membatalkan job-nya

                // Sekarang aman untuk memulai komunikasi eksklusif
                val obdManager = OBDManager(socket)
//                val response = obdManager.getDTCs()

//                gettingDTC(requireContext(), input, output)
                runDtcCheck()
            } catch (e: Exception) {
                e.printStackTrace()
                makeToast(requireContext(), "Gagal mengambil data DTC: ${e.message}")
            } finally {
                // LANGKAH C: Beri tahu service untuk melanjutkan polling
//                Log.d("DTC_CHECK", "Pengecekan DTC selesai. Mengembalikan state ke FREE.")
//                dtcViewModel.updateOBDJobState(OBDJobState.FREE)
            }
        }
    }

    private fun runDtcCheck() {
        val socket = dtcViewModel.socket.value
        if (socket == null || !socket.isConnected) {
            makeToast(requireContext(), "Koneksi Bluetooth tidak aktif.")
            return
        }

        lifecycleScope.launch {
            try {
                // LANGKAH 1: KIRIM SINYAL "JEDA" KE SERVICE
                Log.d("DTC_Flow", "Mengubah state ke BUSY_EXCLUSIVE_TASK untuk menjeda polling.")
                dtcViewModel.updateOBDJobState(OBDJobState.CHECK_ENGINE)
                delay(300) // Beri waktu agar service sempat menghentikan polling-nya

                // LANGKAH 2: LAKUKAN TUGAS DTC PADA KONEKSI YANG SUDAH ADA
                Log.d("DTC_Flow", "Polling dijeda. Memulai pengecekan DTC...")
                val dtcResponse = performDtcRequest(socket)
                Log.i("DTC_Flow", "Hasil DTC yang diterima: $dtcResponse")

                val dtcList = obdRepository.parseOBDDTCResponse(dtcResponse, requireContext())
                dtcViewModel.setDTC(dtcList)
                val data = mutableMapOf<String, String>()
                data["DTC"] = dtcList.toString()
                obdRepository.updateData(data)
            } catch (e: Exception) {
                Log.e("DTC_Flow", "Error selama proses pengecekan DTC", e)
            } finally {
                // LANGKAH 3: KIRIM SINYAL "LANJUTKAN" KE SERVICE
                Log.d("DTC_Flow", "Pengecekan selesai. Mengembalikan state ke POLLING_PIDS.")
                dtcViewModel.updateOBDJobState(OBDJobState.FREE) // atau OBDJobState.FREE
            }
        }
    }

    /**
     * Fungsi sinkron untuk mengirim perintah dan menunggu responsnya.
     * Ini adalah versi sederhana dari 'runDTCFun' Anda.
     */
    private suspend fun performDtcRequest(socket: BluetoothSocket): String {
        // Inisialisasi sederhana (opsional, tergantung perilaku adapter)
        // Seringkali hanya ATE0 dan ATH0 yang diperlukan jika koneksi sudah stabil.
        val obdManager = OBDManager(socket)

        val initCmds = listOf("ATE0", "ATH0")
        for (cmd in initCmds) {
            val initResponse = obdManager.sendCommandAndAwaitResponse(socket.outputStream, socket.inputStream, cmd)
            Log.d("DTC_INIT", "Cmd: '$cmd' -> Resp: '$initResponse'")
        }

        // Kirim perintah DTC dan kembalikan hasilnya
        return obdManager.sendCommandAndAwaitResponse(socket.outputStream, socket.inputStream, "03")
    }

//    private fun gettingDTC(context: Context, input: InputStream, output: OutputStream) {
////        dtcJob?.cancel()
//
//        lifecycleScope.launch {
//            launch {
//                obdRepository.updateOBDJobState(OBDJobState.CHECK_ENGINE)
//            }
//            launch {
//                bluetoothRepository.disconnect()
//                Log.d("DTC fragment", "disconnecting bluetooth")
//            }
//            launch {
//                delay(1000)
//                Log.d("DTC fragment", "connecting bluetooth")
//                bluetoothRepository.connectToDeviceCallback(
//                    address = bluetoothRepository.bluetoothAddress.value!!,
//                    onSuccess = {
////                        val duration = System.nanoTime() - startTime
////                        val durationInMillis = TimeUnit.NANOSECONDS.toMillis(duration)
//                        val socket = bluetoothRepository.bluetoothSocket.value
//                        val device = socket?.remoteDevice
//                        lifecycleScope.launch {
////                            bluetoothViewModel.saveBluetoothAddress(address)
//                            bluetoothRepository.updateConnectionState(BluetoothConnectionState.CONNECTED)
//                            withContext(Dispatchers.Main){
//                                Toast.makeText(this@DTCFragment.requireContext(), "Connected to ${device?.name} ", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                        saveLogToFile(requireContext(), "Connect Bluetooth", "OK", "Connected to ${device?.name}.")
////                        startAndBindOBDService()
//                        try {
//                            if (socket != null) {
//                                runDTCFun(socket.inputStream, socket.outputStream, requireContext())
//                            }
//                        } catch (e: Exception) {
//                            Log.e("run DTC fun", "gettingDTC: ",e )
//                        } finally {
//
//                        }
//                    },
//                    onError = { error ->
////                        val duration = System.nanoTime() - startTime
////                        val durationInMillis = TimeUnit.NANOSECONDS.toMillis(duration)
//                        lifecycleScope.launch {
//                            bluetoothRepository.updateConnectionState(BluetoothConnectionState.IDLE)
//                        }
//                        saveLogToFile(requireContext(), "Connect Bluetooth", "ERROR", "Connection failed: $error.")
//                        Log.e("Bluetooth", "Connection failed: $error")
//                    }
//                )
//            }
//        }
//
//
////        return dtcJob
//    }
//
//    private fun runDTCFun(input: InputStream, output: OutputStream, context: Context){
//        flushInputBuffer(input)
//        lifecycleScope.launch {
//            val responseChannel = Channel<String>(Channel.UNLIMITED)
//
//            val initListenerJob = launch {
//                obdRepository.listenForResponses(input)
//                    .collect { response ->
//                        responseChannel.send(response)
//                    }
//            }
//
//            // inisiasi elm
//            val initCmds = listOf("ATZ", "ATE0", "ATH1", "ATL0", "ATSP0")
//            for (cmd in initCmds) {
//
//                obdRepository.sendCommand(output, cmd)
//                val response = withTimeoutOrNull(2000) { responseChannel.receive() }
//                Log.d("INIT_RESPONSE", "Cmd: '$cmd' -> Resp: '$response'")
//
//                delay(200)
//            }
//
//            initListenerJob.cancel()
//
//            var checkDTC = false
//
//            val listenerJob = launch {
//                obdRepository.listenForResponses(input)
//                    .collect { response ->
//                        val responseDTC = obdRepository.parseOBDDTCResponse(response, context)
//                        if (responseDTC.isNotEmpty()) {
//                            val data = mutableMapOf<String, String>()
//                            data["DTC"] = responseDTC.toString()
//
////                            sendOBDData(parsedData)
//                            Log.d("DTC_RESPONSE", "gettingDTC: $responseDTC")
//                            dtcViewModel.setDTC(responseDTC)
//                            checkDTC = true
//                            obdRepository.updateData(data)
//                        }
//                    }
//            }
//
//            while (isActive) {
//                if (checkDTC){
//                    break
//                }
//
//                val commandSentSuccessfully = obdRepository.sendCommand(output, "03")
//                if (!commandSentSuccessfully) {
//                    this.cancel()
//                    break
//                }
//                delay(1000)
//            }
//            launch {
//                Log.d("DTC fun", "change job state")
//
//                delay(400)
//                obdRepository.updateOBDJobState(OBDJobState.FREE)
//            }
//            listenerJob.cancel()
//        }
//    }

    private fun flushInputBuffer(input: InputStream) {
        try {
            var i = 0
            while (input.available() > 0) {
                Log.d("clean inputstream", "flushInputBuffer ke-$i ")
                input.read()
                i++
            }
        } catch (e: Exception) {
            Log.e("OBD_FLUSH", "Gagal membersihkan buffer input", e)
        }
    }
}