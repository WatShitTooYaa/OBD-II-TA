package com.example.obd_iiservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OBDForegroundService : Service() {

    @Inject lateinit var obdRepositoryImpl: OBDRepositoryImpl
    private val notificationId = 1
    private val channelId = "obd_channel"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, buildNotification(emptyMap()))

        //  Observe
        serviceScope.launch {
            obdRepositoryImpl.obdData.collect{ data ->
                val notification = buildNotification(data)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(notificationId, notification)
            }
        }
    }

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val notification: Notification = NotificationCompat.Builder(this, "channel_id")
//            .setContentTitle("Background Service")
//            .setContentText("Service is running in background...")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .build()
//
//        startForeground(1, notification)
//
//        // Jalankan task di background thread
//        Thread {
//            var i = 1
//            while (true) {
//                // Contoh logika: print ke Logcat setiap 5 detik
//                var text = "toast ke - $i"
////                android.util.Log.d("MyForegroundService", "Running in background...")
//                Handler(Looper.getMainLooper()).post{
//                  Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
//                }
//                Thread.sleep(5000)
//                i++
//            }
//        }.start()
//
//        return START_STICKY
//    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
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
//            val serviceChannel = NotificationChannel(
//                "channel_id",
//                "Background Service Channel",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//            val manager = getSystemService(NotificationManager::class.java)
//            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
