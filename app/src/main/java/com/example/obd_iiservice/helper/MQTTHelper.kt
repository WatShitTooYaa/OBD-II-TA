package com.example.obd_iiservice.helper

import android.util.Log
import com.example.obd_iiservice.setting.ui.mqtt.MQTTConfig
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.util.Locale
import java.util.UUID


class MqttHelper(
    private val config : MQTTConfig,
) {

    private val clientId = "android-client-" + UUID.randomUUID().toString()
    private val mqttHost = config.host
    private val mqttPort = config.port
    private val mqttTopic = config.topic
    private val mqttPortType = config.portType
    private val brokerUrl = "$mqttPortType://${mqttHost}:${mqttPort}"

    private val mqttClient: MqttAsyncClient = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())

    private val connectOptions: MqttConnectOptions = MqttConnectOptions().apply {
        isCleanSession = true
        connectionTimeout = 10
        keepAliveInterval = 20
//        if (config.username.isNullOrBlank() && config.password.isNullOrBlank()){
//            userName = config.username
//            password = config.password?.toCharArray()
//        }
        config.username?.takeIf { it.isNotBlank() }?.let { username ->
            config.password?.takeIf { it.isNotBlank() }?.let { password ->
                userName = username
                this.password = password.toCharArray()
            }
        }
//        isAutomaticReconnect = true
        Log.d("saat connect mqtt", "brokerUrl : $brokerUrl")
        // Gunakan TLS jika pakai ssl://
        if (mqttPortType == "ssl"){
            socketFactory = SSLSocketFactoryGenerator.createSocketFactory()
        }
    }

    fun connect(onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.w("MQTT", "Connection lost: ${cause?.message}")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d("MQTT", "Message received from $topic: ${message.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "Delivery complete")
            }
        })

        mqttClient.connect(connectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.i("MQTT", "Connected to broker")
                sendDiscoveryConfigs(mqttClient)
                onSuccess()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Failed to connect", exception)
                exception?.let { onFailure(it) }
            }
        })
    }

    fun sendDiscoveryConfigs(mqttClient: MqttAsyncClient) {
        val sensors = listOf(
            Triple("rpm", "OBD RPM", "rpm"),
            Triple("speed", "OBD Speed", "km/h"),
            Triple("throttle", "OBD Throttle", "%"),
            Triple("temp", "OBD Temperature", "°C"),
            Triple("maf", "OBD MAF", "g/s"),
        )

        for ((id, name, unit) in sensors) {
            val configTopic = "$mqttTopic/sensor/obd_$id/config"

            val configPayload = JSONObject().apply {
                put("name", name)
//                put("state_topic", "obd_fate")
                put("state_topic", mqttTopic)
                put("unit_of_measurement", unit)
                put("value_template", "{{ value_json.${id.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.ROOT
                    ) else it.toString()
                }} }}")
                put("unique_id", "obd_${id}_1")
                put("device", JSONObject().apply {
                    put("identifiers", listOf("obd_device"))
                    put("manufacturer", "MyOBD")
                    put("name", "OBD Device")
                })
            }

            mqttClient.publish(
                configTopic,
//                mqttTopic,
                configPayload.toString().toByteArray(),
                2,
                true // retain true agar HA tetap bisa deteksi meskipun restart
            )
        }
    }

    fun publish(topic: String, payload: String) {
        if (mqttClient.isConnected) {
            val message = MqttMessage(payload.toByteArray(Charsets.UTF_8))
            message.qos = 2
            message.isRetained = false
            mqttClient.publish(topic, message)
        } else {
            Log.e("MQTT", "Client not connected, can't publish")
        }
    }

    fun disconnect() {
        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

}
