package com.example.obd_iiservice.dtc

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.obd_iiservice.helper.saveLogToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
        delay(1000) // beri delay awal agar tidak tabrakan dengan polling lain

        withContext(Dispatchers.IO) {
            try {
                // Inisialisasi adapter
                val initCommands = listOf(
                    "ATZ",   // Reset ELM327
                    "ATE0",  // Echo Off
                    "ATL0",  // Linefeeds Off
                    "ATH0"   // Headers Off
                )

                for (cmd in initCommands) {
                    clearInputBuffer(input)
                    output.write("$cmd\r".toByteArray())
                    output.flush()
                    readUntilPrompt(input) // abaikan hasilnya, hanya untuk sinkronisasi
                    delay(200) // jeda agar tidak tertimpa
                }

                // Kirim command 03 untuk baca DTC
                clearInputBuffer(input)
                output.write("03\r".toByteArray())
                output.flush()
            } catch (e: Exception) {
                Log.e("OBD", "Error sending DTC command", e)
            }
        }

        val response = readUntilPrompt(input)
        Log.d("OBD", "Raw DTC response: $response")

        return cleanResponse(response)
    }


    private fun clearInputBuffer(input: InputStream) {
        try {
            while (input.available() > 0) {
                input.read() // baca dan buang karakter
            }
        } catch (e: Exception) {
            // Optional: log error
            Log.e("OBD", "Error clearing input buffer", e)
        }
    }


    fun cleanResponse(response: String): String {
        return response
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")
            .trim()
    }

    private fun readUntilPrompt(input: InputStream, timeoutMs: Long = 2000): String {
        val buffer = ByteArrayOutputStream()
        val temp = ByteArray(1024)
        val startTime = System.currentTimeMillis()

        while (true) {
            if (System.currentTimeMillis() - startTime > timeoutMs) break
            if (input.available() > 0) {
                val len = input.read(temp)
                if (len > 0) {
                    buffer.write(temp, 0, len)
                    val text = buffer.toString("UTF-8")
                    if (text.contains('>')) break
                }
            }
        }

        return buffer.toString("UTF-8")
    }

}
