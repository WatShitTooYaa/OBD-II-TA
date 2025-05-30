package com.example.obd_iiservice

import android.app.Application
import android.media.SoundPool
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : ViewModel() {

    private val app = application as App

    val soundPool = app.soundPool
    val beepSoundId = app.beepSoundId

//    private var _soundPool = MutableStateFlow<SoundPool?>(null)
//    val soundPool : StateFlow<SoundPool?> = _soundPool
//
//    private var _beepSoundId = MutableStateFlow<Int?>(null)
//    val beepSoundId : StateFlow<Int?> = _beepSoundId

    private var _isPlaying = MutableStateFlow<Boolean>(false)
    val isPlaying : StateFlow<Boolean> = _isPlaying

    private var _currentStreamId = MutableStateFlow<Int?>(null)
    val currentStreamId : StateFlow<Int?> = _currentStreamId

    private var _isThresholdExceeded = MutableStateFlow<Boolean>(false)
    val isThresholdExceeded : StateFlow<Boolean> = _isThresholdExceeded


//    suspend fun updateSoundPool(pool: SoundPool){
//        _soundPool.emit(pool)
//    }
//
//    suspend fun updateSoundPoolId(id: Int){
//        _beepSoundId.emit(id)
//    }

    suspend fun updateIsPlaying(isPlaying: Boolean){
        _isPlaying.emit(isPlaying)
    }

    suspend fun updateCurrentStreamId(id: Int?){
        _currentStreamId.emit(id)
    }

    suspend fun updateIsThresholdExceeded(bool: Boolean){
        _isThresholdExceeded.emit(bool)
    }
}