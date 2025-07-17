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
    // Fungsi pusat untuk menangani semua aksi dari UI
    fun handleAction(action: MqttAction, value: Any) {
        mqttRepository.handleAction(action, value)
    }
}