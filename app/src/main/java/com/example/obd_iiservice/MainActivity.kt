package com.example.obd_iiservice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.bluetooth.BluetoothDeviceAdapter
import com.example.obd_iiservice.bluetooth.BluetoothDeviceItem
import com.example.obd_iiservice.bluetooth.BluetoothViewModel
import com.example.obd_iiservice.bluetooth.ObserveConnectionBluetooth
import com.example.obd_iiservice.databinding.ActivityMainBinding
import com.example.obd_iiservice.dtc.DTCActivity
import com.example.obd_iiservice.helper.makeToast
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.log.LogViewActivity
import com.example.obd_iiservice.obd.OBDForegroundService
import com.example.obd_iiservice.obd.OBDViewModel
import com.example.obd_iiservice.setting.SettingActivity
import com.example.obd_iiservice.setting.SettingViewModel
import com.example.obd_iiservice.threshold.ThresholdActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
    private lateinit var rvBluetooth: RecyclerView
    private val listBluetoothDevice = mutableListOf<BluetoothDeviceItem>()
    private val bluetoothViewModel : BluetoothViewModel by viewModels()
    private val obdViewModel : OBDViewModel by viewModels()
    private val settingViewModel: SettingViewModel by viewModels()
    private val mainViewModel : MainViewModel by viewModels()

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter


    private val REQUEST_PERMISSION = 2
    private val PERMISSION_REQUEST_BLUETOOTH = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

//        rvBluetooth = binding.rvListDevices
//        rvBluetooth.setHasFixedSize(true)
//        showRecycleView()

//        serviceIntent = obdViewModel.serviceIntent.value

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
//                Toast.makeText(this@MainActivity, "menenak tombol kembali", Toast.LENGTH_LONG).show()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Keluar")
                    .setMessage("Apakah anda ingin keluar dari aplikasi?")
                    .setPositiveButton("Ya") { _,_ ->
                        obdViewModel.stopReading()
                        lifecycleScope.launch {
//                            if (bluetoothViewModel.isReceiverRegistered.first() == true){
//                                unregisterReceiver(receiver)
//                            }
                            unRegReceiver()
                        }
                        bluetoothViewModel.disconnect()
                        finish()
                    }
                    .setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
        })

        val bluetoothManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Not supported Bluetooth", Toast.LENGTH_LONG).show()
            finish()
        }

        initUI()
        observerViewModel()
    }

    private fun initUI() {
        rvBluetooth = binding.rvListDevices
        rvBluetooth.setHasFixedSize(true)
        showRecycleView()

        binding.btnScanIfAutoReconFalse.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnScanIfAutoReconTrue.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnConnectIfAutoReconFalse.setOnClickListener {
            lifecycleScope.launch {
                settingViewModel.isInitialized
                    .filter { it } // hanya lanjut kalau true
                    .first()
                val canConnect = settingViewModel.checkDataForConnecting()
                val address = settingViewModel.bluetoothAddress.first()
//                makeToast(this@MainActivity, "top : $topic, p: $port, ho: $host, adress : $address")
                if (!bluetoothAdapter.isEnabled) {
                    val enableBtnIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtnIntent)
                } else {
                    if (canConnect && address != null){
                        connectToDevice(address)
                    } else {
                        makeToast(this@MainActivity, "topic atau port tidak boleh kosong")
                    }
                }
            }
        }

        binding.btnDisconnectIfAutoReconFalse.setOnClickListener {
            disconnectOrClose()
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId) {
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_log -> {
                    val intent = Intent(this, LogViewActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_dtc -> {
                    val intent = Intent(this, DTCActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_treshold -> {
                    val intent = Intent(this, ThresholdActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_quit -> {
                    disconnectOrClose()
                    finish()
                    true
                }
                else -> false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observerViewModel(){
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    settingViewModel.mqttAutoRecon.collect { isAuto ->
                        if (isAuto){
                            binding.clContainerButtonIfAutoReconTrue.visibility = View.VISIBLE
                            binding.clContainerButtonIfAutoReconFalse.visibility = View.GONE
                        } else {
                            binding.clContainerButtonIfAutoReconTrue.visibility = View.GONE
                            binding.clContainerButtonIfAutoReconFalse.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    bluetoothViewModel.bluetoothSocket.collect {
                        if (it != null && it.isConnected == false){
                            disconnectOrClose()
                        }
                    }
                }

                launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        obdViewModel.obdData.collect { data ->
                            binding.tvDataRpm.text = data["Rpm"]?.toIntOrNull()?.toString() ?: "-"
                            binding.tvDataSpeed.text = data["Speed"]?.toIntOrNull()?.toString() ?: "-"
                            binding.tvDataThrottle.text = data["Throttle"]?.toIntOrNull()?.toString() ?: "-"
                            binding.tvDataTemp.text = data["Temp"]?.toIntOrNull()?.toString() ?: "-"
                            binding.tvDataMaf.text = data["Maf"]?.toDoubleOrNull()?.toString() ?: "-"
                        }
                    }
                }

                launch {
                    combine(
                        settingViewModel.bluetoothAddress,
                        bluetoothViewModel.isConnected,
                        settingViewModel.mqttAutoRecon,
                        bluetoothViewModel.reconnectingJob
                    ) { address, isConnected, isAuto, reconnectingJob ->
                        // Combine jadi Quadruple
                        ObserveConnectionBluetooth(address, isConnected, isAuto, reconnectingJob)
                    }.collect { (address, isConnected, isAuto, reconnectingJob) ->
                        if (address != null && !isConnected && isAuto) {
                            if (reconnectingJob?.isActive != true && settingViewModel.checkDataForConnecting()) {
                                makeToast(this@MainActivity, "recon")
                                bluetoothViewModel.updateReconnectingJob(reconnectUntilSuccess(address))
                            } else if (settingViewModel.checkDataForConnecting() == false){
                                makeToast(this@MainActivity, "topic atau port tidak boleh kosong")
                                saveLogToFile(
                                    this@MainActivity,
                                    "reconnect",
                                    "ERROR",
                                    "Topic atau port tidak boleh kosong"
                                )
                                delay(1000)
                            } else {
                                makeToast(this@MainActivity, "error saat akan reconnecting otomatis")
                                saveLogToFile(
                                    this@MainActivity,
                                    "reconnect",
                                    "ERROR",
                                    "error saat akan reconnecting otomatis")
                            }
                        } else {
                            bluetoothViewModel.updateReconnectingJob(null)
                        }

                        // Pengecekan untuk disconnect jika address == null atau isAuto berubah
                        val addressChangedToNull = bluetoothViewModel.previousAddress.first() != address && address == null
                        val isAutoChanged = bluetoothViewModel.previousIsAuto.first() != isAuto

                        if ((addressChangedToNull || isAutoChanged) && (address == null || !isAuto)) {
                            makeToast(this@MainActivity, "disconnect when address == null or isAuto == false")
                            disconnectOrClose()
                        }

                        // Simpan nilai saat ini sebagai nilai sebelumnya
//                        previousAddress = address
//                        previousIsAuto = isAuto
                        address?.let { bluetoothViewModel.updatePreviousAddress(it) }
                        isAuto.let { bluetoothViewModel.updatePreviousIsAuto(it) }

                        address?.let { binding.tvAddressBluetooth.text = it } ?: "-"
                        // Update UI
                        //jika address tidak ada dan terputus (kondisi awal)
                        if (address == null && !isConnected){
                            makeToast(this@MainActivity, "address tidak ada dan terputus")
                            binding.rvListDevices.visibility = View.VISIBLE
                            binding.llDashboard.visibility = View.GONE
                            binding.btnScanIfAutoReconTrue.visibility = View.VISIBLE
                            binding.btnScanIfAutoReconFalse.visibility = View.VISIBLE
                            binding.rvListDevices.visibility = View.VISIBLE
                            binding.btnConnectIfAutoReconFalse.visibility = View.GONE
                        }
                        //jika address ada dan terhubung
                        else if (address != null && isConnected) {
                            makeToast(this@MainActivity, "address ada dan terhubung")
                            if (isAuto){
//                                binding.btnScanIfAutoReconFalse.visibility = View.GONE
//                                binding.btnDisconnectIfAutoReconFalse.visibility = View.VISIBLE
                                binding.btnConnectIfAutoReconTrue.visibility = View.VISIBLE
                                binding.btnConnectIfAutoReconTrue.text = "Connected"
                            } else {
                                binding.btnDisconnectIfAutoReconFalse.visibility = View.VISIBLE

                            }
                            binding.btnScanIfAutoReconFalse.visibility = View.GONE
                            binding.btnScanIfAutoReconTrue.visibility = View.GONE
                            binding.rvListDevices.visibility = View.GONE
                            binding.btnConnectIfAutoReconFalse.visibility = View.GONE
                            binding.rvListDevices.visibility = View.GONE
                            binding.llDashboard.visibility = View.VISIBLE
                        }
                        //jika address ada namun terputus
                        else if (address != null && !isConnected){
                            makeToast(this@MainActivity, "address ada namun terputus")

                            if (!isAuto){
                                binding.btnConnectIfAutoReconFalse.visibility = View.VISIBLE
                                binding.btnDisconnectIfAutoReconFalse.visibility = View.GONE
                            } else {
                                binding.btnScanIfAutoReconTrue.visibility = View.GONE
                                binding.btnConnectIfAutoReconTrue.visibility = View.VISIBLE
                                binding.btnConnectIfAutoReconTrue.isEnabled = false
                                binding.btnConnectIfAutoReconTrue.text = "Reconnecting..."
                            }
                            binding.rvListDevices.visibility = View.GONE
                            binding.llDashboard.visibility = View.GONE
//                            binding.rvListDevices.visibility = View.VISIBLE
//                            binding.llDashboard.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
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
//                override fun onConnect(address: String) {
//                    connectToDevice(address)
//                }

                override fun onSaveBluetoothDevice(address: String) {
//                    bluetoothViewModel
                    lifecycleScope.launch {
                        settingViewModel.saveBluetoothAddress(address)
                    }
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
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        lifecycleScope.launch {
            if (bluetoothViewModel.isReceiverRegistered.first() == false) {
                registerReceiver(receiver, filter)
                bluetoothViewModel.changeIsReceiverRegistered(true)
            }
        }

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
        // UUID default untuk SPP (Serial Port Profile)
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
            }
            return
        }
        bluetoothViewModel.connectToDevice(
            address = address,
            onSuccess = {
                val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
                binding.tvStatusBluetooth.text = device?.name ?: "Unknown"
                lifecycleScope.launch {
                    settingViewModel.saveBluetoothAddress(address)
                    obdViewModel.updateBluetoothConnection(true)
                }
                Toast.makeText(this, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
                saveLogToFile(this, "Connect Bluetooth", "OK", "Connected to ${device?.name}")
//                testObdConnection()
                startAndBindOBDService()
            },
            onError = { error ->
                Toast.makeText(this, "Connection failed: $error", Toast.LENGTH_LONG).show()
                saveLogToFile(this, "Connect Bluetooth", "ERROR", "Connection failed: $error")
                Log.e("Bluetooth", "Connection failed: $error")
            }
        )
    }


    private fun reconnectUntilSuccess(address: String): Job? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
            }
            return null
        }

        return lifecycleScope.launch {
            var attempt = 0
//            val maxAttempts = 5
            while (bluetoothViewModel.isConnected.value == false) {
                Log.d("Bluetooth", "Attempt reconnect: $attempt")

                val success = bluetoothViewModel.connectToDeviceSuspend(address)

                if (success) {
                    val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
                    binding.tvStatusBluetooth.text = device?.name ?: "Unknown"
                    Toast.makeText(this@MainActivity, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
                    saveLogToFile(
                        this@MainActivity,
                        "Connect Bluetooth",
                        "OK",
                        "Reconnected to ${device?.name}"
                    )
//                    testObdConnection()
                    startAndBindOBDService()
                    obdViewModel.updateBluetoothConnection(true)
                    break // berhenti mencoba jika sudah terkoneksi
                } else {
                    Log.e("Bluetooth", "Reconnect failed")
                    saveLogToFile(
                        this@MainActivity,
                        "Reconnect Bluetooth",
                        "ERROR",
                        "Reconnect ke-${attempt} gagal"
                    )
                }

                attempt++
                delay(3000)
            }
        }
    }


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    }

                    if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.BLUETOOTH),
                            REQUEST_PERMISSION
                        )
                        return
                    }

                    device?.let {
                        val name = it.name ?: "Unknown Device"
                        val address = it.address
                        val deviceItem = BluetoothDeviceItem(name = name, address = address)
                        Log.d("BluetoothScan", "Device found: $name - $address")
                        listBluetoothDevice.add(deviceItem)
                        bluetoothDeviceAdapter.notifyItemInserted(listBluetoothDevice.size - 1)
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d("BluetoothState", "Bluetooth is OFF")
                            disconnectOrClose()
                            // Misalnya update UI atau beri notifikasi ke user
                        }
                        BluetoothAdapter.STATE_ON -> {
                            Log.d("BluetoothState", "Bluetooth is ON")
                        }
                    }
                }
            }
        }
    }

    private fun startAndBindOBDService() {
        val serviceIntent = Intent(this, OBDForegroundService::class.java)
        // Start the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopAndUnbindOBDService() {
        val serviceIntent = Intent(this, OBDForegroundService::class.java)
        stopService(serviceIntent) // This will eventually call onDestroy in the service
        Log.d("MainActivity", "OBD Service stopped and unbound")
    }

    private fun disconnectOrClose(){
        stopAndUnbindOBDService()
        obdViewModel.stopReading()
        if (obdViewModel.serviceIntent.value != null) {
            stopService(obdViewModel.serviceIntent.value)
        }
        lifecycleScope.launch {
            obdViewModel.updateBluetoothConnection(false)
            mainViewModel.updateCurrentStreamId(null)
            mainViewModel.updateIsPlaying(false)
        }
        bluetoothViewModel.disconnect()
        listBluetoothDevice.clear()
    }

    override fun onStop() {
        super.onStop()

        lifecycleScope.launch {
//            unRegReceiver()
            bluetoothViewModel.updateReconnectingJob(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        lifecycleScope.launch {
//            unRegReceiver()
//        }
    }

    suspend fun unRegReceiver(){
        if (bluetoothViewModel.isReceiverRegistered.first() == true){
            unregisterReceiver(receiver)
            bluetoothViewModel.changeIsReceiverRegistered(false)
        }
    }
}
