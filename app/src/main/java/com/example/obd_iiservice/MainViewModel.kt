package com.example.obd_iiservice

import androidx.lifecycle.ViewModel
import com.example.obd_iiservice.main.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
//    application: Application,
    private val mainRepository: MainRepository
) : ViewModel() {

    suspend fun updateIsPlaying(isPlaying: Boolean){
//        _isPlaying.emit(isPlaying)
        mainRepository.updateIsPlaying(isPlaying)
    }

    suspend fun updateCurrentStreamId(id: Int?){
//        _currentStreamId.emit(id)
        mainRepository.updateCurrentStreamId(id)
    }

}