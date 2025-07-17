package com.example.obd_iiservice.setting.ui.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd_iiservice.bluetooth.BluetoothConnectionState
import com.example.obd_iiservice.bluetooth.BluetoothRepository
import com.example.obd_iiservice.bluetooth.ObserveConnectionBluetooth
import com.example.obd_iiservice.obd.OBDRepository
import com.example.obd_iiservice.obd.ServiceState
import com.example.obd_iiservice.setting.ui.mqtt.MqttRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val obdRepository: OBDRepository,
    private val mqttRepository: MqttRepository
) : ViewModel() {
//    private val _isConnected = MutableLiveData(false)
//    val isConnected : LiveData<Boolean> get() = _isConnected

    val serviceState = obdRepository.serviceState


//    private var _reconnectingJob = MutableStateFlow<Job?>(null)
    val reconnectingJob : StateFlow<Job?> = bluetoothRepository.reconnectingJob

    val bluetoothAddress = bluetoothRepository.bluetoothAddress
    val connectionState = bluetoothRepository.connectionState
    val isAutoRecon = mqttRepository.mqttAutoReconnect

    val combineToReconnecting = combine(
        reconnectingJob,
        bluetoothAddress,
        connectionState,
        isAutoRecon
    ){ job, address, state, isAuto ->
        ReconnectingBluetoothData(
            job,
            address,
            state,
            isAuto
        )
    }

//    val isConnected : StateFlow<Boolean> = obdRepository.isBluetoothConnected

    private var _isReceiverRegistered = MutableStateFlow<Boolean>(false)
    val isReceiverRegistered : StateFlow<Boolean> = _isReceiverRegistered

//    private var _bluetoothSocket = MutableStateFlow<BluetoothSocket?>(null)
    val bluetoothSocket: StateFlow<BluetoothSocket?> = bluetoothRepository.bluetoothSocket


    private var _previousAddress = MutableStateFlow<String?>(null)
    val previousAddress: StateFlow<String?> = _previousAddress

    private var _previousIsAuto = MutableStateFlow<Boolean?>(false)
    val previousIsAuto : StateFlow<Boolean?> = _previousIsAuto


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
                updateBluetoothSocket(bluetoothRepository.connectToDevice(address))
                if (bluetoothSocket.first() != null && bluetoothSocket.value?.isConnected == true) {
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
        bluetoothRepository.updateBluetoothSocket(socket)
//        _isConnected.value = socket?.isConnected == true
//        _isConnected.value = if (socket != null && socket.isConnected){
//            true
//        } else {
//            false
//        }

    }

    fun changeAutoReconnect(isAuto: Boolean){
        mqttRepository.saveMqttAutoRecon(isAuto)
    }

    suspend fun updateServiceState(newState: ServiceState){
        obdRepository.updateServiceState(newState)
    }

    suspend fun saveBluetoothAddress(address: String?){
        bluetoothRepository.saveBluetoothAddress(address)
    }

    suspend fun updateConnectionState(state: BluetoothConnectionState){
        bluetoothRepository.updateConnectionState(state)
    }

    suspend fun checkDataForConnecting() : Boolean{
        return bluetoothRepository.checkDataForConnecting()
    }

    suspend fun updateReconnectingJob(job: Job?) {
       bluetoothRepository.updateReconnectingJob(job)
    }

    suspend fun updatePreviousAddress(address: String) {
        _previousAddress.emit(address)
    }

    suspend fun updatePreviousIsAuto(isAuto: Boolean) {
        _previousIsAuto.emit(isAuto)
    }
}