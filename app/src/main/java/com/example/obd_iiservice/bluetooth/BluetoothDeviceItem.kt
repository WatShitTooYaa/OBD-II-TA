package com.example.obd_iiservice.bluetooth

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BluetoothDeviceItem(
    val name : String,
    val address : String
) : Parcelable
