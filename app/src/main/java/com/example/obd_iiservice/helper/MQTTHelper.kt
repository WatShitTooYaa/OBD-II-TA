package com.example.obd_iiservice.helper

import android.util.Log
import com.example.obd_iiservice.app.ApplicationScope
import com.example.obd_iiservice.setting.ui.mqtt.MQTTConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private val scope: CoroutineScope
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
//        Log.d("saat connect mqtt", "brokerUrl : $brokerUrl")
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
                scope.launch {
                    sendDiscoveryConfigs(mqttClient)
                }
                onSuccess()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Failed to connect", exception)
                exception?.let { onFailure(it) }
            }
        })
    }

    suspend fun sendDiscoveryConfigs(mqttClient: MqttAsyncClient) {
        val sensors = listOf(
            Triple("MAF", "OBD MAF", "g/s"),
            Triple("Fuel", "OBD Fuel", "km/l"),
            Triple("Throttle", "OBD Throttle", "%"),
            Triple("Temperature", "OBD Temperature", "°C"),
            Triple("RPM", "OBD RPM", "rpm"),
            Triple("Speed", "OBD Speed", "km/h"),
        )

        Log.d("MQTT_CONFIG_SENDER", "Memulai pengiriman ${sensors.size} konfigurasi dengan delay...")

        for ((index, sensor) in sensors.withIndex()) {
            val (id, name, unit) = sensor
            val configTopic = "$mqttTopic/sensor/obd_$id/config"

            val configPayload = JSONObject().apply {
                put("name", name)
                put("state_topic", mqttTopic)
                put("unit_of_measurement", unit)
                put("value_template", "{{ value_json.$id }}")
                put("unique_id", "obd_${id}_1")
                put("device", JSONObject().apply {
                    put("identifiers", listOf("obd_device"))
                    put("manufacturer", "MyOBD")
                    put("name", "OBD Device")
                })
            }

            val payloadString = configPayload.toString()
            Log.d("MQTT_CONFIG_SENDER", "Payload String #${index + 1} untuk ID '$id': $payloadString")

            try {
                mqttClient.publish(
                    configTopic,
                    payloadString.toByteArray(Charsets.UTF_8),
                    2,
                    true
                )
                Log.d("MQTT_CONFIG_SENDER", "-> Berhasil memanggil publish untuk ID: '$id'")
            } catch (e: Exception) {
                Log.e("MQTT_CONFIG_SENDER", "-> GAGAL memanggil publish untuk ID: '$id'.", e)
            }

            // PERUBAHAN 4: Tambahkan delay 100 milidetik setelah setiap pengiriman
            delay(100)
        }
        Log.d("MQTT_CONFIG_SENDER", "Proses pengiriman konfigurasi selesai.")
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
