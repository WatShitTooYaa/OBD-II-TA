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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
//    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
//    private val deviceList = mutableListOf<String>()
    private lateinit var rvBluetooth: RecyclerView
    private val listBluetoothDevice = mutableListOf<BluetoothDeviceItem>()

    private val REQUEST_ENABLE_BTN = 1
    private val REQUEST_PERMISSION = 2
    private val PERMISSION_REQUEST_BLUETOOTH = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val btnScan = binding.btnScan
//        val listView = binding.lvDevices
        rvBluetooth = binding.rvListDevices
        rvBluetooth.setHasFixedSize(true)
        showRecycleView()

//        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
//        listView.adapter = deviceListAdapter

//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Not supported Bluetooth", Toast.LENGTH_LONG).show()
            finish()
        }

        btnScan.setOnClickListener {
            checkAndRequestPermissions()
//            enableBluetooth()
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
//        val uuid = device.uuids?.firstOrNull {
//            it.uuid.toString().lowercase() == "00001101-0000-1000-8000-00805f9b34fb" ||
//                    it.uuid.toString().lowercase() == "c7f94713-891e-496a-a0e7-983a0946126e"
//        }?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
//        val socket = device.createRfcommSocketToServiceRecord(uuid)


        Thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter.cancelDiscovery() // sangat penting!
                socket.connect()
                testObdConnection(socket)

                runOnUiThread {
                    Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                }

                // Gunakan socket.inputStream dan socket.outputStream untuk komunikasi data di sini

            } catch (e: IOException) {
                e.printStackTrace()
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

    private fun testObdConnection(bluetoothSocket: BluetoothSocket) {
        val TAG = "OBDTest"

//        try {
//            val outputStream = socket.outputStream
//            val inputStream = socket.inputStream
//
//            outputStream.write("ATZ\r".toByteArray())
//            outputStream.flush()
//            Thread.sleep(500) // beri waktu reset
//
//            outputStream.write("ATE0\r".toByteArray())
//            outputStream.flush()
//            Thread.sleep(100)
//
//            outputStream.write("ATL0\r".toByteArray())
//            outputStream.flush()
//            Thread.sleep(100)
//
//            outputStream.write("010C\r".toByteArray())
//            outputStream.flush()
//
////            val reader = BufferedReader(InputStreamReader(inputStream))
////            val writer = OutputStreamWriter(outputStream)
////
////
////            sendCommand("ATZ", writer)
////            Log.d("OBD", readResponse(reader))
////
////            sendCommand("ATE0", writer)
////            Log.d("OBD", readResponse(reader))
////
////            sendCommand("ATL0", writer)
////            Log.d("OBD", readResponse(reader))
////
////            sendCommand("010C", writer) // request RPM
////            Log.d(TAG, "OBD Response: $rpmRaw")
////            val rpmRaw = readResponse(reader)
//            // Baca response
//            val buffer = ByteArray(1024)
//            val bytes = inputStream.read(buffer)
//            val response = String(buffer, 0, bytes)
//            Log.d(TAG, "OBD Response: $response")
//
//
//        } catch (e: IOException) {
//            Log.e(TAG, "Error reading OBD response: ${e.message}")
//        }
        Thread {
            try {
                val socket = bluetoothSocket // socket yang sudah terkoneksi
                val output = socket.outputStream
                val input = socket.inputStream
                val buffer = ByteArray(1024)

                // Setup awal
                val commands = listOf("ATZ", "ATE0", "ATL0", "ATH0") // reset, echo off, linefeed off, headers off
                for (cmd in commands) {
                    output.write((cmd + "\r").toByteArray())
                    output.flush()
                    Thread.sleep(300)
                    input.read(buffer) // baca & buang respons setup
                }

                while (true) {
                    // Kirim command RPM (010C)
                    output.write("010C\r".toByteArray())
                    output.flush()

                    Thread.sleep(500) // tunggu respons

                    val bytesRead = input.read(buffer)
                    if (bytesRead > 0) {
                        val rawResponse = String(buffer, 0, bytesRead).replace("\r", "").replace("\n", "")
                        Log.d("OBD", "Raw Response: $rawResponse")

                        // Cari respons "41 0C xx yy"
                        val match = Regex("41 0C ([0-9A-Fa-f]{2}) ([0-9A-Fa-f]{2})").find(rawResponse)
                        if (match != null) {
                            val a = match.groupValues[1].toInt(16)
                            val b = match.groupValues[2].toInt(16)
                            val rpm = ((a * 256) + b) / 4
                            Log.d("OBD", "RPM: $rpm")
                        } else {
                            Log.d("OBD", "RPM response not found")
                        }
                    }

                    Thread.sleep(1000) // delay antar request
                }
            } catch (e: Exception) {
                Log.e("OBD", "Error reading RPM", e)
            }
        }.start()

    }

    fun sendCommand(command: String, writer: OutputStreamWriter) {
        writer.write(command + "\r")
        writer.flush()
    }

    fun readResponse(reader : BufferedReader): String {
        val response = StringBuilder()
        var line: String?
        do {
            line = reader.readLine()
            if (line != null && line.isNotBlank()) {
                response.appendLine(line)
            }
        } while (line != null && !line.contains(">")) // '>' adalah prompt dari ELM
        return response.toString()
    }


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            if (BluetoothDevice.ACTION_FOUND == p1?.action){

                val device : BluetoothDevice? = if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    p1.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                }
//                val device : BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                if(ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH), REQUEST_PERMISSION)
                    return
                }
                device?.let {
                    val name = it.name ?: "Unknown Device"
                    val address = it.address
//                    val info = "$name\n$address"
//                    val listDeviceItem = ArrayList<BluetoothDeviceItem>()
                    var deviceItem = BluetoothDeviceItem(
                        name = name,
                        address = address
                    )
                    Log.d("BluetoothScan", "Device found: $name - $address")
                    listBluetoothDevice.add(deviceItem)
//                    bluetoothDeviceAdapter.notifyDataSetChanged()
                    bluetoothDeviceAdapter.notifyItemInserted(listBluetoothDevice.size-1)
//                    if (!deviceList.contains(info)) {
//                        deviceList.add(info)
//                        deviceListAdapter.notifyDataSetChanged()
//                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
