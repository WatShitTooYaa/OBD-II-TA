package com.example.obd_iiservice.threshold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd_iiservice.helper.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
//        viewModelScope.launch { preferenceManager.saveRpmThreshold(rpm) }
        thresholdRepository.saveRpmThreshold(rpm)
    }

    fun saveSpeedThreshold(speed: Int) {
//        viewModelScope.launch { preferenceManager.saveSpeedThreshold(speed) }
        thresholdRepository.saveSpeedThreshold(speed)
    }

    fun saveThrottleThreshold(throttle: Int) {
//        viewModelScope.launch { preferenceManager.saveThrottleThreshold(throttle) }
        thresholdRepository.saveThrottleThreshold(throttle)
    }

    fun saveTempThreshold(temp: Int) {
//        viewModelScope.launch { preferenceManager.saveTempThreshold(temp)}
        thresholdRepository.saveTempThreshold(temp)
    }

    fun saveMafThreshold(maf : Double) {
//        viewModelScope.launch { preferenceManager.saveMafThreshold(maf) }
        thresholdRepository.saveMafThreshold(maf)

    }
}