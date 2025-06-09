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
import com.example.obd_iiservice.bluetooth.BluetoothRepository
import com.example.obd_iiservice.helper.MqttHelper
import com.example.obd_iiservice.helper.PreferenceManager
import com.example.obd_iiservice.helper.makeToast
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.internet.NetworkStatus
import com.example.obd_iiservice.main.MainRepository
import com.example.obd_iiservice.threshold.ThresholdRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
//    private val _connectionState = MutableStateFlow(BluetoothConnectionState.IDLE)
//    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState


    private var readJob: Job? = null
    private var mqttJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject lateinit var bluetoothRepository: BluetoothRepository
    @Inject lateinit var obdRepository: OBDRepository
    @Inject lateinit var thresholdRepository: ThresholdRepository
    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var preferenceManager: PreferenceManager
//    @Inject lateinit var mqttHelper: MqttHelper
//    @Inject lateinit var obdViewModel: OBDViewModel
    private lateinit var mqttHelper: MqttHelper


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()

        val input = bluetoothRepository.bluetoothSocket.value?.inputStream
        val output = bluetoothRepository.bluetoothSocket.value?.outputStream

        readJob?.cancel() // cancel sebelumnya kalau ada
        mqttJob?.cancel()
//        if (input != null && output != null) {
//            readJob = startReading(applicationContext, input, output)
//        }

        serviceScope.launch {
            obdRepository.isDoingJob.collectLatest { isDoing ->
                when(isDoing){
                    true -> {
                        readJob?.cancel()
                    }
                    false -> {
                        if (input != null && output != null) {
                            readJob = startReading(applicationContext, input, output)
                        } else {
                            Log.e("OBD", "input atau output null")
                        }
//                        readJob = startReading(applicationContext, input!!, output!!)
                    }
                }
            }
        }
//        while (readJob == null  && input != null && output != null){
//            readJob = startReading(applicationContext, input, output)
//        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            monitorThresholds()
            launch {
//                obdRepository.isDoingJob.collectLatest { isDoing ->
//                    when(isDoing){
//                        true -> {
//                            readJob?.cancel()
//                            mqttJob?.cancel()
//                        }
//                        false -> {
//                            monitorThresholds()
//                        }
//                    }
//                }
            }
            launch {
                combine(
                    obdRepository.isDoingJob,
                    obdRepository.networkStatus
                ) { isDoing, networkStatus ->
                    Pair(isDoing, networkStatus)
                }.collect { (isDoing, networkStatus) ->
                    when(isDoing){
                        true -> {
                            readJob?.cancel()
                            mqttJob?.cancel()
                        }
                        false -> {
                            when (networkStatus) {
                                NetworkStatus.Available -> {
                                    sendToMQTT()
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
                        }
                    }
                }
//                obdRepository.networkStatus.collect { status ->
//                    when (status) {
//                        NetworkStatus.Available -> {
//                            sendToMQTT()
//                            withContext(Dispatchers.Main) {
//                                makeToast(this@OBDForegroundService, "Internet tersedia")
//                            }
//                        }
//                        NetworkStatus.Lost -> {
//                            withContext(Dispatchers.Main) {
////                                makeToast(this@OBDForegroundService, "Internet tersedia")
//                                makeToast(this@OBDForegroundService, "Internet terputus")
//                            }
//                        }
//                        NetworkStatus.Unavailable -> {
//                            withContext(Dispatchers.Main) {
////                                makeToast(this@OBDForegroundService, "Internet tersedia")
//                                makeToast(this@OBDForegroundService, "Tidak ada jaringan")
//                            }
//                        }
//                        else -> {}
//                    }
//                }
            }
        }
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
                        val parsedData = obdRepository.parseOBDResponse(response)
                        if (parsedData.isNotEmpty()) {
                            // Update StateFlow atau kirim ke server
                            obdRepository.updateData(parsedData)
                            sendOBDData(parsedData) // Fungsi Anda untuk mengirim data
                        }
                    }
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
            listenerJob.cancel()
        }
        return readJob
    }

//    fun startReading(context: Context, input: InputStream, output: OutputStream): Job? {
//        readJob?.cancel()
//        readJob = serviceScope.launch {
//            var allSuccess = true
//
//            // GUNAKAN FUNGSI DARI REPOSITORY UNTUK KONSISTENSI
//            // Hapus fungsi readResponse() lokal
//
//            val initCmds = listOf("ATZ", "ATE0", "ATL0", "ATH1", "ATSP0", "0100") // Perhatikan ATH1!
//
//            for (cmd in initCmds) {
//                // Gunakan sendCommand dari repository, namun kita perlu versi yang lebih baik
//                // Untuk sekarang, kita modifikasi sementara di sini
//                val response = withContext(Dispatchers.IO) {
//                    output.write((cmd + "\r").toByteArray())
//                    output.flush()
//                    delay(400) // Beri waktu lebih untuk reset (ATZ) dan perintah lain
//                    val buffer = ByteArray(1024)
//                    val bytesRead = input.read(buffer)
//                    if (bytesRead > 0) {
//                        buffer.decodeToString(0, bytesRead)
//                    } else {
//                        "NO RESPONSE"
//                    }
//                }
//
//                Log.d("OBD_INIT", "Command: $cmd → Response: ${response.trim()}")
//                saveLogToFile(context, "OBD_INIT", "CMD: $cmd", response.trim())
//
//                // Cek sederhana untuk error (bisa dibuat lebih baik)
//                if (response.contains("?") || response.contains("ERROR", ignoreCase = true)) {
//                    allSuccess = false
//                    Log.e("OBD_INIT", "Initialization failed at command: $cmd")
//                    break
//                }
//            }
//
//            if (!allSuccess) {
//                Log.e("OBD", "Aborting due to initialization failure.")
//                return@launch // Hentikan coroutine jika inisialisasi gagal
//            }
//
//            delay(1000) // Beri jeda setelah inisialisasi sebelum membaca data sensor
//
//            while (isActive) {
//                try {
//                    val data = obdRepository.readOBDData(input, output, context)
//                    if (data.isNotEmpty()) {
//                        sendOBDData(data)
//                        Log.d("Data_OBD", data.toString())
//                    } else {
//                        Log.w("Data_OBD", "Received empty data map from repository.")
//                    }
//                } catch (e: IOException) {
//                    Log.e("OBD", "Error reading OBD data", e)
//                    break
//                }
//
//                // !! PENTING: AKTIFKAN KEMBALI DELAY INI !!
//                delay(500) // Beri jeda 500ms (atau sesuai kebutuhan)
//            }
//        }
//        return readJob
//    }

//    fun startReading(context: Context, input: InputStream, output: OutputStream) : Job? {
//        readJob?.cancel()
//        readJob = serviceScope.launch {
//            val buffer = ByteArray(1024)
//
//            // Fungsi bantu untuk membaca respons dari ELM327 sampai tanda '>'
//            suspend fun readResponse(): String {
//                val response = StringBuilder()
//
//                val startTime = System.currentTimeMillis()
//                val timeoutMillis = 3000L // 3 detik timeout
//
//                withContext(Dispatchers.IO) {
//                    while (true) {
//                        val bytesRead = input.read(buffer)
//                        if (bytesRead > 0) {
//                            val chunk = buffer.decodeToString(0, bytesRead)
//                            response.append(chunk)
//                            if ('>' in chunk) break
//                        }
//                        // Timeout protection
//                        if (System.currentTimeMillis() - startTime > timeoutMillis) {
//                            response.append("TIMEOUT")
//                            break
//                        }
//
//                        delay(50)
//                    }
//                }
//                return response.toString().trim()
//            }
//            var allSuccess = true
//
//            val initCmds = listOf("ATZ", "ATE0", "ATL0", "ATH0", "ATSP0", "0100")
//
//            for (cmd in initCmds) {
//                withContext(Dispatchers.IO) {
//                    output.write((cmd + "\r").toByteArray())
//                    output.flush()
//                    delay(200)
////                    input.read(buffer)
//                }
//                delay(300)
//                val response = readResponse()
//                Log.d("OBD_INIT", "Command: $cmd → Response: $response")
//                saveLogToFile(context, "OBD_INIT", "WAIT", response)
//                if (response.contains("ERROR", ignoreCase = true)) {
//                    allSuccess = false
//                    break
//                }
//            }
//            delay(1500)
//
////            _readSuccess.value = allSuccess
//
//            while (isActive){
//                try {
//                    val data = obdRepository.readOBDData(input, output, context)
//                    sendOBDData(data)
////                    Log.d("Data_OBD", data.toString())
////                    saveLogToFile(context, "OBD Data", "DATA", data.toString())
//                } catch (e: IOException) {
//                    Log.e("OBD", "error reading OBD data", e)
////                    _readSuccess.value = false
//                    break
//                }
////                delay(200)
//            }
//        }
//        return readJob
//    }

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
            },
            onFailure = {
                Log.e("MQTT Helper", "Connection error: ${it.message}")
                saveLogToFile(
                    this@OBDForegroundService,
                    "MQTTHelper",
                    "ERROR",
                    "Error saat menghubungkan MQTT, cek konfigurasi" )
            }
        )
    }

    override fun onDestroy() {
        readJob?.cancel()
        mqttJob?.cancel()
        serviceScope.launch {
            obdRepository.updateDoingJob(false)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

}

