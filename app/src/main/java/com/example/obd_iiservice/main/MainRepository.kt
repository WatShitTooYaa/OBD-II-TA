package com.example.obd_iiservice.main

import android.app.Application
import android.media.SoundPool
import com.example.obd_iiservice.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

interface MainRepository {
    suspend fun updateIsPlaying(isPlaying: Boolean)
    suspend fun updateCurrentStreamId(id: Int?)
    val soundPool: SoundPool
    val beepSoundId: Int
    val isPlaying: StateFlow<Boolean>
    val currentStreamId: StateFlow<Int?>
}

class MainRepositoryImpl @Inject constructor(
    application: Application
) : MainRepository {
    private val app = application as App

    override val soundPool = app.soundPool
    override val beepSoundId = app.beepSoundId

    private var _isPlaying = MutableStateFlow<Boolean>(false)
    override val isPlaying : StateFlow<Boolean> = _isPlaying

    private var _currentStreamId = MutableStateFlow<Int?>(null)
    override val currentStreamId : StateFlow<Int?> = _currentStreamId

    override suspend fun updateIsPlaying(isPlaying: Boolean){
        _isPlaying.emit(isPlaying)
    }

    override suspend fun updateCurrentStreamId(id: Int?){
        _currentStreamId.emit(id)
    }
}