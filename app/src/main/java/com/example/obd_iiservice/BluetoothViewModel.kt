package com.example.obd_iiservice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val repository: BluetoothRepository
) : ViewModel() {
    private val _isConnected = MutableLiveData(false)
    val isConnected : LiveData<Boolean> get() = _isConnected

    var bluetoothSocket : BluetoothSocket? = null
        set(value) {
            field = value
            _isConnected.postValue(value != null)
        }

    fun connect(address : String) {
        viewModelScope.launch {
            bluetoothSocket = repository.connectToDevice(address)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        address: String,
        onSuccess : () -> Unit,
        onError : (String) -> Unit
        ) {
        viewModelScope.launch {
            try {
                bluetoothSocket = repository.connectToDevice(address)
                if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                    onSuccess()
                } else {
                    onError("Socket is null or not connected")
                }
            } catch (e : IOException) {
                bluetoothSocket = null
                onError(e.message ?: "connection failed")
            }
        }
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