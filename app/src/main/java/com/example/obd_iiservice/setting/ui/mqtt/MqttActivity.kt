package com.example.obd_iiservice.setting.ui.mqtt

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.ActivityMqttBinding
import com.example.obd_iiservice.setting.ui.bluetooth.BluetoothActivity
import com.example.obd_iiservice.setting.ui.threshold.ThresholdActivity
import com.example.obd_iiservice.ui.decoration.RecyclerViewItemDecoration
import com.example.obd_iiservice.ui.setting.SettingAdapter
//import com.example.obd_iiservice.ui.setting.MqttItem

class MqttActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMqttBinding
    private lateinit var rvMqtt : RecyclerView
    private lateinit var mqttAdapter: MqttAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMqttBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView(){
        rvMqtt = binding.rvMqtt
        rvMqtt.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvMqtt.layoutManager = layoutManager
//        val listMqttItem = listOf(
//            MqttItem(
//                "Bluetooth",
//                "Connection",
//                BluetoothActivity::class.java
//            ),
//            MqttItem(
//                "Threshold",
//                "Limit data",
//                ThresholdActivity::class.java
//            ),
//            MqttItem(
//                "MQTT",
//                "Setting connection MQTT",
//                MqttActivity::class.java
//            ),
//        )
//        mqttAdapter = MqttAdapter(listMqttItem) { clickedItem ->
//            clickedItem.targetActivity.let { activityClass ->
//                val intent = Intent(this, activityClass)
//                startActivity(intent)
//            }
//        }

        rvMqtt.adapter = mqttAdapter


        rvMqtt.addItemDecoration(
//            DividerItemDecoration(
//                activity,
//                layoutManager.orientation
////            )
//            RecyclerViewItemDecoration(
//                requireActivity(),
//                R.drawable.rv_divider
//            )
            RecyclerViewItemDecoration(
                this,
                R.drawable.rv_divider
            )
        )
    }
}