package com.example.obd_iiservice.obd

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.obd_iiservice.R
import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.bluetooth.BluetoothRepository
import com.example.obd_iiservice.helper.MqttHelper
import com.example.obd_iiservice.helper.PreferenceManager
import com.example.obd_iiservice.helper.makeToast
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.internet.NetworkStatus
import com.example.obd_iiservice.main.MainRepository
import com.example.obd_iiservice.setting.ui.threshold.ThresholdRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@AndroidEntryPoint
class OBDForegroundService : Service() {
    private var readJob: Job? = null
    private var mqttJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject lateinit var bluetoothRepository: BluetoothRepository
    @Inject lateinit var obdRepository: OBDRepository
    @Inject lateinit var thresholdRepository: ThresholdRepository
    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var preferenceManager: PreferenceManager

    private lateinit var mqttHelper: MqttHelper


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        readJob?.cancel() // cancel sebelumnya kalau ada
        mqttJob?.cancel()
        serviceScope.launch {
            launch {
                monitorThresholds()
                Log.d("onstartcmnd", "monitor threshold")
            }
            launch {
                obdRepository.updateOBDJobState(OBDJobState.FREE)
                Log.d("onstartcmnd", "obdjobstate")
            }
            launch {
                obdRepository.updateMQTTJobState(MQTTJobState.FREE)
                Log.d("onstartcmnd", "mqtt job")

            }
            launch {
                obdRepository.updateServiceState(ServiceState.RUNNING)
                Log.d("onstartcmnd", "service state")

            }
            launch {
                bluetoothRepository.bluetoothSocket.collect { socket ->
                    Log.d("DEBUG_FLOW", "bluetoothSocket emitted: ${socket != null}")
                }
            }
            launch {
                obdRepository.obdJobState.collect { state ->
                    Log.d("DEBUG_FLOW", "obdJobState emitted: $state")
                }
            }
            launch {
                obdRepository.networkStatus.collect { status ->
                    Log.d("DEBUG_FLOW", "networkStatus emitted: $status")
                }
            }
            launch {
                //fungsi observeConditional
                combine(
                    bluetoothRepository.bluetoothSocket,
                    obdRepository.obdJobState,
                    obdRepository.networkStatus
                ) { socket, jobType, networkStatus ->
//                    Pair(jobType, networkStatus)
                    Triple(socket, jobType, networkStatus)
                }.collect { (socket, jobType, networkStatus) ->
                    Log.d("triple", "socket:${socket.toString()}\njobType:${jobType}\nnetworkStatus${networkStatus}")
                    when(jobType){
                        OBDJobState.CHECK_ENGINE -> {
                            readJob?.cancel()
                            mqttJob?.cancel()
                            Log.d("triple", "obdjobstate:checkengin")
                        }
                        OBDJobState.FREE -> {
                            Log.d("triple", "obdjobstate:free beforesocket")

                            if (socket != null){
                                Log.d("triple", "obdjobstate:free")

                                //start reading
                                readJob = startReading(applicationContext, socket.inputStream, socket.outputStream)
                                launch {
                                    obdRepository.updateServiceState(ServiceState.RUNNING)
                                }
                                //check internet
                                when (networkStatus) {
                                    NetworkStatus.Available -> {
                                        serviceScope.launch {
                                            if (obdRepository.checkDataForMQTTConnection()){
                                                sendToMQTT()
                                            } else {
                                                withContext(Dispatchers.Main){
                                                    makeToast(this@OBDForegroundService, "MQTT not started, data null")
                                                }
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            makeToast(this@OBDForegroundService, "Internet tersedia")
                                        }
                                    }
                                    NetworkStatus.Lost -> {
                                        withContext(Dispatchers.Main) {
    //                                makeToast(this@OBDForegroundService, "Internet tersedia")
                                            makeToast(this@OBDForegroundService, "Internet terputus")
                                        }
                                    }
                                    NetworkStatus.Unavailable -> {
                                        withContext(Dispatchers.Main) {
    //                                makeToast(this@OBDForegroundService, "Internet tersedia")
                                            makeToast(this@OBDForegroundService, "Tidak ada jaringan")
                                        }
                                    }
                                    else -> {}
                                }
                            } else {
                                Log.e("OBD", "input atau output null")
                            }
                        }

                        OBDJobState.READING -> {
                            Log.d("triple", "obdjobstate:reading")
                        }

                        OBDJobState.ERROR -> {
                            Log.d("triple", "obdjobstate:error")

                        }
                    }
                }
            }
        }

        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val channelId = "obd_service_channel"
        val channelName = "OBD Service Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("OBD Monitoring")
            .setContentText("Monitoring data kendaraan...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    fun startReading(context: Context, input: InputStream, output: OutputStream): Job? {
        readJob?.cancel()

        // Daftar PID yang ingin dibaca secara berulang
        val pidList = listOf("010C", "010D", "0111", "0105", "0110")

        readJob = serviceScope.launch {
            // --- Tahap Inisialisasi ---
            val initCmds = listOf("ATZ", "ATE0", "ATH1", "ATSP0", "0100")
            for (cmd in initCmds) {
                obdRepository.sendCommand(output, cmd)
                delay(400) // Delay singkat masih dibutuhkan untuk perintah AT
            }

            // --- Tahap Memulai Listener ---
            // Jalankan listener di coroutine terpisah agar tidak memblokir loop pengirim
            val listenerJob = launch {
                obdRepository.listenForResponses(input)
                    .collect { response ->
                        // Setiap kali respons diterima, kita parse dan update data
                        Log.d("OBD_RESPONSE", "Raw: $response")
                        val parsedData = obdRepository.parseOBDResponse(response, context)
                        if (parsedData.isNotEmpty()) {
                            // Update StateFlow atau kirim ke server
//                            obdRepository.updateData(parsedData)
                            sendOBDData(parsedData) // Fungsi Anda untuk mengirim data
                        }
                    }
            }

            launch {
                obdRepository.updateOBDJobState(OBDJobState.READING)
            }

            // --- Tahap Loop Pengirim Perintah ---
            // Loop ini sekarang hanya bertugas mengirim perintah secara berurutan
            while (isActive) {
                for (pid in pidList) {
                    if (!isActive) break // Cek jika job sudah dibatalkan
                    obdRepository.sendCommand(output, pid)
                    // TIDAK ADA DELAY DI SINI!
                    // Kita akan menunggu secara alami sampai respons diterima oleh listener
                    // Namun, kita perlu sedikit jeda agar tidak terlalu cepat
                    // dan agar listener sempat memproses.
                    delay(100) // Delay minimal untuk mencegah "command overlapping"
                }
            }

            // Pastikan listener juga berhenti saat job utama selesai
            launch {
                obdRepository.updateOBDJobState(OBDJobState.FREE)
            }
            listenerJob.cancel()
        }
        return readJob
    }

    private fun sendOBDData(data : Map<String, String>) {
//        viewModelScope.launch {
//            obdRepository.updateData(data)
//        }
        serviceScope.launch {
            val oldData = obdRepository.obdData.first()

            val mergedData = oldData.toMutableMap().apply {
                for ((key, value ) in data) {
                    if (value.isNotBlank()) {
                        this[key]= value
                    }
                }
            }
            obdRepository.updateData(mergedData)
        }
    }

    private suspend fun monitorThresholds() {
        combine(
            obdRepository.obdData, // ini StateFlow
            thresholdRepository.thresholdData // juga StateFlow
        ) { data, threshold ->
            Pair(data, threshold)
        }.collect { (data, threshold) ->

            val rpm = data["Rpm"]?.toIntOrNull()
            val speed = data["Speed"]?.toIntOrNull()
            val throttle = data["Throttle"]?.toIntOrNull()
            val temp = data["Temp"]?.toIntOrNull()
            val maf = data["Maf"]?.toDoubleOrNull()

            val exceeded = listOfNotNull(
                rpm?.takeIf { it > threshold.rpm },
                speed?.takeIf { it > threshold.speed },
                throttle?.takeIf { it > threshold.throttle },
                temp?.takeIf { it > threshold.temp },
                maf?.takeIf { it > threshold.maf }
            ).isNotEmpty()

            val isPlaying = mainRepository.isPlaying.first()

            if (exceeded && !isPlaying) {
                val streamId = mainRepository.soundPool.play(
                    mainRepository.beepSoundId, 1f, 1f, 0, -1, 1f
                )
                mainRepository.updateCurrentStreamId(streamId)
                mainRepository.updateIsPlaying(true)
            } else if (!exceeded && isPlaying) {
                mainRepository.currentStreamId.firstOrNull()?.let {
                    mainRepository.soundPool.stop(it)
                }
                mainRepository.updateCurrentStreamId(null)
                mainRepository.updateIsPlaying(false)
            }
        }
    }

    private suspend fun sendToMQTT(){
        val mqttConfig = preferenceManager.getMQTTConfig()
        mqttHelper = MqttHelper(mqttConfig)
        mqttHelper.connect(
            onSuccess = {
                saveLogToFile(
                    this@OBDForegroundService,
                    "MQTTHelper",
                    "OK",
                    "Berhasil Terhubung ke ${mqttConfig.host}"
                )
                //                mqttHelper.publish("obd_fate", "{\"rpm\": 3000}")
                mqttJob = serviceScope.launch {
                    obdRepository.obdData.collect { data ->
//                        val notification = buildNotification(data)
                        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        val jsonData = JSONObject(data).toString()
                        mqttHelper.publish(mqttConfig.topic!!, jsonData)

//                        manager.notify(notificationId, notification)
                    }
                }
                serviceScope.launch {
                    obdRepository.updateMQTTJobState(MQTTJobState.RUNNING)
                }
            },
            onFailure = {
                Log.e("MQTT Helper", "Connection error: ${it.message}")
                saveLogToFile(
                    this@OBDForegroundService,
                    "MQTTHelper",
                    "ERROR",
                    "Error saat menghubungkan MQTT, cek konfigurasi" )
                serviceScope.launch {
                    obdRepository.updateMQTTJobState(MQTTJobState.ERROR)
                }
            }
        )
    }

    override fun onDestroy() {
        Log.d("OBDService", "onDestroy() is being called!")
        readJob?.cancel()
        mqttJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)

        applicationScope.launch {
            Log.d("OBDService", "Running async cleanup tasks...")
            obdRepository.updateDoingJob(false)
            mainRepository.updateCurrentStreamId(null)
            mainRepository.updateIsPlaying(false)
            obdRepository.updateServiceState(ServiceState.STOPPED)
            Log.d("OBDService", "Async cleanup tasks launched.")
        }
        Log.d("OBDService", "Cancelling service-specific scope...")
        serviceScope.cancel()
        Log.d("OBDService", "onDestroy() finished.")
        super.onDestroy()
    }

}

