package com.example.obd_iiservice

import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.IOException

class BluetoothViewModel : ViewModel() {
    private val _isConnected = MutableLiveData(false)
    val isConnected : LiveData<Boolean> get() = _isConnected

    var bluetoothSocket : BluetoothSocket? = null
        set(value) {
            field = value
            _isConnected.postValue(value != null)
        }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Disconnect failed", e)
        } finally {
            bluetoothSocket = null
        }
    }

}