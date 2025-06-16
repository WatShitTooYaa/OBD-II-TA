package com.example.obd_iiservice.setting.ui.mqtt

import android.app.Activity

data class MqttItem(
    val title: String,
    val description: String,
    val action: MqttAction,
    val displayValue: String // Nilai yang ditampilkan di UI
)

// Enum yang mewakili setiap fungsi spesifik Anda
enum class MqttAction {
    EDIT_HOST,
    EDIT_PORT,
    EDIT_TOPIC,
    EDIT_USERNAME,
    EDIT_PASSWORD,
    EDIT_PORT_TYPE
}

