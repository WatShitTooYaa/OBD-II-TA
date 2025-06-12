package com.example.obd_iiservice.setting

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

interface SettingRepository {

}

class SettingRepositoryImpl @Inject constructor(
    private val preferenceManager: PreferenceManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : SettingRepository {
    val bluetoothAddress: StateFlow<String?> = preferenceManager.bluetoothAddress
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttTopic: StateFlow<String?> = preferenceManager.mqttTopic
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttHost: StateFlow<String?> = preferenceManager.mqttHost
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttPort: StateFlow<Int?> = preferenceManager.mqttPort
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttUsername: Flow<String?> = preferenceManager.mqttUsername
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttPassword: StateFlow<String?> = preferenceManager.mqttPassword
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttAutoRecon: StateFlow<Boolean> = preferenceManager.mqttAutoRecon
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), false)

    val mqttPortType: StateFlow<String> = preferenceManager.mqttPortType
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), "tcp")

    val delayResponse: StateFlow<Long> = preferenceManager.delayResponse
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), 0)
//    val mqttConfig: Flow<MQTTConfig> = combine(
//        mqttTopic, mqttHost, mqttPort, mqttUsername, mqttPassword
//    ) { topic, host, port, username, password ->
//        MQTTConfig(topic, host, port, username, password)
//    }

//    val mqttConfig: Flow<MQTTConfig> = combine(
//        mqttTopic, mqttHost, mqttPort, mqttUsername, mqttPassword, mqttPort
//    ) { values ->
//        MQTTConfig(
//            topic = values[0] as String?,
//            host = values[1] as String?,
//            port = values[2] as Int?,
//            username = values[3] as String?,
//            password = values[4] as String?,
//            portType = values[5] as String
//        )
//    }

    val partialMQTTConfig = combine(
        mqttTopic, mqttHost, mqttPort, mqttUsername, mqttPassword
    ) { topic, host, port, username, password ->
        PartialMQTTConfig(topic, host, port, username, password)
    }

    val mqttConfig = partialMQTTConfig.combine(mqttPortType) { partial, portType ->
        MQTTConfig(
            topic = partial.topic,
            host = partial.host,
            port = partial.port,
            username = partial.username,
            password = partial.password,
            portType = portType
        )
    }

    val isInitialized = combine(
        bluetoothAddress,
        mqttTopic,
        mqttHost,
        mqttPort
    ) { addr, topic, host, port ->
        addr != null && topic != null && host != null && port != null
    }.stateIn(applicationScope, SharingStarted.Eagerly, false)

    fun saveBluetoothAddress(address: String) {
        applicationScope.launch { preferenceManager.saveBluetoothAddress(address) }
    }

    fun saveMqttTopic(topic: String) {
        applicationScope.launch { preferenceManager.saveMqttTopic(topic) }
    }

    fun saveMqttHost(host: String) {
        applicationScope.launch { preferenceManager.saveMqttHost(host) }
    }

    fun saveMqttPort(port: Int) {
        applicationScope.launch { preferenceManager.saveMqttPort(port) }
    }

    fun saveMqttUsername(username: String) {
        applicationScope.launch { preferenceManager.saveMqttUsername(username) }
    }

    fun saveMqttPassword(password: String) {
        applicationScope.launch { preferenceManager.saveMqttPassword(password) }
    }

    fun saveMqttAutoRecon(isAuto: Boolean) {
        applicationScope.launch { preferenceManager.saveMqttAuto(isAuto) }
    }

    fun saveMqttPortType(type: String) {
        applicationScope.launch { preferenceManager.saveMqttPortType(type) }
    }

    fun saveDelayResponse(delay: Long) {
        applicationScope.launch { preferenceManager.saveDelayResponse(delay) }
    }

    suspend fun checkDataForConnecting() : Boolean {
        return preferenceManager.checkDataForConnecting()
    }

    fun clearData() {
        applicationScope.launch { preferenceManager.clearMQTT() }
    }

}