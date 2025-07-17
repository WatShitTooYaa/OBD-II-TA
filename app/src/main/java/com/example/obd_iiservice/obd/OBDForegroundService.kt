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
import com.example.obd_iiservice.bluetooth.BluetoothConnectionState
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
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
                obdRepository.resetOBDData()
            }
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
                            if (readJob == null || !readJob!!.isActive) {
                                if (socket != null) {
                                    Log.d("triple", "obdjobstate:free")

                                    //start reading
                                    readJob = startReading(
                                        applicationContext,
                                        socket.inputStream,
                                        socket.outputStream
                                    )
                                    launch {
                                        obdRepository.updateServiceState(ServiceState.RUNNING)
                                    }
                                    //check internet
                                    when (networkStatus) {
                                        NetworkStatus.Available -> {
                                            serviceScope.launch {
                                                if (obdRepository.checkDataForMQTTConnection()) {
                                                    sendToMQTT()
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        makeToast(
                                                            this@OBDForegroundService,
                                                            "MQTT not started, data null"
                                                        )
                                                    }
                                                }
                                            }
                                            withContext(Dispatchers.Main) {
                                                makeToast(
                                                    this@OBDForegroundService,
                                                    "Internet tersedia"
                                                )
                                            }
                                        }

                                        NetworkStatus.Lost -> {
                                            withContext(Dispatchers.Main) {
                                                //                                makeToast(this@OBDForegroundService, "Internet tersedia")
                                                makeToast(
                                                    this@OBDForegroundService,
                                                    "Internet terputus"
                                                )
                                            }
                                        }

                                        NetworkStatus.Unavailable -> {
                                            withContext(Dispatchers.Main) {
                                                //                                makeToast(this@OBDForegroundService, "Internet tersedia")
                                                makeToast(
                                                    this@OBDForegroundService,
                                                    "Tidak ada jaringan"
                                                )
                                            }
                                        }

                                        else -> {}
                                    }
                                } else {
                                    Log.e("OBD", "input atau output null")
                                }
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

    private fun startReading(context: Context, input: InputStream, output: OutputStream): Job? {
        readJob?.cancel()

        //PID untuk mengambil data seperti speed, rpm, dll
        val pidList = listOf("010C", "010D", "0111", "0105", "0110")

        readJob = serviceScope.launch {
            val responseChannel = Channel<String>(Channel.UNLIMITED)

            val initListenerJob = launch {
                obdRepository.listenForResponses(input)
                    .collect { response ->
                        responseChannel.send(response)
                    }
            }

            // inisiasi elm
            val initCmds = listOf("ATZ", "ATE0", "ATH1", "ATSP0", "0100")
            for (cmd in initCmds) {

                obdRepository.sendCommand(output, cmd)
                val response = withTimeoutOrNull(2000) { responseChannel.receive() }
                Log.d("INIT_RESPONSE", "Cmd: '$cmd' -> Resp: '$response'")

                delay(200)
            }

            initListenerJob.cancel()

            val listenerJob = launch {
                obdRepository.listenForResponses(input)
                    .collect { response ->
                        val parsedData = obdRepository.parseOBDResponse(response, context)
                        if (parsedData.isNotEmpty()) {
                            sendOBDData(parsedData)
                        }
                    }
            }

            launch {
                obdRepository.updateOBDJobState(OBDJobState.READING)
            }

            while (isActive) {
                for (pid in pidList) {
                    if (!isActive) break
                    val commandSentSuccessfully = obdRepository.sendCommand(output, pid)
                    if (!commandSentSuccessfully) {
                        this.cancel()
                        break
                    }
                    delay(100)
                }
            }
            launch {
                obdRepository.updateOBDJobState(OBDJobState.FREE)
            }
            listenerJob.cancel()
        }
        return readJob
    }



//    private suspend fun sendCommandAndAwaitResponse(
//        output: OutputStream,
//        input: InputStream,
//        command: String,
//        timeoutMs: Long = 2000
//    ): String = withContext(Dispatchers.IO) {
//        // 1. Bersihkan buffer input dari data sisa/lama sebelum mengirim perintah baru
//        while (input.available() > 0) {
//            input.read()
//        }
//
//        // 2. Kirim perintah
//        output.write((command + "\r").toByteArray())
//        output.flush()
//
//        // 3. Baca respons sampai menemukan prompt '>' atau timeout
//        val responseBuffer = StringBuilder()
//        val startTime = System.currentTimeMillis()
//        val temp = ByteArray(1024)
//
//        while (true) {
//            // Cek timeout
//            if (System.currentTimeMillis() - startTime > timeoutMs) {
//                Log.w("OBD_SYNC_READ", "Timeout saat menunggu respons untuk perintah: $command")
//                break
//            }
//
//            if (input.available() > 0) {
//                val len = input.read(temp)
//                if (len > 0) {
//                    responseBuffer.append(String(temp, 0, len, Charsets.UTF_8))
//                    // Jika prompt ditemukan, kita anggap respons selesai
//                    if (responseBuffer.contains('>')) {
//                        break
//                    }
//                }
//            }
//            // Beri sedikit jeda agar tidak membebani CPU
//            delay(50)
//        }
//
//        // 4. Bersihkan dan kembalikan respons
//        return@withContext responseBuffer.toString()
//            .replace(">", "")
//            .replace("\r", " ")
//            .replace("\n", " ")
//            .trim()
//    }

    private fun sendOBDData(data : Map<String, String>) {
        serviceScope.launch {
            // Ambil data lengkap yang sudah ada
            val oldData = obdRepository.obdData.first()

            // Gabungkan data lama dengan data baru yang baru saja di-parse
            val mergedData = oldData.toMutableMap().apply {
                putAll(data)
            }

            // --- LAKUKAN KALKULASI DI SINI ---
            // Gunakan .get() yang mengembalikan null jika kunci tidak ada, agar aman.
            val speedStr = mergedData["Speed"]
            val mafStr = mergedData["MAF"]

            // Hanya lakukan kalkulasi jika kedua nilai yang dibutuhkan ada dan valid.
            if (speedStr != null && mafStr != null) {
                val speedInt = speedStr.toIntOrNull()
                val mafInt = mafStr.toIntOrNull()

                if (speedInt != null && mafInt != null && mafInt > 0) {
                    try {
                        // Rumus konsumsi bahan bakar (contoh, sesuaikan jika perlu)
                        // MPG = (7.718 * VSS * 1) / (MAF * 1)
                        // kml = MPG * 0.425
                        val mpg = (7.718 * speedInt) / mafInt
                        val kml = mpg * 0.425
                        // Tambahkan hasil kalkulasi ke dalam map
//                        mergedData["Fuel"] = "%.2f".format(Locale.US, kml)
                        mergedData["Fuel"] = kml.toInt().toString()
//                        Log.d("Fuel", mergedData.getValue("Fuel"))
                    } catch (e: Exception) {
                        // Tangani jika ada error pembagian dengan nol atau lainnya
                        Log.e("FuelCalc", "Error calculating fuel consumption: ${e.message}")
                    }
                }
            }

            // Update StateFlow utama dengan data yang sudah digabungkan dan dikalkulasi
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

            val rpm = data["RPM"]?.toIntOrNull()
            val speed = data["Speed"]?.toIntOrNull()
            val throttle = data["Throttle"]?.toIntOrNull()
            val temp = data["Temperature"]?.toIntOrNull()
            val maf = data["MAF"]?.toDoubleOrNull()
            val fuel = data["Fuel"]?.toIntOrNull()

            val exceeded = listOfNotNull(
                rpm?.takeIf { it > threshold.rpm },
                speed?.takeIf { it > threshold.speed },
                throttle?.takeIf { it > threshold.throttle },
                temp?.takeIf { it > threshold.temp },
                maf?.takeIf { it > threshold.maf },
                fuel?.takeIf { it > threshold.temp }
            ).isNotEmpty()

            val isPlaying = mainRepository.isPlaying.first()

            if (exceeded && !isPlaying) {
                val streamId = mainRepository.soundPool.play(
                    mainRepository.beepSoundId, 1f, 1f, 0, -1, 1f
                )
                serviceScope.launch {
                    launch {
                        mainRepository.updateCurrentStreamId(streamId)
                    }
                    launch {
                        mainRepository.updateIsPlaying(true)
                    }
                }
            } else if (!exceeded) {
                mainRepository.currentStreamId.firstOrNull()?.let {
                    mainRepository.soundPool.stop(it)
                }
                serviceScope.launch {
                    launch {
                        mainRepository.updateCurrentStreamId(null)
                    }
                    launch {
                        mainRepository.updateIsPlaying(false)
                    }
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun sendToMQTT(){
        val mqttConfig = preferenceManager.getMQTTConfig()
        mqttHelper = MqttHelper(mqttConfig, serviceScope)
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
                    delay(500)
                    obdRepository.obdData
                        .sample(300)
                        .collect { data ->
//                        val notification = buildNotification(data)
//                        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//                        Log.d("MQTT_PAYLOAD", "Data map yang akan dikirim: $data")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopService(Intent(this, OBDForegroundService::class.java))
        }

        applicationScope.launch {
            Log.d("OBDService", "Running async cleanup tasks...")
//            obdRepository.updateDoingJob(false)
            launch {
                mainRepository.currentStreamId.firstOrNull()?.let {
                    mainRepository.soundPool.stop(it)
                }
            }
            launch {
                mainRepository.updateCurrentStreamId(null)
            }
            launch {
                mainRepository.updateIsPlaying(false)
            }
            launch {
                obdRepository.updateServiceState(ServiceState.STOPPED)
            }
            Log.d("OBDService", "Async cleanup tasks launched.")
        }
        Log.d("OBDService", "Cancelling service-specific scope...")
        serviceScope.cancel()
        Log.d("OBDService", "onDestroy() finished.")
        super.onDestroy()
    }

}

