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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
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
import com.example.obd_iiservice.R
import com.example.obd_iiservice.bluetooth.BluetoothConnectionState
import com.example.obd_iiservice.bluetooth.BluetoothDeviceAdapter
import com.example.obd_iiservice.bluetooth.BluetoothDeviceItem
import com.example.obd_iiservice.databinding.ActivityBluetoothBinding
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.obd.OBDForegroundService
import com.example.obd_iiservice.obd.ServiceState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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
        supportActionBar?.setDisplayShowTitleEnabled(false)

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
        val isAuto = bluetoothViewModel.isAutoRecon.value
        val reconnectItem = menu.findItem(R.id.action_reconnect_status)

        val actionView = reconnectItem?.actionView

        val iconBackground = actionView?.findViewById<ImageView>(R.id.icon_background)

        // Ambil status koneksi saat ini (misalnya dari ViewModel)
//
        if (isAuto) {
            iconBackground?.visibility = View.VISIBLE
        } else {
            iconBackground?.visibility = View.INVISIBLE
        }

        actionView?.setOnClickListener {
            bluetoothViewModel.changeAutoReconnect(!isAuto)

            lifecycleScope.launch {
                bluetoothViewModel.updateReconnectingJob(null)
            }
            binding.btnScanConnect.text = "Connect"
        }
        return super.onPrepareOptionsMenu(menu)
    }

    // 3. Tangani klik pada item menu (ikon Anda)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
//            R.id.action_reconnect_status -> {
//                val isAuto = bluetoothViewModel.isAutoRecon.value
//                lifecycleScope.launch {
//                    bluetoothViewModel.changeAutoReconnect(!isAuto)
//                }
//                true
//            }
            R.id.action_reset_status -> {
                AlertDialog.Builder(this)
                    .setTitle("Reset")
                    .setMessage("Reset connection?")
                    .setPositiveButton("Yes") { _,_ ->
                        lifecycleScope.launch {
                            launch {
                                bluetoothViewModel.saveBluetoothAddress(null)
                            }
                            launch {
                                disconnectOrClose()
                            }
                        }
                    }
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeConnectionStatus() {
        lifecycleScope.launch {
            bluetoothViewModel.isAutoRecon.collect { isAuto ->
                // PENTING: Panggil invalidateOptionsMenu() setiap kali status berubah.
                // Ini akan memaksa Android untuk memanggil onPrepareOptionsMenu() lagi
                // dan menggambar ulang menu dengan ikon yang benar.
                invalidateOptionsMenu()
            }
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
                        checkAndRequestPermissions()
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
                    bluetoothViewModel.combineToReconnecting.collectLatest { (reconnectingJob, address, connectionState, isAuto) ->
                        binding.rvListDevices.visibility = View.GONE // Selalu terlihat kecuali saat connected

                        when (connectionState) {
                            BluetoothConnectionState.IDLE -> {
                                binding.btnScanConnect.visibility = View.VISIBLE
                                binding.btnScanConnect.isEnabled = true
                                if (address == null) {
                                    Log.d("combine reconnect", "address null")
                                    binding.btnScanConnect.text = "Scan Devices..."
                                    binding.rvListDevices.visibility = View.VISIBLE
                                }
                                //ada address
                                else {
                                    //auto reconnect hidup
                                    if (isAuto){
                                        //job reconnect null
                                        if (reconnectingJob == null){
                                            lifecycleScope.launch {
                                                launch {
                                                    bluetoothViewModel.updateReconnectingJob(reconnectUntilSuccess(address))
                                                }
                                                launch {
                                                    bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTING)
                                                }
                                            }
                                        }
                                    }
                                    //tidak auto reconnect
                                    else{
                                        binding.btnScanConnect.text = "Connect"

                                    }
//                                    Log.d("combine reconnect", "address not null")
                                }
                            }

                            BluetoothConnectionState.CONNECTING -> {
                                binding.btnScanConnect.visibility = View.VISIBLE
                                binding.btnScanConnect.isEnabled = false
                                binding.btnScanConnect.text = "Connecting..."
                            }

                            BluetoothConnectionState.CONNECTED -> {
                                binding.btnScanConnect.visibility = View.VISIBLE
                                binding.btnScanConnect.isEnabled = !isAuto // Hanya bisa disconnect jika tidak auto-reconnect
                                binding.btnScanConnect.text = "Disconnect"
                                if (isAuto){
                                    binding.btnScanConnect.text = "Connected"
                                }
                                // Sembunyikan daftar perangkat jika sudah terhubung
//                                 binding.rvListDevices.visibility = View.GONE
                            }

                            BluetoothConnectionState.FAILED -> {
                                // Kembali ke IDLE, collector akan menangani UI-nya
                                lifecycleScope.launch {
                                    bluetoothViewModel.updateConnectionState(BluetoothConnectionState.IDLE)
                                }
                            }
                        }
                    }
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
) { result ->
    if (result.resultCode == RESULT_OK) {
        // Pengguna berhasil menyalakan Bluetooth.
        // Langkah selanjutnya adalah mulai discovery.
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

    private fun checkAndRequestPermissions() {
        //check gps hidup atau tidak
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        //jika gps mati maka tidak lanjut ke tahap berikutnya
        if (!isGpsEnabled) {
            return
        }

        val permissionsToRequest = mutableListOf<String>()

        // perizinan untuk Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        // perizinan Android 11 dan di bawahnya
        else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_BLUETOOTH)
        } else {
            proceedWithBluetoothProcess()
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Jika izin diberikan, panggil fungsi "gerbang utama" kita.
                proceedWithBluetoothProcess() // <-- UBAH INI
            } else {
                Toast.makeText(this, "Izin diperlukan untuk mencari perangkat bluetooth.", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Ganti nama dari 'enableBluetooth' menjadi lebih deskriptif
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    private fun proceedWithBluetoothProcess() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            startDiscovery()
        }
    }

//    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    private fun startDiscovery(){
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }

            // Pastikan RecyclerView terlihat sebelum memulai scan
            binding.rvListDevices.visibility = View.VISIBLE

            lifecycleScope.launch {
                if (!bluetoothViewModel.isReceiverRegistered.first()) {
                    registerReceiver(receiver, filter)
                    launch {
                        bluetoothViewModel.changeIsReceiverRegistered(true)
                    }
                }
            }
            listBluetoothDevice.clear()
            bluetoothDeviceAdapter.notifyDataSetChanged()
            bluetoothAdapter.startDiscovery()
        }

    private fun connectToDevice(address: String) {
        val startTime = System.nanoTime()

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
            }
        }
        lifecycleScope.launch {
            bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTING)
        }
        bluetoothViewModel.connectToDevice(
            address = address,
            onSuccess = {
                val duration = System.nanoTime() - startTime
                val durationInMillis = TimeUnit.NANOSECONDS.toMillis(duration)
                val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
                lifecycleScope.launch {
                    bluetoothViewModel.saveBluetoothAddress(address)
                    bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTED)
                }
                Toast.makeText(this, "Connected to ${device?.name} in ${durationInMillis}ms", Toast.LENGTH_SHORT).show()
                saveLogToFile(this, "Connect Bluetooth", "OK", "Connected to ${device?.name}. Duration: ${durationInMillis}ms")
                startAndBindOBDService()
            },
            onError = { error ->
                val duration = System.nanoTime() - startTime
                val durationInMillis = TimeUnit.NANOSECONDS.toMillis(duration)
                lifecycleScope.launch {
                    bluetoothViewModel.updateConnectionState(BluetoothConnectionState.IDLE)
                }
                saveLogToFile(this, "Connect Bluetooth", "ERROR", "Connection failed: $error. Duration: ${durationInMillis}ms")
                Log.e("Bluetooth", "Connection failed: $error")
            }
        )
    }


    private fun reconnectUntilSuccess(address: String): Job {
        Log.d("Bluetooth", "reconnect")

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
            }
//            return null
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
                    lifecycleScope.launch {
                        launch {
                            bluetoothViewModel.updateConnectionState(BluetoothConnectionState.CONNECTED)
                        }
                        launch {
                            bluetoothViewModel.reconnectingJob.value?.cancel()
                            bluetoothViewModel.updateReconnectingJob(null)
                        }
                    }
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
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    // HAPUS SEMUA BLOK 'if (checkSelfPermission...)' DARI SINI

                    device?.let {
                        // Cek agar tidak menambahkan duplikat
                        if (listBluetoothDevice.none { item -> item.address == it.address }) {
                            val name = it.name ?: "Unknown Device" // it.name butuh BLUETOOTH_CONNECT
                            val address = it.address
                            val deviceItem = BluetoothDeviceItem(name = name, address = address)

                            Log.d("BluetoothScan", "Device found: $name - $address")

                            listBluetoothDevice.add(deviceItem)
                            bluetoothDeviceAdapter.notifyItemInserted(listBluetoothDevice.size - 1)
                        }
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
//        lifecycleScope.launch {
//            bluetoothViewModel.updateServiceState(ServiceState.RUNNING)
//        }
    }

    private fun stopAndUnbindOBDService() {
        stopService(INTENT_SERVICE_STATE) // This will eventually call onDestroy in the service
        Log.d("MainActivity", "OBD Service stopped and unbound")
    }

    private fun disconnectOrClose(){
//        stopAndUnbindOBDService()
//        obdViewModel.stopReading()
        if (bluetoothViewModel.serviceState.value == ServiceState.RUNNING) {
//            stopService(INTENT_SERVICE_STATE)
            stopAndUnbindOBDService()
            Log.d("service", "service stop called.")
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