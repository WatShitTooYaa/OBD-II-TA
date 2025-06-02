package com.example.obd_iiservice.setting

data class PartialMQTTConfig(
    val topic: String?,
    val host: String?,
    val port: Int?,
    val username: String?,
    val password: String?
)


data class MQTTConfig(
    val topic: String?,
    val host: String?,
    val port: Int?,
    val username: String?,
    val password: String?,
    val portType : String
)

