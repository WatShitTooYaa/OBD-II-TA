package com.example.obd_iiservice.obd

//import kotlinx.coroutines.NonCancellable.isActive

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.helper.PreferenceManager
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.internet.NetworkConnectivityObserver
import com.example.obd_iiservice.internet.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import javax.inject.Inject

// Definisikan status yang mungkin untuk service
enum class ServiceState {
    RUNNING,
    STOPPED
}

// Definisi Job OBD yang dilakukan
enum class OBDJobState {
    FREE,
    READING,
    CHECK_ENGINE,
    ERROR
}

// Definisi Job OBD yang dilakukan
enum class MQTTJobState {
    FREE,
    RUNNING,
    ERROR
}

interface OBDRepository {
//    suspend fun readOBDData(input: InputStream, output: OutputStream, context: Context) : Map<String, String>
    suspend fun updateData(data: Map<String, String>)
    suspend fun updateBluetoothConnection(connect: Boolean)
//    suspend fun updateDoingJob(doing: Boolean)
    // Fungsi BARU: Menghasilkan Flow berisi respons mentah
    fun listenForResponses(input: InputStream): Flow<String>
    // Fungsi parsing bisa kita buat public agar bisa diakses dari luar
    fun parseOBDResponse(response: String, context: Context): Map<String, String>
    suspend fun sendCommand(output: OutputStream, command: String) : Boolean
    // Fungsi untuk service agar bisa mengupdate statusnya
    suspend fun updateServiceState(newState: ServiceState)
    // Fungsi untuk mengubah state obd
    suspend fun updateOBDJobState(obdJobState: OBDJobState)
    // Fungsi untuk mengubah state mqtt
    suspend fun updateMQTTJobState(mqttJobState: MQTTJobState)

    suspend fun checkDataForMQTTConnection() : Boolean

    // StateFlow yang bisa diamati oleh UI
    val serviceState: StateFlow<ServiceState>
    var obdData : StateFlow<Map<String, String>>
//    var isBluetoothConnected : StateFlow<Boolean>
    val networkStatus: Flow<NetworkStatus>
//    val isDoingJob : StateFlow<Boolean>
    val obdJobState : StateFlow<OBDJobState>
    val mqttJobState : StateFlow<MQTTJobState>
    val obdItemsState : StateFlow<List<OBDItem>>
}

class OBDRepositoryImpl @Inject constructor(
    private val preferenceManager: PreferenceManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context
) : OBDRepository {
    private val observer = NetworkConnectivityObserver(context)
    override val networkStatus = observer.networkStatus

//    private var _isDoingJob = MutableStateFlow<Boolean>(false)
//    override val isDoingJob: StateFlow<Boolean> = _isDoingJob.asStateFlow()

    private var _serviceState = MutableStateFlow<ServiceState>(ServiceState.STOPPED)
    override val serviceState: StateFlow<ServiceState> = _serviceState

    private var _obdJobState = MutableStateFlow<OBDJobState>(OBDJobState.FREE)
    override val obdJobState : StateFlow<OBDJobState> = _obdJobState

    private var _mqttJobState = MutableStateFlow<MQTTJobState>(MQTTJobState.FREE)
    override val mqttJobState: StateFlow<MQTTJobState> = _mqttJobState

    private val delayResponse : StateFlow<Long> = preferenceManager.delayResponse
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), 100)


    private val _obdData = MutableStateFlow<Map<String, String>>(emptyMap())
    override var obdData: StateFlow<Map<String, String>> = _obdData.asStateFlow()

    init {
        // 3. Luncurkan coroutine satu kali saat repository dibuat
        applicationScope.launch {
            // 4. Ambil nilai PERTAMA dari setiap flow secara suspend.
            //    .first() akan menunggu sampai nilai pertama tersedia lalu berhenti.
            val rpm = preferenceManager.rpmThreshold.first()
            val speed = preferenceManager.speedThreshold.first()
            val throttle = preferenceManager.throttleThreshold.first()
            val temp = preferenceManager.tempThreshold.first()
            val maf = preferenceManager.mafThreshold.first()
            val fuel = preferenceManager.fuelThreshold.first()

            // 5. Buat list final setelah semua nilai didapat
            val finalList = listOf(
                OBDItem("RPM", "0", "rpm", "0", "10000", threshold = rpm.toString()),
                OBDItem("Speed", "0", "km/h", "0", "250", threshold = speed.toString()),
                OBDItem("Throttle", "0", "%", "0", "100", threshold = throttle.toString()),
                OBDItem("Temperature", "30", "°C", "30", "140", threshold = temp.toString()),
                OBDItem("MAF", "0", "g/s", "0", "120", threshold = maf.toString()),
                OBDItem("Fuel", "0", "Km/L", "0", "30", threshold = fuel.toString()),
            )

            // 6. Update StateFlow dengan list final. Ini hanya terjadi SEKALI.
            _obdItemsState.value = finalList
        }
    }

    // Fungsi helper untuk membuat list default saat inisialisasi
    private fun createDefaultObdList(): List<OBDItem> {
        return listOf(
            OBDItem("RPM", "0", "rpm", "0", "7000", threshold = "0"),
            OBDItem("Speed", "0", "km/h", "0", "250", threshold = "0"),
            OBDItem("Throttle", "0", "%", "0", "100", threshold = "0"),
            OBDItem("Temperature", "30", "°C", "30", "140", threshold = "0"),
            OBDItem("MAF", "0", "g/s", "0", "120", threshold = "0"),
            OBDItem("Fuel", "0", "Km/L", "0", "50", threshold = "0"),
        )
    }

    val obdList = listOf(
        OBDItem(
            "RPM",
            "0",
            "rpm",
            startValue = "0",
            endValue = "10000",
            threshold = preferenceManager.rpmThreshold.toString()
        ),
        OBDItem(
            "Speed",
            "0",
            "km/h",
            startValue = "0",
            endValue = "250",
            threshold = preferenceManager.speedThreshold.toString()
        ),
        OBDItem(
            "Throttle",
            "0",
            "%",
            startValue = "0",
            endValue = "100",
            threshold = preferenceManager.throttleThreshold.toString()
        ),
        OBDItem(
            "Temperature",
            "30",
            "°C",
            startValue = "30",
            endValue = "140",
            threshold = preferenceManager.tempThreshold.toString()
        ),
        OBDItem(
            "MAF",
            "0",
            "g/s",
            startValue = "0",
            endValue = "120",
            threshold = preferenceManager.mafThreshold.toString()
        ),
        OBDItem(
            "Fuel",
            "0",
            "Km/L",
            startValue = "0",
            endValue = "30",
            threshold = preferenceManager.fuelThreshold.toString()
        ),
    )
    // StateFlow internal yang menyimpan List<OBDItem> lengkap
    private val _obdItemsState = MutableStateFlow<List<OBDItem>>(createDefaultObdList())

    // StateFlow publik yang akan diobservasi oleh Fragment
    override val obdItemsState: StateFlow<List<OBDItem>> = _obdItemsState.asStateFlow()

    private var _serviceIntent = MutableLiveData<Intent?>(null)
    val serviceIntent : LiveData<Intent?> = _serviceIntent

    //gak kangge
    private val _isBluetoothConnected = MutableStateFlow<Boolean>(false)
//    override var isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()
    //

    private val pidList = listOf("010C", "010D", "0111", "0105", "0110")

    suspend fun readOBDData(
        input: InputStream,
        output: OutputStream,
        context: Context
    ): Map<String, String> {
        val data = mutableMapOf<String, String>()
        for (pid in pidList) {
//            if (!isBluetoothConnected.first()) break
            withContext(Dispatchers.IO) {
                try {
                    output.write("$pid\r".toByteArray())
                    output.flush()
                    val delayResp = delayResponse.first()
                    delay(delayResp) // beri waktu respons

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
//                    updateBluetoothConnection(false)
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

    // Fungsi ini sekarang mengembalikan true jika berhasil, false jika gagal.
    override suspend fun sendCommand(output: OutputStream, command: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                output.write((command + "\r").toByteArray())
                output.flush()
                true // Berhasil
            } catch (e: IOException) {
                Log.e("OBD_COMMAND", "Koneksi terputus saat mengirim perintah: ${e.message}")
                false // Gagal
            }
        }
    }

    override suspend fun updateServiceState(newState: ServiceState) {
        _serviceState.emit(newState)
    }

    override suspend fun updateOBDJobState(obdJobState: OBDJobState) {
        _obdJobState.emit(obdJobState)
    }

    override suspend fun updateMQTTJobState(mqttJobState: MQTTJobState) {
        _mqttJobState.emit(mqttJobState)
    }

    override suspend fun checkDataForMQTTConnection(): Boolean {
        return preferenceManager.checkDataForMQTTConnection()
    }

    override fun listenForResponses(input: InputStream): Flow<String> = callbackFlow {
        val readerJob = launch(Dispatchers.IO) {
            // Menggunakan StringBuilder lebih efisien untuk manipulasi string daripada ByteArrayOutputStream
            val responseBuffer = StringBuilder()
            val temp = ByteArray(1024)

            try {
                while (isActive) {
                    val len = input.read(temp)
                    if (len == -1) {
                        break // Akhir dari stream, koneksi terputus
                    }

                    // Tambahkan data yang baru dibaca ke StringBuilder
                    responseBuffer.append(String(temp, 0, len, Charsets.UTF_8))

                    // Terus proses selama masih ada prompt '>' di buffer
                    while (responseBuffer.contains('>')) {
                        // Ambil satu pesan penuh sampai prompt '>'
                        val promptIndex = responseBuffer.indexOf('>')
                        val singleResponse = responseBuffer.substring(0, promptIndex)

                        // Kirim pesan yang sudah dibersihkan ke flow
                        val cleanResponse = singleResponse.trim().replace("\r", "").replace("\n", "")
                        if (cleanResponse.isNotEmpty()) {
                            trySend(cleanResponse) // Mengirim data ke collector
                        }

                        // Hapus pesan yang sudah diproses dari buffer
                        responseBuffer.delete(0, promptIndex + 1)
                    }
                }
            } catch (e: IOException) {
                cancel("Connection lost", e)
            }
        }

        awaitClose { readerJob.cancel() }
    }

    override fun parseOBDResponse(response: String, context: Context): Map<String, String> {
        val data = mutableMapOf<String, String>()

        // Membersihkan karakter umum dan "SEARCHING..."
        var cleanResponse = response
            .replace("SEARCHING...", "", ignoreCase = true)
            .replace(" ", "") // Hapus semua spasi agar lebih mudah di-parse
            .trim()

        // Cari kemunculan terakhir dari "41" (ID respons untuk mode 01)
        // Ini membantu mengabaikan echo dan data sampah di awal string.
        val responseIndex = cleanResponse.lastIndexOf("41")
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
                    val rpm = hexValue.toInt(16) / 4
                    data["RPM"] = rpm.toString()
                }
            }
            cleanResponse.startsWith("410D") -> {
                val match = "410D([0-9A-Fa-f]{2})".toRegex().find(cleanResponse)
                if (match != null) {
                    val speed = match.groupValues[1].toInt(16)
                    data["Speed"] = speed.toString()
                }
            }
            cleanResponse.startsWith("4111") -> {
                val match = "4111([0-9A-Fa-f]{2})".toRegex().find(cleanResponse)
                if (match != null) {
                    val throttle = (match.groupValues[1].toInt(16) * 100) / 255
                    data["Throttle"] = throttle.toString()
                }
            }
            cleanResponse.startsWith("4105") -> {
                val match = "4105([0-9A-Fa-f]{2})".toRegex().find(cleanResponse)
                if (match != null) {
                    val temp = match.groupValues[1].toInt(16) - 40
                    data["Temperature"] = temp.toString()
                }
            }
            cleanResponse.startsWith("4110") -> {
                val match = "4110([0-9A-Fa-f]{4})".toRegex().find(cleanResponse)
                if (match != null) {
                    val maf = match.groupValues[1].toInt(16) / 100.0
                    data["MAF"] = maf.toInt().toString()
                }
            }
            else -> {
                if (cleanResponse.isNotBlank() && !cleanResponse.contains("NODATA", true)) {
                    Log.w("OBD", "No parser for PID: $cleanResponse")
                    // saveLogToFile(...)
                }
            }
        }

//        data["Fuel"] = (data.getValue("Speed").toInt() / (data.getValue("MAF").toInt() / 14.7 / 737 * 3600)).toString()

        onNewObdDataReceived(data)
        return data
    }

    override suspend fun updateData(data: Map<String, String>) {
        _obdData.emit(data)
    }

    private fun onNewObdDataReceived(newDataMap: Map<String, String>) {
        _obdItemsState.update { currentList ->

            // 1. DAPATKAN NILAI SPEED & MAF TERBARU
            // Ambil nilai baru dari newDataMap. Jika tidak ada, gunakan nilai lama dari currentList.
            val latestSpeedStr = newDataMap["Speed"] ?: currentList.find { it.label == "Speed" }?.value
            val latestMafStr = newDataMap["MAF"] ?: currentList.find { it.label == "MAF" }?.value

            var calculatedKml: String? = null

            // 2. LAKUKAN KALKULASI HANYA SATU KALI
            // Hanya lakukan kalkulasi jika kedua nilai yang dibutuhkan ada dan valid.
            if (latestSpeedStr != null && latestMafStr != null) {
                val speedInt = latestSpeedStr.toIntOrNull()
                val mafInt = latestMafStr.toIntOrNull()

                // Pastikan nilai valid dan MAF tidak nol untuk menghindari pembagian dengan nol
                if (speedInt != null && mafInt != null && mafInt > 0) {
                    try {
                        // Rumus konsumsi bahan bakar (contoh)
                        // MPG = (7.718 * Kecepatan (km/jam)) / MAF (gram/detik) -> konstanta ini untuk bensin
                        // kml = MPG * 0.425
                        // consume
                        val consume = mafInt / 14.7
                        // l/s
                        val consumePerSecond = consume / 737

                        val consumePerHour = consumePerSecond * 3600

                        val kml = speedInt / consumePerHour
//                        val mpg = (7.718 * speedInt) / mafInt
//                        val kml = mpg * 0.425

                        // Simpan hasil kalkulasi dalam format string dengan satu angka desimal
                        calculatedKml = kml.toInt().toString()

                    } catch (e: Exception) {
                        Log.e("FuelCalc", "Error calculating fuel consumption: ${e.message}")
                    }
                } else if (speedInt != null && mafInt != null && mafInt == 0){
                    calculatedKml = "0"
                }
            }

            // 3. BUAT LIST BARU DENGAN NILAI YANG SUDAH DIPERBARUI
            currentList.map { obdItem ->
                when (obdItem.label) {
                    // KASUS KHUSUS: Untuk item "Fuel"
                    "Fuel" -> {
                        // Gunakan hasil kalkulasi jika ada, jika tidak, pertahankan nilai lama.
//                        obdItem.copy(currValue = calculatedKml ?: obdItem.currValue)
                        obdItem.copy(value = calculatedKml ?: obdItem.value)
                    }
                    // KASUS UMUM: Untuk semua item lainnya (RPM, Speed, dll.)
                    else -> {
                        val newValue = newDataMap[obdItem.label]
                        if (newValue != null) {
                            // Jika ada nilai baru di newDataMap, update item ini
                            obdItem.copy(value = newValue)
                        } else {
                            // Jika tidak, pertahankan item seperti semula
                            obdItem
                        }
                    }
                }
            }
        }
    }

    override suspend fun updateBluetoothConnection(connect: Boolean) {
        _isBluetoothConnected.emit(connect)
    }
}