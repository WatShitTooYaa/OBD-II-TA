package com.example.obd_iiservice.setting.ui.mqtt

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
    val mqttConfig: Flow<MQTTConfig>
    val isInitialized: StateFlow<Boolean>
    val mqttAutoReconnect: StateFlow<Boolean>

    fun saveMqttTopic(topic: String)
    fun saveMqttHost(host: String)
    fun saveMqttPort(port: Int)
    fun saveMqttUsername(username: String)
    fun saveMqttPassword(password: String)
    fun saveMqttAutoRecon(isAuto: Boolean)
    fun saveMqttPortType(type: String)
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
        MQTTConfig(
            topic = partial.topic,
            host = partial.host,
            port = partial.port,
            username = partial.username,
            password = partial.password,
            portType = portType
        )
    }

    override val isInitialized = combine(
        mqttTopic,
        mqttHost,
        mqttPort
    ) { topic, host, port ->
        topic != null && host != null && port != null
    }.stateIn(applicationScope, SharingStarted.Eagerly, false)

    fun saveBluetoothAddress(address: String) {
        applicationScope.launch { preferenceManager.saveBluetoothAddress(address) }
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

    override fun saveMqttAutoRecon(isAuto: Boolean) {
        applicationScope.launch { preferenceManager.saveMqttAuto(isAuto) }
    }

    override fun saveMqttPortType(type: String) {
        applicationScope.launch { preferenceManager.saveMqttPortType(type) }
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