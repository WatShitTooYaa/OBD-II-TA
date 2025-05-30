package com.example.obd_iiservice.obd

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd_iiservice.helper.saveLogToFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlin.collections.iterator

@HiltViewModel
class OBDViewModel @Inject constructor(
    private val obdRepository: OBDRepository
) : ViewModel() {
    private var _obdData = MutableStateFlow<Map<String,String>>(emptyMap())
    val obdData : StateFlow<Map<String,String>> = _obdData

    private var _serviceIntent = MutableLiveData<Intent?>(null)
    val serviceIntent : LiveData<Intent?> = _serviceIntent

    private var _readSuccess = MutableStateFlow<Boolean>(false)
    val readSuccess : StateFlow<Boolean> = _readSuccess

    private var readJob : Job? = null

//    fun startReading(input: InputStream, output: OutputStream) {
    fun startReading(context: Context, input: InputStream, output: OutputStream) {
        readJob?.cancel()
        readJob = viewModelScope.launch {
            val buffer = ByteArray(1024)

            // Fungsi bantu untuk membaca respons dari ELM327 sampai tanda '>'
            suspend fun readResponse(): String {
                val response = StringBuilder()

                val startTime = System.currentTimeMillis()
                val timeoutMillis = 3000L // 3 detik timeout

                withContext(Dispatchers.IO) {
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead > 0) {
                            val chunk = buffer.decodeToString(0, bytesRead)
                            response.append(chunk)
                            if ('>' in chunk) break
                        }
                        // Timeout protection
                        if (System.currentTimeMillis() - startTime > timeoutMillis) {
                            response.append("TIMEOUT")
                            break
                        }

                        delay(50)
                    }
                }
                return response.toString().trim()
            }
                //init
//            val initCmds = listOf(
//                "ATZ",
////                "ATD",
//                "ATE0",
//                "ATL0",
//                "ATS0",
//                "ATH0",
//                "ATSP0",
////                "0100",
//            )
            var allSuccess = true
//            for (cmd in initCmds) { Command: 0100 → Response: 410088198000>
//                val response = sendCommand(cmd, input, output)
//                Log.d("OBD", "Response: $response")
//                if (response.contains("ERROR", ignoreCase = true)) {
//                    allSuccess = false
//                    break
//                }
//            }
            val initCmds = listOf("ATZ", "ATE0", "ATL0", "ATH0", "ATSP0", "0100")

            for (cmd in initCmds) {
                withContext(Dispatchers.IO) {
                    output.write((cmd + "\r").toByteArray())
                    output.flush()
                    delay(200)
//                    input.read(buffer)
                }
                delay(300)
                val response = readResponse()
                Log.d("OBD_INIT", "Command: $cmd → Response: $response")
                saveLogToFile(context, "OBD_INIT", "WAIT", response)
                if (response.contains("ERROR", ignoreCase = true)) {
                    allSuccess = false
                    break
                }
            }
            delay(1500)

            _readSuccess.value = allSuccess

            while (isActive){
                try {
                    val data = obdRepository.readOBDData(input, output, context)
                    sendOBDData(data)
//                    Log.d("Data_OBD", data.toString())
                    saveLogToFile(context, "OBD Data", "DATA", data.toString())
//                    _obdData.value =
                    _obdData.update { oldData ->
                        oldData.toMutableMap().apply {
                            putAll(data)
                        }
                    }
//                    _readSuccess.update { oldData ->
//                        oldData
//                    }
                } catch (e: IOException) {
                    Log.e("OBD", "error reading OBD data", e)
                    _readSuccess.value = false
                    break
                }
                delay(500)
            }
        }
    }

    private suspend fun sendCommand(
        cmd: String,
        input: InputStream,
        output: OutputStream
    ): String = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        val response = StringBuilder()

        // Flush sebelum kirim
        output.flush()
        input.skip(input.available().toLong()) // bersihkan buffer lama

        // Kirim perintah
        output.write((cmd + "\r").toByteArray())
        output.flush()

        // Baca respons sampai selesai
        var len: Int
        val timeout = 2000L // maksimal tunggu 2 detik
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            if (input.available() > 0) {
                len = input.read(buffer)
                val part = String(buffer, 0, len)
                response.append(part)

                // Cek jika respons sudah selesai (ELM biasanya akhiri dengan '>')
                if (part.contains(">")) break
            } else {
                delay(50) // hindari busy loop
            }
        }

        return@withContext response.toString().replace("\r", "").replace(">", "").trim()
    }


    private fun sendOBDData(data : Map<String, String>) {
//        viewModelScope.launch {
//            obdRepository.updateData(data)
//        }
        viewModelScope.launch {
            val oldData = obdRepository.obdData.first()

            val mergedData = oldData.toMutableMap().apply {
                for ((key, value ) in data) {
                    if (value.isNotBlank()) {
                        this[key]= value
                    }
                }
            }
            obdRepository.updateData(mergedData)
        }
    }

    suspend fun updateBluetoothConnection(connect: Boolean){
        obdRepository.updateBluetoothConnection(connect)
    }

    fun stopReading() {
        _readSuccess.value = false
        readJob?.cancel()
    }

    fun changeServiceIntent(intent: Intent) {
        _serviceIntent.value = intent
    }
}