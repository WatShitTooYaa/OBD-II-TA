package com.example.obd_iiservice.setting

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.ActivitySettingBinding
import com.example.obd_iiservice.helper.saveLogToFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private val settingViewModel: SettingViewModel by viewModels()
    var isProgrammaticChange = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                setUI()
                collectUI()
            }
        }

//        setUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_setting)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private suspend fun setUI(){

        binding.apply {
//            this.etMqttTopic.setText(settingViewModel.mqttTopic.first())
//            this.etMqttHost.setText(settingViewModel.mqttHost.first())
//            settingViewModel.mqttPort.first()?.toInt()?.let { this.etMqttPort.setText(it) } ?: 0
//            this.etMqttUsername.setText(settingViewModel.mqttUsername.first())
//            this.etMqttPassword.setText(settingViewModel.mqttPassword.first())

            lifecycleScope.launch {

                launch {
                    settingViewModel.mqttAutoRecon.collect { isActive ->
                        isProgrammaticChange = true
                        if (isActive){
                            binding.rgAutoRecon.check(binding.radioYes.id)
                        } else {
                            binding.rgAutoRecon.check(binding.radioNo.id)
                        }
                        isProgrammaticChange = false
                    }
                }

                launch {
                    settingViewModel.mqttPortType.collect { portType ->
                        isProgrammaticChange = true
                        if (portType == "tcp"){
                            binding.rgPort.check(binding.radioTcp.id)
                        } else {
                            binding.rgPort.check(binding.radioTls.id)
                        }
                        isProgrammaticChange = false
                    }
                }

                launch {
//                    combine(
//                        settingViewModel.mqttTopic,
//                        settingViewModel.mqttHost,
//                        settingViewModel.mqttPort,
//                        settingViewModel.mqttUsername,
//                        settingViewModel.mqttPassword,
//                        settingViewModel.mqttPortType
//                    ) { topic, host, port, username, password, portType ->
//                        MQTTConfig(topic, host, port, username, password, portType)
//                    }.collectLatest { config ->
//                        binding.etMqttTopic.setText(config.topic)
//                        binding.etMqttHost.setText(config.host)
//    //                    config.port?.let { binding.etMqttPort.setText(it.toString()) } ?: 0
//                        binding.etMqttPort.setText(config.port?.toString() ?: "0")
//                        binding.etMqttUsername.setText(config.username)
//                        binding.etMqttPassword.setText(config.password)
//
//                        Log.d("MQTT_PREFS", "Semua data diisi ke UI")
//                    }
                    settingViewModel.mqttConfig.collectLatest { config ->
                        binding.etMqttTopic.setText(config.topic)
                        binding.etMqttHost.setText(config.host)
    //                    config.port?.let { binding.etMqttPort.setText(it.toString()) } ?: 0
                        binding.etMqttPort.setText(config.port?.toString() ?: "0")
                        binding.etMqttUsername.setText(config.username)
                        binding.etMqttPassword.setText(config.password)
                    }
                }
            }


            this.rgAutoRecon.setOnCheckedChangeListener { group, checkedId ->
                if (isProgrammaticChange) return@setOnCheckedChangeListener
                when(checkedId){
                    R.id.radio_yes -> {
                        lifecycleScope.launch {
                            settingViewModel.saveMqttAutoRecon(true)
                        }
                    }
                    R.id.radio_no -> {
                        lifecycleScope.launch {
                            settingViewModel.saveMqttAutoRecon(false)
                        }
                    }
                    else -> {}
                }
            }

            this.rgPort.setOnCheckedChangeListener { group, checkedId ->
                if (isProgrammaticChange) return@setOnCheckedChangeListener
                when(checkedId){
                    R.id.radio_tcp -> {
                        lifecycleScope.launch {
                            settingViewModel.saveMqttPortType("tcp")
                        }
                    }
                    R.id.radio_tls -> {
                        lifecycleScope.launch {
                            settingViewModel.saveMqttPortType("ssl")
                        }
                    }
                    else -> {}
                }
            }


            this.btnMqttSave.setOnClickListener {
                val topic = binding.etMqttTopic.text.toString()
                val host = binding.etMqttHost.text.toString()
                val port = binding.etMqttPort.text.toString()
                val allData = topic.isNotBlank() && host.isNotBlank() && port.isNotBlank()
                if (allData){
                    settingViewModel.saveMqttTopic(this.etMqttTopic.text.toString())
                    settingViewModel.saveMqttHost(this.etMqttHost.text.toString())
                    settingViewModel.saveMqttPort(this.etMqttPort.text.toString().toInt())
                    settingViewModel.saveMqttUsername(this.etMqttUsername.text.toString())
                    settingViewModel.saveMqttPassword(this.etMqttPassword.text.toString())
                } else {
                    makeToast("Cannot null")
                }
            }

            this.btnMqttClear.setOnClickListener {
                AlertDialog.Builder(this@SettingActivity)
                    .setTitle("Hapus data")
                    .setMessage("Apakah anda ingin menghapus data")
                    .setPositiveButton("Ya") { _,_ ->
                        settingViewModel.clearData()
                    }
                    .setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
//                settingViewModel.clearData()
            }
        }
//        binding.apply {
//
//        }
    }

    private suspend fun collectUI() {
        settingViewModel.mqttConfig.collect { config ->
            Log.d("collectt", "topic : ${config.topic}")
            Log.d("collecth", "host : ${config.host}")
            Log.d("collectp", "port : ${config.port}")
            Log.d("collectu", "username : ${config.username}")
            Log.d("collectpass", "password : ${config.password}")
            Log.d("collecttype", "type : ${config.portType}")

            saveLogToFile(this, "Mqttconfig", "OK", config.toString())
        }
    }

    fun makeToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}