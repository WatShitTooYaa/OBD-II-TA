package com.example.obd_iiservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat

class MyForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("Background Service")
            .setContentText("Service is running in background...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)

        // Jalankan task di background thread
        Thread {
            var i = 1
            while (true) {
                // Contoh logika: print ke Logcat setiap 5 detik
                var text = "toast ke - $i"
//                android.util.Log.d("MyForegroundService", "Running in background...")
                Handler(Looper.getMainLooper()).post{
                  Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
                }
                Thread.sleep(5000)
                i++
            }
        }.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "channel_id",
                "Background Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
