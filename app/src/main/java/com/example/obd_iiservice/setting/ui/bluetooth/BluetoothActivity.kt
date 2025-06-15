package com.example.obd_iiservice.setting.ui.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.bluetooth.BluetoothConnectionState
import com.example.obd_iiservice.bluetooth.BluetoothDeviceAdapter
import com.example.obd_iiservice.bluetooth.BluetoothDeviceItem
import com.example.obd_iiservice.bluetooth.ObserveConnectionBluetooth
import com.example.obd_iiservice.databinding.ActivityBluetoothBinding
import com.example.obd_iiservice.dtc.DTCActivity
import com.example.obd_iiservice.helper.makeToast
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.log.LogViewActivity
import com.example.obd_iiservice.obd.OBDForegroundService
import com.example.obd_iiservice.obd.ServiceState
import com.example.obd_iiservice.setting.SettingActivity
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
class BluetoothActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBluetoothBinding
    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
    private lateinit var rvBluetooth: RecyclerView
    private val listBluetoothDevice = mutableListOf<BluetoothDeviceItem>()
    private val bluetoothViewModel : BluetoothViewModel by viewModels()
    // Variabel untuk menyimpan menu agar bisa diakses nanti
    private var optionsMenu: Menu? = null
    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    private val REQUEST_PERMISSION = 2
    private val PERMISSION_REQUEST_BLUETOOTH = 1

    private lateinit var INTENT_SERVICE_STATE : Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Atur Toolbar sebagai ActionBar
        setSupportActionBar(binding.topAppBar)

        // Amati status koneksi dari ViewModel atau Repository
        initUI()
        observeConnectionStatus()
        observerConnection()
        INTENT_SERVICE_STATE = Intent(this, OBDForegroundService::class.java)

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
    }
    // 1. Inflate menu Anda ke AppBar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bluetooth_menu, menu)
        this.optionsMenu = menu
        return true
    }

    // 2. Metode ini dipanggil setiap kali menu akan ditampilkan.
    //    Ini adalah tempat terbaik untuk mengubah ikon secara dinamis.
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
//        val connectionItem = menu.findItem(R.id.action_connection_status)

        // Ambil status koneksi saat ini (misalnya dari ViewModel)
//        val isConnected = viewModel.isConnected.value
//
//        if (isConnected) {
//            connectionItem?.setIcon(R.drawable.ic_bluetooth_connected)
//        } else {
//            connectionItem?.setIcon(R.drawable.ic_bluetooth_disconnected)
//        }
        return super.onPrepareOptionsMenu(menu)
    }

    // 3. Tangani klik pada item menu (ikon Anda)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
//            R.id.action_connection_status -> {
//                // Logika saat ikon di-klik
//                // Misalnya: buka dialog koneksi, coba sambungkan ulang, dll.
//                viewModel.toggleConnection()
//                true
//            }
//            // Handle item menu lain jika ada
//            R.id.action_settings -> {
//                // Buka halaman setting
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeConnectionStatus() {
        lifecycleScope.launch {
//            viewModel.isConnected.collect { isConnected ->
//                // PENTING: Panggil invalidateOptionsMenu() setiap kali status berubah.
//                // Ini akan memaksa Android untuk memanggil onPrepareOptionsMenu() lagi
//                // dan menggambar ulang menu dengan ikon yang benar.
//                invalidateOptionsMenu()
//            }
        }
    }

    private fun initUI() {
        rvBluetooth = binding.rvListDevices
        rvBluetooth.setHasFixedSize(true)
        showRecycleView()

        // Atur OnClickListener HANYA SATU KALI di sini.
        binding.btnScanConnect.setOnClickListener {
            // Di dalam listener, kita cek state TERKINI dari ViewModel
            val currentState = bluetoothViewModel.connectionState.value
            val currentAddress = bluetoothViewModel.bluetoothAddress.value

            Log.d("ButtonClick", "Button clicked with state: $currentState")

            when (currentState) {
                BluetoothConnectionState.IDLE -> {
                    if (currentAddress == null) {
                        // Aksi untuk "Scan Devices..."
                        if (!bluetoothAdapter.isEnabled) {
                            val enableBtnIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            enableBluetoothLauncher.launch(enableBtnIntent)
                        } else {
                            checkAndRequestPermissions()
                        }
                    } else {
                        // Aksi untuk "Connect"
                        connectToDevice(currentAddress)
                    }
                }

                BluetoothConnectionState.CONNECTED -> {
                    // Aksi untuk "Disconnect"
                    disconnectOrClose()
                }

                BluetoothConnectionState.CONNECTING -> {
                    // Tombol dinonaktifkan, seharusnya tidak bisa diklik,
                    // tapi kita bisa tambahkan log untuk jaga-jaga.
                    Log.d("ButtonClick", "Clicked while connecting, no action.")
                }

                else -> {
                    // State lain, tidak melakukan apa-apa
                }
            }
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observerConnection(){
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    bluetoothViewModel.combineToReconnecting.collect { (reconnectingJob, address, connectionState, isAuto) ->
                        binding.rvListDevices.visibility = View.VISIBLE // Selalu terlihat kecuali saat connected

                        when (connectionState) {
                            BluetoothConnectionState.IDLE -> {
                                binding.btnScanConnect.visibility = View.VISIBLE
                                binding.btnScanConnect.isEnabled = true
                                if (address == null) {
                                    binding.btnScanConnect.text = "Scan Devices..."
                                } else {
                                    binding.btnScanConnect.text = "Connect"
                                }
                            }

                            BluetoothConnectionState.CONNECTING -> {
                                binding.btnScanConnect.visibility = View.VISIBLE
                                binding.btnScanConnect.isEnabled = false
                                binding.btnScanConnect.text = "Connecting..."
                            }

                            BluetoothConnectionState.CONNECTED -> {
                                binding.btnScanConnect.visibility = View.VISIBLE
                                binding.btnScanConnect.text = "Disconnect"
                                binding.btnScanConnect.isEnabled = !isAuto // Hanya bisa disconnect jika tidak auto-reconnect

                                // Sembunyikan daftar perangkat jika sudah terhubung
                                // binding.rvListDevices.visibility = View.GONE
                            }

                            BluetoothConnectionState.FAILED -> {
                                // Kembali ke IDLE, collector akan menangani UI-nya
                                bluetoothViewModel.updateConnectionState(BluetoothConnectionState.IDLE)
                            }
                        }
                    }
//                    bluetoothViewModel.combineToReconnecting.collect { (reconnectingJob, address, connectionState, isAuto) ->
//                        when(connectionState){
//                            BluetoothConnectionState.IDLE -> {
//                                //address kosong
//                                if (address == null) {
//                                    bluetoothViewModel.reconnectingJob.value?.cancel()
//                                    bluetoothViewModel.updateReconnectingJob(null)
//
//                                    binding.btnScanConnect.apply {
//                                        text = "Scan devices..."
//                                        visibility = View.VISIBLE
//                                        setOnClickListener {
//                                            if (!bluetoothAdapter.isEnabled) {
//                                                val enableBtnIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                                                enableBluetoothLauncher.launch(enableBtnIntent)
//                                            } else {
//                                                checkAndRequestPermissions()
//                                            }
//                                            makeToast(this@BluetoothActivity, "click scan")
//                                        }
//                                    }
//                                    binding.rvListDevices.visibility = View.VISIBLE
//                                }
//                                //address ada
//                                else {
//                                    when(isAuto){
//                                        //tidak auto reconnect
//                                        false -> {
//                                            binding.btnScanConnect.apply {
//                                                text = "Connect"
//                                                visibility = View.VISIBLE
//                                                setOnClickListener {
//                                                    connectToDevice(address)
//                                                    Log.d("connect", "klik konek buton")
//                                                }
//                                            }
//                                            binding.rvListDevices.visibility = View.VISIBLE
//                                        }
//                                        //auto reconnect
//                                        true -> {
//                                            //job reconnect kosong
//                                            if (reconnectingJob == null){
////                                                reconnectUntilSuccess(address)
//                                                bluetoothViewModel.updateReconnectingJob(reconnectUntilSuccess(address))
//                                                bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTING)
//                                            } else {
//                                                binding.btnScanConnect.apply {
//                                                    text = "Connecting"
//                                                    isEnabled = false
//                                                }
//                                            }
//                                            binding.rvListDevices.visibility = View.VISIBLE
//                                        }
//                                    }
//                                }
//                            }
//
//                            BluetoothConnectionState.CONNECTING -> {
//                                binding.btnScanConnect.apply {
//                                    text = "Connecting"
//                                    isEnabled = false
//                                }
//                            }
//                            BluetoothConnectionState.CONNECTED -> {
//                                when(isAuto){
//                                    false -> {
//                                        binding.btnScanConnect.apply {
//                                            text = "Connected"
//                                            isEnabled = true
//                                            setOnClickListener {
//                                                disconnectOrClose()
//                                            }
//                                        }
////                                        binding.rvListDevices.visibility = View.GONE
//                                    }
//
//                                    true -> {
//                                        binding.btnScanConnect.apply {
//                                            text = "Connected"
//                                            isEnabled = false
//                                        }
//                                    }
//                                }
//                            }
//                            BluetoothConnectionState.FAILED -> {
//                                bluetoothViewModel.updateConnectionState(BluetoothConnectionState.IDLE)
//                            }
//                        }
//                    }
                }

                launch {
                    bluetoothViewModel.bluetoothSocket.collect {
                        if (it != null && !it.isConnected){
                            disconnectOrClose()
                        }
                    }
                }

            }
        }
    }

    @SuppressLint("MissingPermission")
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
//            startDiscovery()
            checkAndRequestPermissions()
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
                override fun onSaveBluetoothDevice(address: String) {
//                    bluetoothViewModel
                    lifecycleScope.launch {
                        bluetoothViewModel.saveBluetoothAddress(address)
                    }
                }
            }
        )
        rvBluetooth.adapter = bluetoothDeviceAdapter
    }

//    private fun startScanProcess() {
//        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//
//        if (!isGpsEnabled) {
//            // Beri tahu pengguna untuk menyalakan GPS
//            Toast.makeText(this, "Tolong nyalakan Layanan Lokasi (GPS) untuk menemukan perangkat.", Toast.LENGTH_LONG).show()
//            // Anda juga bisa mengarahkan pengguna ke pengaturan lokasi
//            // startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//            return // Jangan lanjutkan scan jika GPS mati
//        }
//
//        // Jika GPS sudah nyala, baru lanjutkan ke startDiscovery()
//        startDiscovery()
//    }


    private fun checkAndRequestPermissions() {
        //check gps hidup atau tidak
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            // Beri tahu pengguna untuk menyalakan GPS
            Toast.makeText(this, "Tolong nyalakan Layanan Lokasi (GPS) untuk menemukan perangkat.", Toast.LENGTH_LONG).show()
            // Anda juga bisa mengarahkan pengguna ke pengaturan lokasi
            // startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return // Jangan lanjutkan scan jika GPS mati
        }

        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Untuk Android 12+
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            Log.d("Android Version", "12+")

        } else {
            // Untuk Android 11 dan di bawahnya, butuh izin lokasi
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            Log.d("Android Version", "11-")
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Minta semua izin yang dibutuhkan sekaligus
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_BLUETOOTH)
        } else {
            // Jika semua izin sudah ada, langsung jalankan prosesnya
            enableBluetooth()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
            // Cek apakah semua izin yang diminta telah diberikan
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Jika semua izin diberikan, lanjutkan prosesnya
                Log.d("Permissions", "All permissions granted. Starting process.")
                enableBluetooth()
            } else {
                // Jika ada izin yang ditolak, beri tahu pengguna
                Toast.makeText(this, "Permission is required to find bluetooth devices.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            startDiscovery()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startDiscovery(){
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        lifecycleScope.launch {
            if (!bluetoothViewModel.isReceiverRegistered.first()) {
                registerReceiver(receiver, filter)
                bluetoothViewModel.changeIsReceiverRegistered(true)
                Log.d("bluetooth", "isReceiverRegistered")
            }
        }
        Log.d("bluetooth", "Start discovery")
//        deviceList.clear()
        listBluetoothDevice.clear()
//        deviceListAdapter.notifyDataSetChanged()
        bluetoothDeviceAdapter.notifyDataSetChanged()
        bluetoothAdapter.startDiscovery()
        Toast.makeText(this, "Scanning bluetooth devices....", Toast.LENGTH_LONG).show()

    }

    private fun connectToDevice(address: String) {
        // UUID default untuk SPP (Serial Port Profile)
        Log.d("bluetooth", "connecting")
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
            }
//            return
        }
        Log.d("bluetooth", "connecting")

        lifecycleScope.launch {
            bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTING)
        }
        bluetoothViewModel.connectToDevice(
            address = address,
            onSuccess = {
                val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
//                binding.tvStatusBluetooth.text = device?.name ?: "Unknown"
                lifecycleScope.launch {
                    bluetoothViewModel.saveBluetoothAddress(address)
//                    obdViewModel.updateBluetoothConnection(true)
                    bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTED)
                }
                Toast.makeText(this, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
                saveLogToFile(this, "Connect Bluetooth", "OK", "Connected to ${device?.name}")
//                testObdConnection()
                startAndBindOBDService()
            },
            onError = { error ->
                lifecycleScope.launch {
                    bluetoothViewModel.updateConnectionState(BluetoothConnectionState.IDLE)
                }
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
            while (bluetoothViewModel.connectionState.value == BluetoothConnectionState.CONNECTING
                || bluetoothViewModel.connectionState.value == BluetoothConnectionState.IDLE) {
                Log.d("Bluetooth", "Attempt reconnect: $attempt")

                val success = bluetoothViewModel.connectToDeviceSuspend(address)

                if (success) {
                    val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
//                    binding.tvStatusBluetooth.text = device?.name ?: "Unknown"
                    Toast.makeText(this@BluetoothActivity, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
                    saveLogToFile(
                        this@BluetoothActivity,
                        "Connect Bluetooth",
                        "OK",
                        "Reconnected to ${device?.name}"
                    )
//                    testObdConnection()
                    startAndBindOBDService()
//                    obdViewModel.updateBluetoothConnection(true)
                    bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTED)
                    break // berhenti mencoba jika sudah terkoneksi
                } else {
                    Log.e("Bluetooth", "Reconnect failed")
                    bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTING)
                    saveLogToFile(
                        this@BluetoothActivity,
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

                    if (ActivityCompat.checkSelfPermission(this@BluetoothActivity, Manifest.permission.BLUETOOTH)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@BluetoothActivity,
                            arrayOf(Manifest.permission.BLUETOOTH),
                            REQUEST_PERMISSION
                        )
                        return
                    }

                    Log.d("bluetooth", "Bluetooth Action Found")

                    device?.let {
                        val name = it.name ?: "Unknown Device"
                        val address = it.address
                        val deviceItem = BluetoothDeviceItem(name = name, address = address)
                        Log.d("BluetoothScan", "Device found: $name - $address")
                        listBluetoothDevice.add(deviceItem)
                        Log.d("device", deviceItem.toString())
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
        // Start the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(INTENT_SERVICE_STATE)
        } else {
            startService(INTENT_SERVICE_STATE)
        }
    }

    private fun stopAndUnbindOBDService() {
        stopService(INTENT_SERVICE_STATE) // This will eventually call onDestroy in the service
        Log.d("MainActivity", "OBD Service stopped and unbound")
    }

    private fun disconnectOrClose(){
//        stopAndUnbindOBDService()
//        obdViewModel.stopReading()
        if (bluetoothViewModel.serviceState.value == ServiceState.RUNNING) {
            stopService(INTENT_SERVICE_STATE)
        }
        lifecycleScope.launch {
            bluetoothViewModel.updateConnectionState(BluetoothConnectionState.IDLE)
        }
        bluetoothViewModel.disconnect()
        listBluetoothDevice.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            unRegReceiver()
        }
    }
    suspend fun unRegReceiver(){
        if (bluetoothViewModel.isReceiverRegistered.first()){
            unregisterReceiver(receiver)
            bluetoothViewModel.changeIsReceiverRegistered(false)
        }
    }
}

