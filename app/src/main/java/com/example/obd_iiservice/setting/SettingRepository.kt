package com.example.obd_iiservice.setting

import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.helper.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

interface SettingRepository {

}

class SettingRepositoryImpl @Inject constructor(
    private val preferenceManager: PreferenceManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : SettingRepository {
    val bluetoothAddress: StateFlow<String?> = preferenceManager.bluetoothAddress
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)



}