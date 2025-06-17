package com.example.obd_iiservice.setting.ui.mqtt

import androidx.lifecycle.viewModelScope
import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.helper.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


interface MqttRepository {
    val mqttConfig: Flow<List<MqttItem>>
    val isInitialized: StateFlow<Boolean>
    val mqttAutoReconnect: StateFlow<Boolean>

    fun handleAction(action: MqttAction, value: Any)
    fun saveMqttTopic(topic: String)
    fun saveMqttHost(host: String)
    fun saveMqttPort(port: Int)
    fun saveMqttUsername(username: String)
    fun saveMqttPassword(password: String)
    fun saveMqttPortType(type: String)
    fun saveMqttAutoRecon(isAuto: Boolean)
    fun saveDelayResponse(delay: Long)
    fun clearData()
}

class MqttRepositoryImpl @Inject constructor(
    private val preferenceManager: PreferenceManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : MqttRepository {
    // Helper function untuk mengurangi boilerplate
//    private fun <T> Flow<T?>.toState(initialValue: T?): StateFlow<T?> =
//        stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), initialValue)


    private fun <T> Flow<T>.toState(initialValue: T): StateFlow<T> =
        stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), initialValue)

    // Definisikan flow individual sebagai private, tidak perlu diekspos jika tidak dibutuhkan
    private val mqttTopic: StateFlow<String?> = preferenceManager.mqttTopic.toState(null)
    private val mqttHost: StateFlow<String?> = preferenceManager.mqttHost.toState(null)
    private val mqttPort: StateFlow<Int?> = preferenceManager.mqttPort.toState(null)
    private val mqttUsername: StateFlow<String?> = preferenceManager.mqttUsername.toState(null)
    private val mqttPassword: StateFlow<String?> = preferenceManager.mqttPassword.toState(null)
    private val mqttPortType: StateFlow<String> = preferenceManager.mqttPortType.toState("tcp")
    override val mqttAutoReconnect: StateFlow<Boolean> = preferenceManager.mqttAutoRecon.toState(false)
//
//    val mqttTopic: StateFlow<String?> = preferenceManager.mqttTopic
//        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)
//
//    val mqttHost: StateFlow<String?> = preferenceManager.mqttHost
//        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)
//
//    val mqttPort: StateFlow<Int?> = preferenceManager.mqttPort
//        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)
//
//    val mqttUsername: Flow<String?> = preferenceManager.mqttUsername
//        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)
//
//    val mqttPassword: StateFlow<String?> = preferenceManager.mqttPassword
//        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)
//
//    val mqttAutoRecon: StateFlow<Boolean> = preferenceManager.mqttAutoRecon
//        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), false)
//
//    val mqttPortType: StateFlow<String> = preferenceManager.mqttPortType
//        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), "tcp")

    val delayResponse: StateFlow<Long> = preferenceManager.delayResponse
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), 0)
//    val mqttConfig: Flow<MQTTConfig> = combine(


    val partialMQTTConfig = combine(
        mqttTopic, mqttHost, mqttPort, mqttUsername, mqttPassword
    ) { topic, host, port, username, password ->
        PartialMQTTConfig(topic, host, port, username, password)
    }

    override val mqttConfig = partialMQTTConfig.combine(mqttPortType) { partial, portType ->
        buildSettingsList(
            host = partial.host,
            port = partial.port,
            username = partial.username,
            pass = partial.password,
            topic = partial.topic,
            type = portType
        )
    }

    private fun buildSettingsList(host: String?, port: Int?, topic: String?, username: String?, pass: String?, type: String): List<MqttItem> {
        return listOf(
            MqttItem("Host", "Alamat server MQTT", MqttAction.EDIT_HOST, host),
            MqttItem("Port", "Port server MQTT", MqttAction.EDIT_PORT, port?.toString() ?: "0"),
            MqttItem("Topic", "Topic untuk subscribe/publish", MqttAction.EDIT_TOPIC, topic),
            MqttItem("Username", "Username untuk otentikasi", MqttAction.EDIT_USERNAME, username?.ifEmpty { "Tidak diatur" }),
            MqttItem("Password", "Password untuk otentikasi", MqttAction.EDIT_PASSWORD, if (pass?.isNotEmpty() == true) "********" else "Tidak diatur"),
            MqttItem("Tipe Koneksi", "Protokol koneksi", MqttAction.EDIT_PORT_TYPE, type)
        )
    }

    override val isInitialized = combine(
        mqttTopic,
        mqttHost,
        mqttPort
    ) { topic, host, port ->
        topic != null && host != null && port != null
    }.stateIn(applicationScope, SharingStarted.Eagerly, false)


    // Fungsi pusat untuk menangani semua aksi dari UI
    override fun handleAction(action: MqttAction, value: Any) {
        applicationScope.launch {
            when (action) {
                MqttAction.EDIT_HOST -> preferenceManager.saveMqttHost(value as String)
                MqttAction.EDIT_PORT -> preferenceManager.saveMqttPort(value as Int)
                MqttAction.EDIT_TOPIC -> preferenceManager.saveMqttTopic(value as String)
                MqttAction.EDIT_USERNAME -> preferenceManager.saveMqttUsername(value as String)
                MqttAction.EDIT_PASSWORD -> preferenceManager.saveMqttPassword(value as String)
                MqttAction.EDIT_PORT_TYPE -> preferenceManager.saveMqttPortType(value as String)
            }
        }
    }

    override fun saveMqttTopic(topic: String) {
        applicationScope.launch { preferenceManager.saveMqttTopic(topic) }
    }

    override fun saveMqttHost(host: String) {
        applicationScope.launch { preferenceManager.saveMqttHost(host) }
    }

    override fun saveMqttPort(port: Int) {
        applicationScope.launch { preferenceManager.saveMqttPort(port) }
    }

    override fun saveMqttUsername(username: String) {
        applicationScope.launch { preferenceManager.saveMqttUsername(username) }
    }

    override fun saveMqttPassword(password: String) {
        applicationScope.launch { preferenceManager.saveMqttPassword(password) }
    }

    override fun saveMqttPortType(type: String) {
        applicationScope.launch { preferenceManager.saveMqttPortType(type) }
    }

    override fun saveMqttAutoRecon(isAuto: Boolean) {
        applicationScope.launch { preferenceManager.saveMqttAuto(isAuto) }
    }

    override fun saveDelayResponse(delay: Long) {
        applicationScope.launch { preferenceManager.saveDelayResponse(delay) }
    }

    suspend fun checkDataForConnecting() : Boolean {
        return preferenceManager.checkDataForMQTTConnection()
    }

    override fun clearData() {
        applicationScope.launch { preferenceManager.clearMQTT() }
    }
}