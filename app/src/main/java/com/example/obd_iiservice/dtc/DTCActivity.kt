package com.example.obd_iiservice.dtc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@AndroidEntryPoint
class DTCActivity : AppCompatActivity() {

    private var dtcJob: Job? = null

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
//                if (socket == null){
//                    makeToast(this@DTCActivity, //            Toast.makeText(this, "Perangkat OBD tidak ditemukan", Toast.LENGTH_SHORT).show()
//                        "Perangkat OBD tidak ditemukan")
//                    return@launch
//                }
//                val obdManager = OBDManager(socket)
//                val response = obdManager.getDTCs()
//                saveLogToFile(this@DTCActivity, "DTC response", "res", response)
//                dtcViewModel.parseAndSetDTC(response)
                if (socket != null) {
                    gettingDTC(this@DTCActivity, socket.inputStream, socket.outputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let { saveLogToFile(this@DTCActivity, "DTC response", "res", it) }
            }
        }
    }

    private fun gettingDTC(context: Context, input: InputStream, output: OutputStream): Job? {
        dtcJob?.cancel()

        lifecycleScope.launch {
            launch {
                obdRepository.updateOBDJobState(OBDJobState.CHECK_ENGINE)
            }
        }

        dtcJob = lifecycleScope.launch {
            val responseChannel = Channel<String>(Channel.UNLIMITED)

            val initListenerJob = launch {
                obdRepository.listenForResponses(input)
                    .collect { response ->
                        responseChannel.send(response)
                    }
            }

            // inisiasi elm
            val initCmds = listOf("ATZ", "ATE0", "ATH1", "ATL0", "ATSP0")
            for (cmd in initCmds) {

                obdRepository.sendCommand(output, cmd)
                val response = withTimeoutOrNull(2000) { responseChannel.receive() }
                Log.d("INIT_RESPONSE", "Cmd: '$cmd' -> Resp: '$response'")

                delay(200)
            }

            initListenerJob.cancel()

            var checkDTC = false

            val listenerJob = launch {
                obdRepository.listenForResponses(input)
                    .collect { response ->
                        val responseDTC = obdRepository.parseOBDDTCResponse(response, context)
                        if (responseDTC.isNotEmpty()) {
                            val data = mutableMapOf<String, String>()
                            data["DTC"] = responseDTC.toString()

//                            sendOBDData(parsedData)
                            Log.d("DTC_RESPONSE", "gettingDTC: $responseDTC")
                            dtcViewModel.setDTC(responseDTC)
                            checkDTC = true
                            obdRepository.updateData(data)
                        }
                    }
            }

            while (isActive) {
                val commandSentSuccessfully = obdRepository.sendCommand(output, "03")
//                if (!commandSentSuccessfully) {
//                    this.cancel()
//                    break
//                }
                if (checkDTC){
                    break
                }
                delay(300)
            }
            launch {
                delay(1000)
                obdRepository.updateOBDJobState(OBDJobState.FREE)
            }
            listenerJob.cancel()
        }
        return dtcJob
    }
}