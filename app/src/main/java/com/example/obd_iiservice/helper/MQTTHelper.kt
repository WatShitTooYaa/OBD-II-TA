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
import org.eclipse.paho.client.mqttv3.MqttException
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
        config.username?.takeIf { it.isNotBlank() }?.let { username ->
            config.password?.takeIf { it.isNotBlank() }?.let { password ->
                userName = username
                this.password = password.toCharArray()
            }
        }
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

            try {
                mqttClient.publish(
                    configTopic,
                    payloadString.toByteArray(Charsets.UTF_8),
                    2,
                    true
                )
            } catch (e: Exception) {
                Log.e("MQTT_CONFIG_SENDER", "-> GAGAL memanggil publish untuk ID: '$id'.", e)
            }

            delay(100)
        }
        Log.d("MQTT_CONFIG_SENDER", "Proses pengiriman konfigurasi selesai.")
    }

    fun publish(topic: String, payload: String) {
        // 1. Pengecekan Proaktif: Cek kondisi umum terlebih dahulu.
        // Jika tidak terhubung, langsung keluar agar tidak membuang sumber daya.
        if (!mqttClient.isConnected) {
            Log.w("MQTT_Publish", "Client not connected. Skipping publish to topic: $topic")
            return
        }

        // Jika sampai di sini, berarti klien kemungkinan besar terhubung.
        try {
            // 2. Aksi Utama dengan Penanganan Error Reaktif
            val message = MqttMessage(payload.toByteArray(Charsets.UTF_8))
            message.qos = 1 // QoS 1 atau 0 lebih disarankan untuk data telemetri yang sering untuk mengurangi overhead
            message.isRetained = false

            // Menggunakan publish dengan listener untuk menangani hasil secara asinkron
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT_Publish", "Publish successful to topic: $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    // Ini menangani kegagalan pada level jaringan atau broker
                    Log.e("MQTT_Publish", "Publish failed to topic: $topic", exception)
                }
            })

        } catch (e: MqttException) {
            // Ini menangani error yang mungkin terjadi SAAT memanggil publish(),
            // misalnya jika antrian internal penuh (Too many publishes in progress).
            Log.e("MQTT_Publish", "MqttException on calling publish: ${e.message}")
        } catch (e: Exception) {
            // Jaring pengaman untuk error tak terduga lainnya.
            Log.e("MQTT_Publish", "An unexpected error occurred during publish call: ${e.message}")
        }
    }

    fun disconnect() {
        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }

}
