package com.example.obd_iiservice.obd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import com.example.obd_iiservice.MainActivity
import com.example.obd_iiservice.helper.MqttHelper
import com.example.obd_iiservice.R
import com.example.obd_iiservice.bluetooth.BluetoothRepository
import com.example.obd_iiservice.bluetooth.BluetoothViewModel
import com.example.obd_iiservice.helper.PreferenceManager
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.main.MainRepository
import com.example.obd_iiservice.obd.OBDForegroundDump
import com.example.obd_iiservice.threshold.ThresholdRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@AndroidEntryPoint
class OBDForegroundService : Service() {

    private var readJob: Job? = null
    private var mqttJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject lateinit var bluetoothRepository: BluetoothRepository
    @Inject lateinit var obdRepository: OBDRepository
    @Inject lateinit var thresholdRepository: ThresholdRepository
    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var preferenceManager: PreferenceManager
//    @Inject lateinit var mqttHelper: MqttHelper
    private lateinit var mqttHelper: MqttHelper

    private val notificationId = 1


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()

        val input = bluetoothRepository.bluetoothSocket.value?.inputStream
        val output = bluetoothRepository.bluetoothSocket.value?.outputStream

        readJob?.cancel() // cancel sebelumnya kalau ada
        mqttJob?.cancel()
        if (input != null && output != null) {
            readJob = startReading(applicationContext, input, output)
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            launch {
                monitorThresholds()
            }
            launch {
                sendToMQTT()
            }
        }
    }

    override fun onDestroy() {
        readJob?.cancel()
        mqttJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
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

    fun startReading(context: Context, input: InputStream, output: OutputStream) : Job? {
        readJob?.cancel()
        readJob = serviceScope.launch {
            val buffer = ByteArray(1024)

            // Fungsi bantu untuk membaca respons dari ELM327 sampai tanda '>'
            suspend fun readResponse(): String {
                val response = StringBuilder()

                val startTime = System.currentTimeMillis()
                val timeoutMillis = 3000L // 3 detik timeout

                withContext(Dispatchers.IO) {
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead > 0) {
                            val chunk = buffer.decodeToString(0, bytesRead)
                            response.append(chunk)
                            if ('>' in chunk) break
                        }
                        // Timeout protection
                        if (System.currentTimeMillis() - startTime > timeoutMillis) {
                            response.append("TIMEOUT")
                            break
                        }

                        delay(50)
                    }
                }
                return response.toString().trim()
            }
            var allSuccess = true

            val initCmds = listOf("ATZ", "ATE0", "ATL0", "ATH0", "ATSP0", "0100")

            for (cmd in initCmds) {
                withContext(Dispatchers.IO) {
                    output.write((cmd + "\r").toByteArray())
                    output.flush()
                    delay(200)
//                    input.read(buffer)
                }
                delay(300)
                val response = readResponse()
                Log.d("OBD_INIT", "Command: $cmd → Response: $response")
                saveLogToFile(context, "OBD_INIT", "WAIT", response)
                if (response.contains("ERROR", ignoreCase = true)) {
                    allSuccess = false
                    break
                }
            }
            delay(1500)

//            _readSuccess.value = allSuccess

            while (isActive){
                try {
                    val data = obdRepository.readOBDData(input, output, context)
                    sendOBDData(data)
//                    Log.d("Data_OBD", data.toString())
//                    saveLogToFile(context, "OBD Data", "DATA", data.toString())
                } catch (e: IOException) {
                    Log.e("OBD", "error reading OBD data", e)
//                    _readSuccess.value = false
                    break
                }
                delay(200)
            }
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

}

