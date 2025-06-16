package com.example.obd_iiservice.setting.ui.threshold

import com.example.obd_iiservice.helper.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

interface ThresholdRepository {
    val thresholdData : Flow<ThresholdConfig>
    fun saveRpmThreshold(rpm : Int)
    fun saveSpeedThreshold(speed : Int)
    fun saveThrottleThreshold(throttle : Int)
    fun saveTempThreshold(temp : Int)
    fun saveMafThreshold(maf : Double)
}

class ThresholdRepositoryImpl @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ThresholdRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    val rpmThreshold: StateFlow<Int> = preferenceManager.rpmThreshold
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    val speedThreshold: StateFlow<Int> = preferenceManager.speedThreshold
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    val throttleThreshold: StateFlow<Int> = preferenceManager.throttleThreshold
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    val tempThreshold: StateFlow<Int> = preferenceManager.tempThreshold
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    val mafThreshold : StateFlow<Double> = preferenceManager.mafThreshold
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0.0)

    override val thresholdData = combine(
        rpmThreshold, speedThreshold, throttleThreshold, tempThreshold, mafThreshold
    ) {rpm, speed, throttle, temp, maf ->
        ThresholdConfig(
            rpm,
            speed,
            throttle,
            temp,
            maf
        )
    }

    override fun saveRpmThreshold(rpm: Int) {
        scope.launch { preferenceManager.saveRpmThreshold(rpm) }
    }

    override fun saveSpeedThreshold(speed: Int) {
        scope.launch { preferenceManager.saveSpeedThreshold(speed) }
    }

    override fun saveThrottleThreshold(throttle: Int) {
        scope.launch { preferenceManager.saveThrottleThreshold(throttle) }
    }

    override fun saveTempThreshold(temp: Int) {
        scope.launch { preferenceManager.saveTempThreshold(temp)}
    }

    override fun saveMafThreshold(maf : Double) {
        scope.launch { preferenceManager.saveMafThreshold(maf) }
    }
}