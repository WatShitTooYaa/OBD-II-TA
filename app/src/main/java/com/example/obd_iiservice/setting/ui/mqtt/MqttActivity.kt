package com.example.obd_iiservice.setting.ui.mqtt

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.ActivityMqttBinding
import com.example.obd_iiservice.setting.ui.bluetooth.BluetoothActivity
import com.example.obd_iiservice.setting.ui.threshold.ThresholdActivity
import com.example.obd_iiservice.ui.decoration.RecyclerViewItemDecoration
import com.example.obd_iiservice.ui.setting.SettingAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

//import com.example.obd_iiservice.ui.setting.MqttItem

@AndroidEntryPoint
class MqttActivity : AppCompatActivity(), MqttAdapter.OnMqttItemClickListener {
    private lateinit var binding : ActivityMqttBinding
    private lateinit var rvMqtt : RecyclerView
    private lateinit var mqttAdapter: MqttAdapter

    private val mqttViewModel : MqttViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMqttBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_mqtt)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView(){
        mqttAdapter = MqttAdapter(this)
        rvMqtt = binding.rvMqtt
        rvMqtt.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvMqtt.layoutManager = layoutManager
        // Observe UI state
        lifecycleScope.launch {
            mqttViewModel.uiState.collect { list ->
                mqttAdapter.submitList(list)
            }
        }
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
            RecyclerViewItemDecoration(
                this,
                R.drawable.rv_divider
            )
        )
    }

    override fun onItemClicked(item: MqttItem) {
        when (item.action) {
            MqttAction.EDIT_HOST,
            MqttAction.EDIT_TOPIC,
            MqttAction.EDIT_USERNAME ->
                showEditTextDialog(item, InputType.TYPE_CLASS_TEXT)

            MqttAction.EDIT_PASSWORD ->
                showEditTextDialog(item, InputType.TYPE_TEXT_VARIATION_PASSWORD)

            MqttAction.EDIT_PORT ->
                showEditTextDialog(item, InputType.TYPE_CLASS_NUMBER)

            MqttAction.EDIT_PORT_TYPE ->
                showPortTypeSelectionDialog(item)
        }
    }

    // --- Fungsi Helper untuk Dialog ---

    private fun showEditTextDialog(item: MqttItem, inputType: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.et_edit_item)
        editText.inputType = inputType

        // Ambil nilai terbaru langsung dari state untuk di-edit
        val currentValue = item.displayValue.takeIf { it != "Tidak diatur" && it != "********" } ?: ""
        editText.setText(currentValue)
        editText.hint = item.title

        AlertDialog.Builder(this)
            .setTitle("Edit ${item.title}")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val newValue = editText.text.toString()
                val valueToSave: Any = if (inputType == InputType.TYPE_CLASS_NUMBER) {
                    newValue.toIntOrNull() ?: 0 // Konversi ke Int untuk Port
                } else {
                    newValue // String untuk yang lain
                }
                mqttViewModel.handleAction(item.action, valueToSave)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showPortTypeSelectionDialog(item: MqttItem) {
        val types = arrayOf("tcp", "tls")
        val currentIndex = types.indexOf(item.displayValue)

        AlertDialog.Builder(this)
            .setTitle("Pilih Tipe Koneksi")
            .setSingleChoiceItems(types, currentIndex) { dialog, which ->
                val selectedType = types[which]
                mqttViewModel.handleAction(item.action, selectedType)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}