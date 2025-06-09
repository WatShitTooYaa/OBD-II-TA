package com.example.obd_iiservice.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obd_iiservice.helper.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    val bluetoothAddress: StateFlow<String?> = preferenceManager.bluetoothAddress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttTopic: StateFlow<String?> = preferenceManager.mqttTopic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttHost: StateFlow<String?> = preferenceManager.mqttHost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttPort: StateFlow<Int?> = preferenceManager.mqttPort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttUsername: Flow<String?> = preferenceManager.mqttUsername
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttPassword: StateFlow<String?> = preferenceManager.mqttPassword
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mqttAutoRecon: StateFlow<Boolean> = preferenceManager.mqttAutoRecon
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val mqttPortType: StateFlow<String> = preferenceManager.mqttPortType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "tcp")

    val delayResponse: StateFlow<Long> = preferenceManager.delayResponse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun saveBluetoothAddress(address: String) {
        viewModelScope.launch { preferenceManager.saveBluetoothAddress(address) }
    }

    fun saveMqttTopic(topic: String) {
        viewModelScope.launch { preferenceManager.saveMqttTopic(topic) }
    }

    fun saveMqttHost(host: String) {
        viewModelScope.launch { preferenceManager.saveMqttHost(host) }
    }

    fun saveMqttPort(port: Int) {
        viewModelScope.launch { preferenceManager.saveMqttPort(port) }
    }

    fun saveMqttUsername(username: String) {
        viewModelScope.launch { preferenceManager.saveMqttUsername(username) }
    }

    fun saveMqttPassword(password: String) {
        viewModelScope.launch { preferenceManager.saveMqttPassword(password) }
    }

    fun saveMqttAutoRecon(isAuto: Boolean) {
        viewModelScope.launch { preferenceManager.saveMqttAuto(isAuto) }
    }

    fun saveMqttPortType(type: String) {
        viewModelScope.launch { preferenceManager.saveMqttPortType(type) }
    }

    fun saveDelayResponse(delay: Long) {
        viewModelScope.launch { preferenceManager.saveDelayResponse(delay) }
    }

    suspend fun checkDataForConnecting() : Boolean {
        return preferenceManager.checkDataForConnecting()
    }

    fun clearData() {
        viewModelScope.launch { preferenceManager.clearMQTT() }
    }

}
