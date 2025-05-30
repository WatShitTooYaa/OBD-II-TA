package com.example.obd_iiservice.obd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.obd_iiservice.helper.MqttHelper
import com.example.obd_iiservice.R
import com.example.obd_iiservice.helper.PreferenceManager
import com.example.obd_iiservice.helper.saveLogToFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class OBDForegroundService : Service() {

//    @Inject lateinit var obdRepositoryImpl: OBDRepositoryImpl
    @Inject lateinit var obdRepository: OBDRepository
    @Inject lateinit var preferenceManager: PreferenceManager
    private lateinit var mqttHelper: MqttHelper
    private val notificationId = 1
    private val channelId = "obd_channel"

//    lateinit var private val MqttHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            val mqttConfig = preferenceManager.getCurrentConfig()
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
                    serviceScope.launch {
                        obdRepository.obdData.collect { data ->
                            val notification = buildNotification(data)
                            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            val jsonData = JSONObject(data).toString()
                            mqttHelper.publish(mqttConfig.topic!!, jsonData)

                            manager.notify(notificationId, notification)
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
            createNotificationChannel()
            startForeground(notificationId, buildNotification(emptyMap()))
        }


        //  Observe
//        serviceScope.launch {
//            obdRepository.obdData.collect{ data ->
//                val notification = buildNotification(data)
//                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//                val jsonData = JSONObject(data).toString()
//                mqttHelper.publish("obd_fate", jsonData)
//
//                manager.notify(notificationId, notification)
//            }
//        }
    }


    private fun buildNotification(data : Map<String, String>) : Notification {
        val contextText = data.entries.joinToString("\n") {
            "${it.key} : ${it.value}"
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("OBD II Data")
            .setContentText("Live Data")
            .setStyle(NotificationCompat.BigTextStyle().bigText(contextText))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "OBD Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }



    override fun onDestroy() {
        serviceScope.cancel()
        mqttHelper.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
