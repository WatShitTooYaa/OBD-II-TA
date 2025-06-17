package com.example.obd_iiservice.setting.ui.mqtt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MqttViewModel @Inject constructor(
    private val mqttRepository: MqttRepository
) : ViewModel() {

    val uiState = mqttRepository.mqttConfig

//    private fun buildSettingsList(host: String, port: Int, topic: String, username: String, pass: String, type: String): List<MqttItem> {
//        return listOf(
//            MqttItem("Host", "Alamat server MQTT", MqttAction.EDIT_HOST, host),
//            MqttItem("Port", "Port server MQTT", MqttAction.EDIT_PORT, port.toString()),
//            MqttItem("Topic", "Topic untuk subscribe/publish", MqttAction.EDIT_TOPIC, topic),
//            MqttItem("Username", "Username untuk otentikasi", MqttAction.EDIT_USERNAME, username.ifEmpty { "Tidak diatur" }),
//            MqttItem("Password", "Password untuk otentikasi", MqttAction.EDIT_PASSWORD, if (pass.isNotEmpty()) "********" else "Tidak diatur"),
//            MqttItem("Tipe Koneksi", "Protokol koneksi", MqttAction.EDIT_PORT_TYPE, type)
//        )
//    }

    // Fungsi pusat untuk menangani semua aksi dari UI
    fun handleAction(action: MqttAction, value: Any) {
        mqttRepository.handleAction(action, value)
    }
}