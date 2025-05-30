package com.example.obd_iiservice

import android.app.Application
import android.media.SoundPool
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    lateinit var soundPool: SoundPool
    var beepSoundId: Int = 0

    override fun onCreate() {
        super.onCreate()
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        beepSoundId = soundPool.load(this, R.raw.beep, 1)
    }
}