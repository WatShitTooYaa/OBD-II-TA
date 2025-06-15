package com.example.obd_iiservice.setting.ui.bluetooth

import com.example.obd_iiservice.bluetooth.BluetoothConnectionState
import kotlinx.coroutines.Job

data class ReconnectingBluetoothData(
    val reconnectingJob: Job?,
    val address : String?,
    val connection: BluetoothConnectionState,
    val isAuto: Boolean,
)
