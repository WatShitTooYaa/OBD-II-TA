package com.example.obd_iiservice.helper

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.obd_iiservice.setting.ui.mqtt.MQTTConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


private val Context.dataStore by preferencesDataStore(name = "mqtt_preferences")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val BLUETOOTH_ADDRESS = stringPreferencesKey("bluetooth_address")
        val MQTT_HOST = stringPreferencesKey("mqtt_host")
        val MQTT_PORT = intPreferencesKey("mqtt_port")
        val MQTT_USERNAME = stringPreferencesKey("mqtt_username")
        val MQTT_PASSWORD = stringPreferencesKey("mqtt_password")
        val MQTT_TOPIC = stringPreferencesKey("mqtt_topic")
        val MQTT_AUTO_RECONNECT = booleanPreferencesKey("mqtt_auto_reconnect")
        val MQTT_PORT_TYPE = stringPreferencesKey("mqtt_port_type")

        val DELAY_RESPONSE = longPreferencesKey("delay_response")

        //untuk meyimpan threshold
        val RPM_THRESHOLD = intPreferencesKey("rpm_threshold")
        val SPEED_THRESHOLD = intPreferencesKey("speed_threshold")
        val THROTTLE_THRESHOLD = intPreferencesKey("throttle_threshold")
        val TEMP_THRESHOLD = intPreferencesKey("temp_threshold")
        val MAF_THRESHOLD = doublePreferencesKey("maf_threshold")
        val FUEL_THRESHOLD = intPreferencesKey("fuel_threshold")
    }


    //Getter
    //bluetooth & MQTT
    val bluetoothAddress : Flow<String?> = context.dataStore.data.map { it[BLUETOOTH_ADDRESS] }
    val mqttHost: Flow<String?> = context.dataStore.data.map { it[MQTT_HOST] }
    val mqttPort: Flow<Int?> = context.dataStore.data.map { it[MQTT_PORT] }
    val mqttUsername: Flow<String?> = context.dataStore.data.map { it[MQTT_USERNAME] }
    val mqttPassword: Flow<String?> = context.dataStore.data.map { it[MQTT_PASSWORD] }
    val mqttTopic: Flow<String?> = context.dataStore.data.map { it[MQTT_TOPIC] }
    val mqttAutoRecon: Flow<Boolean> = context.dataStore.data.map { it[MQTT_AUTO_RECONNECT] == true }
    val mqttPortType: Flow<String> = context.dataStore.data.map { it[MQTT_PORT_TYPE] ?: "tcp" }

    //delay get data
    val delayResponse: Flow<Long> = context.dataStore.data.map { it[DELAY_RESPONSE] ?: 100 }

    //threshold
    val rpmThreshold : Flow<Int> = context.dataStore.data.map { it[RPM_THRESHOLD] ?: 2500 }
    val speedThreshold : Flow<Int> = context.dataStore.data.map { it[SPEED_THRESHOLD] ?: 100 }
    val throttleThreshold : Flow<Int> = context.dataStore.data.map { it[THROTTLE_THRESHOLD] ?: 70 }
    val tempThreshold : Flow<Int> = context.dataStore.data.map { it[TEMP_THRESHOLD] ?: 110 }
    val mafThreshold : Flow<Double> = context.dataStore.data.map { it[MAF_THRESHOLD] ?: 20.0}
    val fuelThreshold : Flow<Int> = context.dataStore.data.map { it[FUEL_THRESHOLD] ?: 20 }


    private suspend fun <T> saveOrRemove(key: Preferences.Key<T>, value: T?) {
        context.dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }

    // 2. FUNGSI PUBLIK YANG JAUH LEBIH RINGKAS

    // --- FUNGSI DATA BLUETOOTH DAN MQTT ---
    suspend fun saveBluetoothAddress(address: String?) = saveOrRemove(BLUETOOTH_ADDRESS, address)
    suspend fun saveMqttHost(host: String?) = saveOrRemove(MQTT_HOST, host)
    suspend fun saveMqttUsername(username: String?) = saveOrRemove(MQTT_USERNAME, username)
    suspend fun saveMqttPassword(password: String?) = saveOrRemove(MQTT_PASSWORD, password)
    suspend fun saveMqttTopic(topic: String?) = saveOrRemove(MQTT_TOPIC, topic)

    // Untuk tipe data yang tidak nullable, gunakan fungsi ekspresi tunggal
    suspend fun saveMqttPort(port: Int) = context.dataStore.edit { it[MQTT_PORT] = port }
    suspend fun saveMqttAuto(isAuto: Boolean) = context.dataStore.edit { it[MQTT_AUTO_RECONNECT] = isAuto }
    suspend fun saveMqttPortType(type: String) = context.dataStore.edit { it[MQTT_PORT_TYPE] = type }

    // --- FUNGSI DATA DELAY ---
    suspend fun saveDelayResponse(delay: Long) = context.dataStore.edit { it[DELAY_RESPONSE] = delay }

    // --- FUNGSI DATA THRESHOLD ---
    suspend fun saveRpmThreshold(rpm: Int) = context.dataStore.edit { it[RPM_THRESHOLD] = rpm }
    suspend fun saveSpeedThreshold(speed: Int) = context.dataStore.edit { it[SPEED_THRESHOLD] = speed }
    suspend fun saveThrottleThreshold(throttle: Int) = context.dataStore.edit { it[THROTTLE_THRESHOLD] = throttle }
    suspend fun saveTempThreshold(temp: Int) = context.dataStore.edit { it[TEMP_THRESHOLD] = temp }
    suspend fun saveMafThreshold(maf: Double) = context.dataStore.edit { it[MAF_THRESHOLD] = maf }
    suspend fun saveFuelThreshold(fuel: Int) = context.dataStore.edit { it[FUEL_THRESHOLD] = fuel }

    //fungsi menyimpan data bluetooth dan mqtt
//    suspend fun saveBluetoothAddress (address: String?) {
//        context.dataStore.edit { preferences ->
//            if (address == null) {
//                preferences.remove(BLUETOOTH_ADDRESS)
//            } else {
//                preferences[BLUETOOTH_ADDRESS] = address
//            }
//        }
//    }
//
//    suspend fun saveMqttHost(host: String?) {
//        context.dataStore.edit { preferences ->
//            if (host == null) {
//                preferences.remove(MQTT_HOST)
//            } else {
//                preferences[MQTT_HOST] = host
//            }
//        }
////        context.dataStore.edit { it[MQTT_HOST] = host }
//    }
//
//    suspend fun saveMqttPort(port: Int) {
//        context.dataStore.edit { it[MQTT_PORT] = port }
//    }
//
//    suspend fun saveMqttUsername(username: String?) {
//        context.dataStore.edit { preferences ->
//            if (username == null) {
//                preferences.remove(MQTT_USERNAME)
//            } else {
//                preferences[MQTT_USERNAME] = username
//            }
//        }
//    }
//
//    suspend fun saveMqttPassword(password: String?) {
//        context.dataStore.edit { preferences ->
//            if (password == null) {
//                preferences.remove(MQTT_PASSWORD)
//            } else {
//                preferences[MQTT_PASSWORD] = password
//            }
//        }
//    }
//
//    suspend fun saveMqttTopic(topic: String?) {
//        context.dataStore.edit { preferences ->
//            if (topic == null) {
//                preferences.remove(MQTT_TOPIC)
//            } else {
//                preferences[MQTT_TOPIC] = topic
//            }
//        }
////        context.dataStore.edit { it[MQTT_TOPIC] = topic }
//    }
//
//    suspend fun saveMqttAuto(isAuto: Boolean) {
//        context.dataStore.edit { it[MQTT_AUTO_RECONNECT] = isAuto }
//    }
//
//    suspend fun saveMqttPortType(type: String) {
//        context.dataStore.edit { it[MQTT_PORT_TYPE] = type }
//    }
//
//    //fungsi menyimpan data delay
//    suspend fun saveDelayResponse(delay : Long) {
//        context.dataStore.edit { it[DELAY_RESPONSE] = delay }
//    }
//
//    //fungsi menyimpan data threshold
//    suspend fun saveRpmThreshold(rpm : Int) {
//        context.dataStore.edit { it[RPM_THRESHOLD] = rpm }
//    }
//
//    suspend fun saveSpeedThreshold(speed : Int) {
//        context.dataStore.edit { it[SPEED_THRESHOLD] = speed }
//    }
//
//    suspend fun saveThrottleThreshold(throttle : Int) {
//        context.dataStore.edit { it[THROTTLE_THRESHOLD] = throttle }
//    }
//
//    suspend fun saveTempThreshold(temp : Int) {
//        context.dataStore.edit { it[TEMP_THRESHOLD] = temp }
//    }
//
//    suspend fun saveMafThreshold(maf : Double) {
//        context.dataStore.edit { it[MAF_THRESHOLD] = maf }
//    }
//
//    suspend fun saveFuelThreshold(fuel : Int) {
//        context.dataStore.edit { it[FUEL_THRESHOLD] = fuel }
//    }


    //clear data MQTT
    suspend fun clearMQTT() {
        context.dataStore.edit { preferences ->
            preferences.remove(BLUETOOTH_ADDRESS)
            preferences.remove(MQTT_HOST)
            preferences.remove(MQTT_PORT)
            preferences.remove(MQTT_USERNAME)
            preferences.remove(MQTT_PASSWORD)
            preferences.remove(MQTT_TOPIC)
            preferences.remove(MQTT_AUTO_RECONNECT)
            preferences.remove(MQTT_PORT_TYPE)
        }
    }

    //clear data Threshold
    suspend fun clearThresholds() {
        context.dataStore.edit { preferences ->
            preferences.remove(RPM_THRESHOLD)
            preferences.remove(SPEED_THRESHOLD)
            preferences.remove(THROTTLE_THRESHOLD)
            preferences.remove(TEMP_THRESHOLD)
            preferences.remove(MAF_THRESHOLD)
        }
    }


    //Delete alllll
    suspend fun clear(){
        context.dataStore.edit { it.clear() }
    }

    suspend fun checkDataForMQTTConnection() : Boolean {
        val topic = mqttTopic.first()
        val host = mqttHost.first()
        val port = mqttPort.first()
        return topic != null && host != null && port != null
    }

    suspend fun getMQTTConfig() : MQTTConfig {
        val topic = mqttTopic.first()
        val host = mqttHost.first()
        val port = mqttPort.first()
        val username = mqttUsername.first()
        val password = mqttPassword.first()
        val portType = mqttPortType.first()
        return MQTTConfig(topic, host, port, username, password, portType)
    }
}