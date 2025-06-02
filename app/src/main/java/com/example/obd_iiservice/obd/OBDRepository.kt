package com.example.obd_iiservice.obd

import android.content.Context
import android.util.Log
import com.example.obd_iiservice.helper.saveLogToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import javax.inject.Inject

interface OBDRepository {
    suspend fun readOBDData(input: InputStream, output: OutputStream, context: Context) : Map<String, String>
    suspend fun updateData(data: Map<String, String>)
    suspend fun updateBluetoothConnection(connect: Boolean)
    var obdData : StateFlow<Map<String, String>>
    var isBluetoothConnected : StateFlow<Boolean>
//    suspend fun getDtcCodes(): List<String>
    suspend fun sendCommand(
    input : InputStream,
    output: OutputStream,
    command: String
): String
}

class OBDRepositoryImpl @Inject constructor() : OBDRepository {

    private val _obdData = MutableStateFlow<Map<String, String>>(emptyMap())
    override var obdData: StateFlow<Map<String, String>> = _obdData.asStateFlow()

    private val _isBluetoothConnected = MutableStateFlow<Boolean>(false)
    override var isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()

    private val pidList = listOf("010C", "010D", "0111", "0105", "0110")

    override suspend fun readOBDData(
        input: InputStream,
        output: OutputStream,
        context: Context
    ): Map<String, String> {
        val data = mutableMapOf<String, String>()
        for (pid in pidList) {
            if (isBluetoothConnected.first() == false) break
            withContext(Dispatchers.IO) {
                try {
                    output.write("$pid\r".toByteArray())
                    output.flush()

                    delay(100) // beri waktu respons

                    val rawResponseFromAdapter = readUntilPrompt(input)
//                    Log.d("OBD_V21_RAW", "PID: $pid, Raw Response: '$rawResponseFromAdapter'")
//                    val response = readUntilPrompt(input)
                    val response = rawResponseFromAdapter
                        .replace(
                            "SEARCHING...",
                            "",
                            ignoreCase = true
                        )
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim()

//                    saveLogToFile(context, "OBD_RESPONSE_Repo", "data", response)

                    when (pid) {
                        "010C" -> parseRegex(
                            response,
                            "41 0C ([0-9A-Fa-f]{2}) ([0-9A-Fa-f]{2})"
                        )?.let {
                            val rpm = (it[0] * 256 + it[1]) / 4
                            data["Rpm"] = rpm.toString()
//                            saveLogToFile(context, "RPM Repo", "data", rpm.toString())
                        }

                        "010D" -> parseRegex(response, "41 0D ([0-9A-Fa-f]{2})")?.let {
                            val speed = it[0]
                            data["Speed"] = speed.toString()
//                            saveLogToFile(context, "speed repo", "data", speed.toString())
                        }

                        "0111" -> parseRegex(response, "41 11 ([0-9A-Fa-f]{2})")?.let {
                            val throttle = (it[0] * 100) / 255
                            data["Throttle"] = throttle.toString()
//                            saveLogToFile(context, "throttle repo", "data", throttle.toString())
                        }

                        "0105" -> parseRegex(response, "41 05 ([0-9A-Fa-f]{2})")?.let {
                            val temp = it[0] - 40
                            data["Temp"] = temp.toString()
//                            saveLogToFile(context, "temp repo", "data", temp.toString())
                        }

                        "0110" -> parseRegex(
                            response,
                            "41 10 ([0-9A-Fa-f]{2}) ([0-9A-Fa-f]{2})"
                        )?.let {
                            val maf = (it[0] * 256 + it[1]) / 100.0
                            data["Maf"] = "%.2f".format(Locale.US, maf)
//                            saveLogToFile(context, "maf repo", "data", maf.toString())
                        }

                        else -> {
                            Log.w("OBD", "No parser for PID: $pid")
                            saveLogToFile(
                                context,
                                "parsing",
                                "Error",
                                "No parser implemented for PID: $pid"
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e("OBD", "Error while reading PID: $pid", e)
                    saveLogToFile(
                        context,
                        "reading pid",
                        "Error",
                        "Error while reading PID: $pid,\n $e"
                    )
                    updateBluetoothConnection(false)
                }

            }
        }
        return data
    }

    private fun readUntilPrompt(input: InputStream): String {
        val buffer = ByteArrayOutputStream()
        val temp = ByteArray(1024)

        while (true) {
            val len = input.read(temp)
            if (len == -1) break
            buffer.write(temp, 0, len)
            val text = buffer.toString("UTF-8")
            if (text.contains('>')) break
        }

        return buffer.toString("UTF-8")
    }

    private fun parseRegex(response: String, pattern: String): List<Int>? {
        val regex = Regex(pattern)
        val match = regex.find(response) ?: return null
        return match.groupValues.drop(1).mapNotNull {
            try {
                it.toInt(16)
            } catch (e: Exception) {
                null
            }
        }
    }


    override suspend fun sendCommand(
        input : InputStream,
        output: OutputStream,
        command: String
    ): String = withContext(Dispatchers.IO) {
        output.write((command + "\r").toByteArray())
        output.flush()

        val buffer = ByteArray(1024)
        val bytes = input.read(buffer)
        val rawResponse = String(buffer, 0, bytes)
        rawResponse
    }

    suspend fun getDTCs(input: InputStream, output: OutputStream): String {
        sendCommand(input, output, "ATZ") // reset
        delay(1000)
        sendCommand(input, output, "ATE0") // echo off
        delay(300)
        sendCommand(input, output, "0100") // test connection
        delay(300)
        return sendCommand(input, output, "03") // DTC request
    }

    override suspend fun updateData(data: Map<String, String>) {
        _obdData.emit(data)
    }

    override suspend fun updateBluetoothConnection(connect: Boolean) {
        _isBluetoothConnected.emit(connect)
    }
}