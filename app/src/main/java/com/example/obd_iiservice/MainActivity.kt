package com.example.obd_iiservice

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
    private lateinit var rvBluetooth: RecyclerView
    private val listBluetoothDevice = mutableListOf<BluetoothDeviceItem>()
    private val bluetoothViewModel : BluetoothViewModel by viewModels()


    private val REQUEST_PERMISSION = 2
    private val PERMISSION_REQUEST_BLUETOOTH = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val btnScan = binding.btnScan
        rvBluetooth = binding.rvListDevices
        rvBluetooth.setHasFixedSize(true)
        showRecycleView()


        bluetoothViewModel.isConnected.observe(this) { connected ->
            if (connected){
                binding.btnScan.visibility = View.GONE
                binding.btnDisconnect.visibility = View.VISIBLE
                binding.rvListDevices.visibility = View.GONE
                binding.llDashboard.visibility = View.VISIBLE
            } else {
                binding.btnScan.visibility = View.VISIBLE
                binding.btnDisconnect.visibility = View.GONE
                binding.rvListDevices.visibility = View.VISIBLE
                binding.llDashboard.visibility = View.GONE
            }
        }

        val bluetoothManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Not supported Bluetooth", Toast.LENGTH_LONG).show()
            finish()
        }

        btnScan.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnDisconnect.setOnClickListener {
            bluetoothViewModel.disconnect()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == RESULT_OK) {
            startDiscovery()
        } else {
            Toast.makeText(this, "Bluetooth tidak diaktifkan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRecycleView() {
        rvBluetooth.layoutManager = LinearLayoutManager(this)
//        val listBluetoothAdapter = BluetoothDeviceAdapter(listDevice)
        bluetoothDeviceAdapter = BluetoothDeviceAdapter(
            listBluetoothDevice,
            object : BluetoothDeviceAdapter.OnDeviceConnectListener {
                override fun onConnect(address: String) {
                    connectToDevice(address)
                }
            }
        )
        rvBluetooth.adapter = bluetoothDeviceAdapter
    }

    private fun checkAndRequestPermissions() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = mutableListOf<String>()
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (permissions.isNotEmpty()) {
                requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_BLUETOOTH)
            } else {
//                startDiscovery() // Aman langsung scanning
                enableBluetooth()
            }
        } else {
//            startDiscovery(
            enableBluetooth()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startDiscovery()
//                enableBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            startDiscovery()
        }
    }

    private fun startDiscovery(){
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(receiver, filter)

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION)
            return
        }
//        deviceList.clear()
        listBluetoothDevice.clear()
//        deviceListAdapter.notifyDataSetChanged()
        bluetoothDeviceAdapter.notifyDataSetChanged()
        bluetoothAdapter.startDiscovery()
        Toast.makeText(this, "Scanning bluetooth devices....", Toast.LENGTH_LONG).show()

    }

    private fun connectToDevice(address: String) {
        val device = bluetoothAdapter.getRemoteDevice(address)
        device.uuids?.forEach {
            Log.d("Bluetooth", "Device UUID: ${it.uuid}")
        }

        // UUID default untuk SPP (Serial Port Profile)
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
            }
            return
        }
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        Thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter.cancelDiscovery() // sangat penting!
                socket?.connect()
                bluetoothViewModel.bluetoothSocket = socket
                runOnUiThread {
                    binding.tvStatusBluetooth.text = device.name
                    Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                }

                testObdConnection()

            } catch (e: IOException) {
                e.printStackTrace()
                bluetoothViewModel.bluetoothSocket = null
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Failed to connect to ${device.name}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("Bluetooth", "Connection failed: ${e.message}", e)
                }
            }
        }.start()
    }

    private fun testObdConnection() {

        val pidList = listOf(
            "010C", // RPM
            "010D", // Speed
            "0111", // Throttle
            "0105", // Coolant Temp
            "0110"  // MAF
        )

        Thread {
            try {
                val output = bluetoothViewModel.bluetoothSocket?.outputStream
                val input = bluetoothViewModel.bluetoothSocket?.inputStream
                val buffer = ByteArray(1024)
                var rpm = 0
                var speed = 0
                var throttle = 0
                var temp = 0
                var maf = 0.0

                // Inisialisasi (sama seperti sebelumnya)
                val initCmds = listOf("ATZ", "ATE0", "ATL0", "ATH0")
                for (cmd in initCmds) {
                    if (output != null && input != null) {
                        output.write((cmd + "\r").toByteArray())
                        output.flush()
                        Thread.sleep(300)
                        input.read(buffer)
                    }
                }

                while (output != null && input != null) {
                    for (pid in pidList) {
                        output.write((pid + "\r").toByteArray())
                        output.flush()
                        Thread.sleep(300)

                        val bytesRead = input.read(buffer)
                        if (bytesRead > 0) {
                            val response = String(buffer, 0, bytesRead).replace("\r", "").replace("\n", "")
                            Log.d("OBD", "Response for $pid: $response")

                            when (pid) {
                                "010C" -> Regex("41 0C ([0-9A-Fa-f]{2}) ([0-9A-Fa-f]{2})").find(response)?.let {
                                    rpm = ((it.groupValues[1].toInt(16) * 256) + it.groupValues[2].toInt(16)) / 4
                                    Log.d("OBD", "RPM: $rpm")

                                }
                                "010D" -> Regex("41 0D ([0-9A-Fa-f]{2})").find(response)?.let {
                                    speed = it.groupValues[1].toInt(16)
                                    Log.d("OBD", "Speed: $speed km/h")
                                }
                                "0111" -> Regex("41 11 ([0-9A-Fa-f]{2})").find(response)?.let {
                                    throttle = (it.groupValues[1].toInt(16) * 100) / 255
                                    Log.d("OBD", "Throttle: $throttle%")
                                }
                                "0105" -> Regex("41 05 ([0-9A-Fa-f]{2})").find(response)?.let {
                                    temp = it.groupValues[1].toInt(16) - 40
                                    Log.d("OBD", "Coolant Temp: $temp°C")
                                }
                                "0110" -> Regex("41 10 ([0-9A-Fa-f]{2}) ([0-9A-Fa-f]{2})").find(response)?.let {
                                    maf = ((it.groupValues[1].toInt(16) * 256) + it.groupValues[2].toInt(16)) / 100.0
                                    Log.d("OBD", "MAF: $maf g/s")
                                }
                            }
                            runOnUiThread {
                                binding.tvDataRpm.text = "$rpm"
                                binding.tvDataSpeed.text = "$speed"
                                binding.tvDataThrottle.text = "$throttle"
                                binding.tvDataTemp.text = "$temp"
                                binding.tvDataMaf.text = "$maf"
                            }
                        }
                    }
                    Thread.sleep(500) // delay antar loop
                }
            } catch (e: Exception) {
                Log.e("OBD", "Error reading PIDs", e)
                Log.e("Bluetooth", "Connection failed", e)
                bluetoothViewModel.disconnect()
            }
        }.start()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            if (BluetoothDevice.ACTION_FOUND == p1?.action){

                val device : BluetoothDevice? = if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    p1.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                }
                if(ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH), REQUEST_PERMISSION)
                    return
                }
                device?.let {
                    val name = it.name ?: "Unknown Device"
                    val address = it.address
                    var deviceItem = BluetoothDeviceItem(
                        name = name,
                        address = address
                    )
                    Log.d("BluetoothScan", "Device found: $name - $address")
                    listBluetoothDevice.add(deviceItem)
                    bluetoothDeviceAdapter.notifyItemInserted(listBluetoothDevice.size-1)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothViewModel.disconnect()
        unregisterReceiver(receiver)
    }
}
