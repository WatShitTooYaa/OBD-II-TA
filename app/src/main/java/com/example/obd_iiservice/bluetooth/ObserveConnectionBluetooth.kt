package com.example.obd_iiservice.bluetooth

import kotlinx.coroutines.Job

data class ObserveConnectionBluetooth(
    val address : String?,
    val isConnected: Boolean,
    val isAuto: Boolean,
    val reconnectingJob: Job?,
)
