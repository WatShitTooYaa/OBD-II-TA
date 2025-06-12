package com.example.obd_iiservice.setting.ui.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd_iiservice.bluetooth.BluetoothRepository
import com.example.obd_iiservice.obd.OBDRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothrepository: BluetoothRepository,
    private val obdRepository: OBDRepository
) : ViewModel() {
//    private val _isConnected = MutableLiveData(false)
//    val isConnected : LiveData<Boolean> get() = _isConnected
    val isConnected : StateFlow<Boolean> = obdRepository.isBluetoothConnected

    private var _isReceiverRegistered = MutableStateFlow<Boolean>(false)
    val isReceiverRegistered : StateFlow<Boolean> = _isReceiverRegistered

//    private var _bluetoothSocket = MutableStateFlow<BluetoothSocket?>(null)
    val bluetoothSocket: StateFlow<BluetoothSocket?> = bluetoothrepository.bluetoothSocket

    private var _reconnectingJob = MutableStateFlow<Job?>(null)
    val reconnectingJob : StateFlow<Job?> = _reconnectingJob.asStateFlow()

    private var _previousAddress = MutableStateFlow<String?>(null)
    val previousAddress: StateFlow<String?> = _previousAddress

    private var _previousIsAuto = MutableStateFlow<Boolean?>(false)
    val previousIsAuto : StateFlow<Boolean?> = _previousIsAuto

//    var bluetoothSocket : BluetoothSocket? = null
//        set(value) {
//            field = value
////            _isConnected.postValue(value != null)
//            _isConnected.postValue(value?.isConnected == true)
//        }

//    fun connect(address : String) {
//        viewModelScope.launch {
//            bluetoothSocket = repository.connectToDevice(address)
//        }
//    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun connectToDeviceSuspend(address: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            connectToDevice(
                address = address,
                onSuccess = {
                    continuation.resume(true) {} // lanjutkan coroutine dengan hasil true
                },
                onError = { error ->
                    continuation.resume(false) {} // lanjutkan coroutine dengan hasil false
                }
            )
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
//                bluetoothSocket = repository.connectToDevice(address)
                updateBluetoothSocket(bluetoothrepository.connectToDevice(address))
                if (bluetoothSocket.value != null && bluetoothSocket.value?.isConnected == true) {
                    onSuccess()
                } else {
                    onError("Socket is null or not connected")
                }
            } catch (e : IOException) {
//                bluetoothSocket = null
                updateBluetoothSocket(null)
                onError(e.message ?: "connection failed")
            }
        }
    }


    fun disconnect() {
        try {
//            bluetoothSocket?.close()
            bluetoothSocket.value.takeIf { it?.isConnected == true }?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Disconnect failed", e)
        } finally {
//            bluetoothSocket = null
            viewModelScope.launch {
                updateBluetoothSocket(null)
            }
        }
    }

    suspend fun changeIsReceiverRegistered(isRegistered : Boolean){
//        _isReceiverRegistered.value = isRegistered
        _isReceiverRegistered.emit(isRegistered)
    }

    suspend fun updateBluetoothSocket(socket: BluetoothSocket?) {
//        _bluetoothSocket.value = socket
        bluetoothrepository.updateBluetoothSocket(socket)
//        _isConnected.value = socket?.isConnected == true
//        _isConnected.value = if (socket != null && socket.isConnected){
//            true
//        } else {
//            false
//        }

    }

    suspend fun updateReconnectingJob(job: Job?) {
        _reconnectingJob.value?.cancel()
        _reconnectingJob.emit(job)
    }

    suspend fun updatePreviousAddress(address: String) {
        _previousAddress.emit(address)
    }

    suspend fun updatePreviousIsAuto(isAuto: Boolean) {
        _previousIsAuto.emit(isAuto)
    }
}