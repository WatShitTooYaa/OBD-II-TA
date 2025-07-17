package com.example.obd_iiservice.setting.ui.threshold

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThresholdViewModel @Inject constructor(
    private val thresholdRepository: ThresholdRepository
) : ViewModel() {
//    val thresholdKey = mutableMapOf<ThresholdConfig>()
    val thresholdKey = mutableMapOf<String, Int>()
//
//
//    val rpmThreshold: StateFlow<Int> = preferenceManager.rpmThreshold
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
//
//    val speedThreshold: StateFlow<Int> = preferenceManager.speedThreshold
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
//
//    val throttleThreshold: StateFlow<Int> = preferenceManager.throttleThreshold
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
//
//    val tempThreshold: StateFlow<Int> = preferenceManager.tempThreshold
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
//
//    val mafThreshold : StateFlow<Double> = preferenceManager.mafThreshold
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val thresholdData = thresholdRepository.thresholdData

    fun saveRpmThreshold(rpm: Int) {
        thresholdRepository.saveRpmThreshold(rpm)
    }

    fun saveSpeedThreshold(speed: Int) {
        thresholdRepository.saveSpeedThreshold(speed)
    }

    fun saveThrottleThreshold(throttle: Int) {
        thresholdRepository.saveThrottleThreshold(throttle)
    }

    fun saveTempThreshold(temp: Int) {
        thresholdRepository.saveTempThreshold(temp)
    }

    fun saveMafThreshold(maf : Double) {
        thresholdRepository.saveMafThreshold(maf)
    }

    fun saveFuelThreshold(fuel : Int) {
        thresholdRepository.saveFuelThreshold(fuel)
    }
}