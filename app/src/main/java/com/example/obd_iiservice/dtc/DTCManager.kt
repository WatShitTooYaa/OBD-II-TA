package com.example.obd_iiservice.dtc

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.helper.saveLogToFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class OBDManager (
    private val bluetoothSocket: BluetoothSocket,
    ) {

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

    fun parseOBDResponse(response: String, context: Context): Map<String, String> {
        val data = mutableMapOf<String, String>()

        // Membersihkan karakter umum dan "SEARCHING..."
        var cleanResponse = response
            .replace("SEARCHING...", "", ignoreCase = true)
            .replace(" ", "") // Hapus semua spasi agar lebih mudah di-parse
            .trim()

        // Cari kemunculan terakhir dari "41" (ID respons untuk mode 01)
        // Ini membantu mengabaikan echo dan data sampah di awal string.
        val responseIndex = cleanResponse.lastIndexOf("43")
        if (responseIndex != -1) {
            cleanResponse = cleanResponse.substring(responseIndex)
        }

        // Gunakan 'when' untuk logika yang lebih bersih
        when {
            cleanResponse.startsWith("410C") -> {
                // Regex di sini tidak lagi perlu menangani spasi
                // 410C(....) -> 4 byte data
                val match = "410C([0-9A-Fa-f]{4})".toRegex().find(cleanResponse)
                if (match != null) {
                    val hexValue = match.groupValues[1]
                    val dtc = hexValue.toInt(16) / 4
                    data["DTC"] = dtc.toString()
                }
            }
            else -> {
                if (cleanResponse.isNotBlank() && !cleanResponse.contains("NODATA", true)) {
                    Log.w("OBD", "No parser for PID: $cleanResponse")
                    saveLogToFile(context, "parsing OBD", "Warning", "No parser for PID: $cleanResponse")
                }
            }
        }

//        data["Fuel"] = (data.getValue("Speed").toInt() / (data.getValue("MAF").toInt() / 14.7 / 737 * 3600)).toString()

//        onNewObdDataReceived(data)
        return data
    }


    private fun parseDTCResponse(response: String): List<DTCItem> {
        val clean = response.replace("\\s".toRegex(), "")
        Log.d("OBD", "Raw DTC response: $response")

        if (!clean.startsWith("43")) return emptyList()

        val hexData = clean.removePrefix("43")
        val dtcList = mutableListOf<DTCItem>()

        for (i in hexData.indices step 4) {
            if (i + 4 > hexData.length) break
            val code = hexData.substring(i, i + 4)
            if (code == "0000") continue
            val parsedCode = convertHexToDTC(code)
            dtcList.add(DTCItem(parsedCode, "Deskripsi tidak tersedia"))
        }

        return dtcList
    }

    private fun convertHexToDTC(hex: String): String {
        val firstChar = hex[0]
        val dtcType = when (firstChar) {
            '0', '1', '2', '3' -> "P0"
            '4', '5', '6', '7' -> "C0"
            '8', '9', 'A', 'B' -> "B0"
            'C', 'D', 'E', 'F' -> "U0"
            else -> "P0"
        }
        return dtcType + hex.substring(1)
    }
}
