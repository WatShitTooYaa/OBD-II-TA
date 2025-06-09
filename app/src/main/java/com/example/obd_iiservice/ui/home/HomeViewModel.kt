package com.example.obd_iiservice.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.obd_iiservice.main.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    suspend fun updateIsPlaying(isPlaying: Boolean){
//        _isPlaying.emit(isPlaying)
        mainRepository.updateIsPlaying(isPlaying)
    }

    suspend fun updateCurrentStreamId(id: Int?){
//        _currentStreamId.emit(id)
        mainRepository.updateCurrentStreamId(id)
    }
}