package com.example.obd_iiservice.dtc

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class OBDManager(private val bluetoothSocket: BluetoothSocket) {

    private val output: OutputStream = bluetoothSocket.outputStream
    private val input: InputStream = bluetoothSocket.inputStream

    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        output.write((command + "\r").toByteArray())
        output.flush()

        val buffer = ByteArray(1024)
        val bytes = input.read(buffer)
        val rawResponse = String(buffer, 0, bytes)
        rawResponse
    }

    suspend fun getDTCs(): String {
        sendCommand("ATZ") // reset
        delay(1000)
        sendCommand("ATE0") // echo off
        delay(1000)
        sendCommand("0100") // test connection
        delay(1000)
        return sendCommand("03") // DTC request
    }
}
