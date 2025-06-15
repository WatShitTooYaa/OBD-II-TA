package com.example.obd_iiservice.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.helper.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

interface BluetoothRepository {
    suspend fun connectToDevice(address: String): BluetoothSocket?
    suspend fun updateBluetoothSocket(socket: BluetoothSocket?)
    suspend fun updateConnectionState(state: BluetoothConnectionState)
    suspend fun checkDataForConnecting() : Boolean
    suspend fun saveBluetoothAddress(address: String?)
    val bluetoothSocket: StateFlow<BluetoothSocket?>
    val connectionState : StateFlow<BluetoothConnectionState>
    val bluetoothAddress: StateFlow<String?>
}

class BluetoothRepositoryImpl @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val preferenceManager: PreferenceManager,
    @ApplicationScope private val applicationScope: CoroutineScope
) : BluetoothRepository {

    private var _bluetoothAddress: StateFlow<String?> = preferenceManager.bluetoothAddress
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)
    override val bluetoothAddress: StateFlow<String?> = _bluetoothAddress

    private var _bluetoothSocket = MutableStateFlow<BluetoothSocket?>(null)
    override val bluetoothSocket : StateFlow<BluetoothSocket?> = _bluetoothSocket

    private val _connectionState = MutableStateFlow(BluetoothConnectionState.IDLE)
    override val connectionState: StateFlow<BluetoothConnectionState> = _connectionState

    @SuppressLint("MissingPermission")
    override suspend fun connectToDevice(address: String): BluetoothSocket? {
        val device = bluetoothAdapter.getRemoteDevice(address)
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        return withContext(Dispatchers.IO) {
            val socket = device.createRfcommSocketToServiceRecord(uuid)
//            val socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter.cancelDiscovery()
            try {
                socket.connect()
                Log.d("Bluetooth", "Socket connected successfully")
                return@withContext socket
            } catch (e: IOException) {
                Log.e("Bluetooth", "Socket connection failed: ${e.message}")
                socket.close()
                return@withContext null
            }
        }
    }

    override suspend fun saveBluetoothAddress(address: String?) {
        applicationScope.launch { preferenceManager.saveBluetoothAddress(address) }
    }

    override suspend fun updateBluetoothSocket(socket: BluetoothSocket?) {
        _bluetoothSocket.emit(socket)
    }

    override suspend fun updateConnectionState(state: BluetoothConnectionState) {
        _connectionState.emit(state)
    }

    override suspend fun checkDataForConnecting() : Boolean {
        return preferenceManager.checkDataForMQTTConnection()
    }

}


//class BluetoothRepository(private val adapter: BluetoothAdapter) {
//    @SuppressLint("MissingPermission")
//    suspend fun connectToDevice(address: String): BluetoothSocket? = withContext(Dispatchers.IO) {
//        val device = adapter.getRemoteDevice(address)
//        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
//
//        val socket = device.createRfcommSocketToServiceRecord(uuid)
//        adapter.cancelDiscovery()
//        try {
//            socket.connect()
//            return@withContext socket
//        } catch (e : IOException) {
//            e.printStackTrace()
//            return@withContext null
//        }
//    }
//}