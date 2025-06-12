package com.example.obd_iiservice.ui.home

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.MainViewModel
import com.example.obd_iiservice.bluetooth.BluetoothDeviceItem
import com.example.obd_iiservice.setting.ui.bluetooth.BluetoothViewModel
import com.example.obd_iiservice.databinding.FragmentHomeBinding
import com.example.obd_iiservice.obd.OBDAdapter
import com.example.obd_iiservice.obd.OBDItem
import com.example.obd_iiservice.obd.OBDViewModel
import com.example.obd_iiservice.setting.SettingViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
//    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
//    private lateinit var rvBluetooth: RecyclerView
    private lateinit var rvOBD: RecyclerView
    private val listBluetoothDevice = mutableListOf<BluetoothDeviceItem>()
    private var listOBD = mutableListOf<OBDItem>()
    private val bluetoothViewModel : BluetoothViewModel by viewModels()
    private val obdViewModel : OBDViewModel by viewModels()
    private val settingViewModel: SettingViewModel by viewModels()
    private val mainViewModel : MainViewModel by viewModels()

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var obdAdapter: OBDAdapter


    private val REQUEST_PERMISSION = 2
    private val PERMISSION_REQUEST_BLUETOOTH = 1
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        initUI()
//        showRecycleView()
//        val textView: TextView = binding.
//
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gunakan ViewTreeObserver untuk menunggu layout selesai digambar
        binding.rvObd.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Hapus listener agar tidak berjalan berulang kali
                binding.rvObd.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Ambil tinggi RecyclerView yang tersedia
                val recyclerViewHeight = binding.rvObd.height

                // Pastikan tingginya valid sebelum melakukan kalkulasi
                if (recyclerViewHeight > 0) {
                    // Tentukan jumlah baris yang Anda inginkan
                    val numberOfRows = 3

                    // Hitung tinggi yang seharusnya untuk setiap item
                    // Kurangi sedikit untuk margin jika perlu
                    val verticalMargin = (binding.rvObd.layoutParams as ViewGroup.MarginLayoutParams).topMargin * 2
                    val targetItemHeight = (recyclerViewHeight - verticalMargin) / numberOfRows

                    // Sekarang, inisialisasi dan atur adapter dengan tinggi yang sudah dihitung
                    setupRecyclerView(targetItemHeight)
                    loadDashboardData()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



//    private val enableBluetoothLauncher = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { res ->
//        if (res.resultCode == RESULT_OK) {
//            startDiscovery()
//        } else {
////            Toast.makeText(this, "Bluetooth tidak diaktifkan", Toast.LENGTH_SHORT).show()
//            Toast.makeText(activity, "Bluetooth tidak diaktifkan", Toast.LENGTH_LONG).show()
//        }
//    }

    private fun setupRecyclerView(itemHeight: Int) {
        val spanCount = 2 // Jumlah kolom grid Anda
        val spacingInDp = 2 // Jarak yang Anda inginkan dalam dp

        // Konversi dp ke piksel menggunakan fungsi bantuan
        val spacingInPixels = spacingInDp.dpToPx(requireContext())

        rvOBD = binding.rvObd
        rvOBD.setHasFixedSize(true)
        // Inisialisasi adapter dengan tinggi yang sudah dihitung
//        homeAdapter = HomeAdapter(itemHeight)
        rvOBD.layoutManager = GridLayoutManager(activity, 2)
        obdAdapter = OBDAdapter(
            listOBD,
            itemHeight
        )

//        val layoutManager = GridLayoutManager(requireContext(), 2)
        // ... Logika SpanSizeLookup jika ada ...

//        binding.rvObd.layoutManager = layoutManager
        rvOBD.adapter = obdAdapter
//        binding.rvObd.adapter = homeAdapter
        // Hapus dekorasi lama jika ada untuk menghindari duplikasi
        if (binding.rvObd.itemDecorationCount > 0) {
            binding.rvObd.removeItemDecorationAt(0)
        }

        // --- INI BAGIAN PENTINGNYA ---
        // Tambahkan ItemDecoration yang baru kita buat
        binding.rvObd.addItemDecoration(GridSpacingItemDecoration(spanCount, spacingInPixels, false))
        // `includeEdge: false` berarti tidak ada margin di tepi luar grid.
        // Ganti ke `true` jika Anda menginginkan margin di tepi luar juga.
    }

    private fun loadDashboardData() {
        val newItems = listOf(
            OBDItem("Throttle","0", "%"),
            OBDItem("Speed","0", "km/h"),
            OBDItem("Temperature","0", "°C"),
            OBDItem("RPM","0", "rpm"),
            OBDItem("MAF","0", "g/s"),
            OBDItem("Fuel Consumption","0", "Km/L"),
        )
        // Panggil fungsi updateData di adapter
        obdAdapter.updateData(newItems)
    }

//    private fun showRecycleView() {
////        rvBluetooth = binding.rvListDevices
////        rvBluetooth.setHasFixedSize(true)
//        rvOBD = binding.rvObd
//        rvOBD.setHasFixedSize(true)
////
////        //rv bluetooth
////        rvBluetooth.layoutManager = LinearLayoutManager(activity)
//////        val listBluetoothAdapter = BluetoothDeviceAdapter(listDevice)
////        bluetoothDeviceAdapter = BluetoothDeviceAdapter(
////            listBluetoothDevice,
////            object : BluetoothDeviceAdapter.OnDeviceConnectListener {
//////                override fun onConnect(address: String) {
//////                    connectToDevice(address)
//////                }
////
////                override fun onSaveBluetoothDevice(address: String) {
//////                    bluetoothViewModel
////                    lifecycleScope.launch {
////                        settingViewModel.saveBluetoothAddress(address)
////                    }
////                }
////            }
////        )
////        rvBluetooth.adapter = bluetoothDeviceAdapter
//
//        val items = listOf(
//            OBDItem("Throttle","0", "%"),
//            OBDItem("Speed","0", "km/h"),
//            OBDItem("Temperature","0", "°C"),
//            OBDItem("RPM","0", "rpm"),
//            OBDItem("MAF","0", "g/s"),
//        )
//        listOBD = items as MutableList<OBDItem>
//        //rv obd
////        rvOBD.layoutManager = LinearLayoutManager(activity
//        rvOBD.layoutManager = GridLayoutManager(activity, 2)
////        obdAdapter = OBDAdapter(
////            listOBD,
////
////        )
//        rvOBD.adapter = obdAdapter
//    }
//
//    private fun checkAndRequestPermissions() {
//        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            val permissions = mutableListOf<String>()
////            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
////                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
////            }
//            activity?.let {
//                if (checkSelfPermission(it, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                    permissions.add(Manifest.permission.BLUETOOTH_SCAN)
//                }
//                if (checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
//                }
//            }
//
//            if (permissions.isNotEmpty()) {
//                requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_BLUETOOTH)
//            } else {
////                startDiscovery() // Aman langsung scanning
//                enableBluetooth()
//            }
//        } else {
////            startDiscovery(
//            enableBluetooth()
//        }
//    }
//
//    @Deprecated("Deprecated in Java")
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
//            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                startDiscovery()
////                enableBluetooth()
//            } else {
//                Toast.makeText(activity, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun enableBluetooth() {
//        if (!bluetoothAdapter.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            enableBluetoothLauncher.launch(enableBtIntent)
//        } else {
//            startDiscovery()
//        }
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    private fun startDiscovery(){
//        val filter = IntentFilter().apply {
//            addAction(BluetoothDevice.ACTION_FOUND)
//            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
//            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
//            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//        }
//
//        lifecycleScope.launch {
//            if (bluetoothViewModel.isReceiverRegistered.first() == false) {
//                activity?.let { registerReceiver(it, receiver, filter, RECEIVER_EXPORTED) }
//                bluetoothViewModel.changeIsReceiverRegistered(true)
//            }
//        }
//
//        activity?.let {
//            if(ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
//                ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION)
//                return
//            }
//        }
////        deviceList.clear()
//        listBluetoothDevice.clear()
////        deviceListAdapter.notifyDataSetChanged()
//        bluetoothDeviceAdapter.notifyDataSetChanged()
//        bluetoothAdapter.startDiscovery()
//        Toast.makeText(activity, "Scanning bluetooth devices....", Toast.LENGTH_LONG).show()
//
//    }
//
//    private fun connectToDevice(address: String) {
//        // UUID default untuk SPP (Serial Port Profile)
//        activity?.let {
//            if(ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
//                if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
//                }
//                return
//            }
//        }
//        bluetoothViewModel.connectToDevice(
//            address = address,
//            onSuccess = {
//                val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
////                binding.tvStatusBluetooth.text = device?.name ?: "Unknown"
//                lifecycleScope.launch {
//                    settingViewModel.saveBluetoothAddress(address)
//                    obdViewModel.updateBluetoothConnection(true)
//                }
//                Toast.makeText(activity, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
//                saveLogToFile(requireContext(), "Connect Bluetooth", "OK", "Connected to ${device?.name}")
//                activity?.let { saveLogToFile(it, "Connect Bluetooth", "OK", "Connected to ${device?.name}") }
////                testObdConnection()
//                startAndBindOBDService()
//            },
//            onError = { error ->
//                Toast.makeText(activity, "Connection failed: $error", Toast.LENGTH_LONG).show()
//                activity?.let { saveLogToFile(it, "Connect Bluetooth", "ERROR", "Connection failed: $error") }
//                Log.e("Bluetooth", "Connection failed: $error")
//            }
//        )
//    }
//
//
//    private fun reconnectUntilSuccess(address: String): Job? {
//        activity?.let {
//            if (ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
//                }
//                return null
//            }
//        }
//
//        return lifecycleScope.launch {
//            var attempt = 0
////            val maxAttempts = 5
//            while (bluetoothViewModel.isConnected.value == false) {
//                Log.d("Bluetooth", "Attempt reconnect: $attempt")
//
//                val success = bluetoothViewModel.connectToDeviceSuspend(address)
//
//                if (success) {
//                    val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
////                    binding.tvStatusBluetooth.text = device?.name ?: "Unknown"
//                    Toast.makeText(activity, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
//                    activity?.let {
//                        saveLogToFile(
//                            it,
//                            "Connect Bluetooth",
//                            "OK",
//                            "Reconnected to ${device?.name}"
//                        )
//                    }
////                    testObdConnection()
//                    startAndBindOBDService()
//                    obdViewModel.updateBluetoothConnection(true)
//                    break // berhenti mencoba jika sudah terkoneksi
//                } else {
//                    Log.e("Bluetooth", "Reconnect failed")
//                    activity?.let {
//                        saveLogToFile(
//                            it,
//                            "Reconnect Bluetooth",
//                            "ERROR",
//                            "Reconnect ke-${attempt} gagal"
//                        )
//                    }
//                }
//
//                attempt++
//                delay(3000)
//            }
//        }
//    }
//
//
//    private val receiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            when (intent?.action) {
//                BluetoothDevice.ACTION_FOUND -> {
//                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
//                    } else {
//                        @Suppress("DEPRECATION")
//                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
//                    }
//
//                    activity?.let {
//                        if (ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH)
//                            != PackageManager.PERMISSION_GRANTED
//                        ) {
//                            ActivityCompat.requestPermissions(
//                                it,
//                                arrayOf(Manifest.permission.BLUETOOTH),
//                                REQUEST_PERMISSION
//                            )
//                            return
//                        }
//                    }
//
//                    device?.let {
//                        val name = it.name ?: "Unknown Device"
//                        val address = it.address
//                        val deviceItem = BluetoothDeviceItem(name = name, address = address)
//                        Log.d("BluetoothScan", "Device found: $name - $address")
//                        listBluetoothDevice.add(deviceItem)
//                        bluetoothDeviceAdapter.notifyItemInserted(listBluetoothDevice.size - 1)
//                    }
//                }
//
//                BluetoothAdapter.ACTION_STATE_CHANGED -> {
//                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
//                    when (state) {
//                        BluetoothAdapter.STATE_OFF -> {
//                            Log.d("BluetoothState", "Bluetooth is OFF")
//                            disconnectOrClose()
//                            // Misalnya update UI atau beri notifikasi ke user
//                        }
//                        BluetoothAdapter.STATE_ON -> {
//                            Log.d("BluetoothState", "Bluetooth is ON")
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun startAndBindOBDService() {
//        activity?.let {
//            val serviceIntent = Intent(it, OBDForegroundService::class.java)
//            // Start the service
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                it.startForegroundService(serviceIntent)
//            } else {
//                it.startService(serviceIntent)
//            }
//        }
//    }
//
//    private fun stopAndUnbindOBDService() {
//        activity?.let {
//            val serviceIntent = Intent(it, OBDForegroundService::class.java)
//            it.stopService(serviceIntent) // This will eventually call onDestroy in the service
//            Log.d("MainActivity", "OBD Service stopped and unbound")
//        }
//    }
//
//    private fun disconnectOrClose(){
//        stopAndUnbindOBDService()
//        obdViewModel.stopReading()
//        if (obdViewModel.serviceIntent.value != null && activity != null) {
//            activity?.stopService(obdViewModel.serviceIntent.value)
//        }
//        lifecycleScope.launch {
//            obdViewModel.updateBluetoothConnection(false)
//            mainViewModel.updateCurrentStreamId(null)
//            mainViewModel.updateIsPlaying(false)
//        }
//        bluetoothViewModel.disconnect()
//        listBluetoothDevice.clear()
//    }
//
//    override fun onStop() {
//        super.onStop()
//
//        lifecycleScope.launch {
////            unRegReceiver()
//            bluetoothViewModel.updateReconnectingJob(null)
//        }
//    }
//
//    suspend fun unRegReceiver(){
//        if (bluetoothViewModel.isReceiverRegistered.first() == true){
//            activity?.unregisterReceiver(receiver)
//            bluetoothViewModel.changeIsReceiverRegistered(false)
//        }
//    }



//    private fun initUI() {
//        rvBluetooth = binding.rvListDevices
//        rvBluetooth.setHasFixedSize(true)
//        rvOBD = binding.rvObd
//        rvOBD.setHasFixedSize(true)
//        showRecycleView()
//
//        binding.btnScanConnectBluetooth.apply {
//            when (bluetoothAdapter.isEnabled) {
//                true -> {
//                    text = "Scan Bluetooth"
//                    setOnClickListener {
//
//                    }
//                }
//
//                false -> {
//                    text = "Enable Bluetooth"
//                }
//            }
//        }
////        binding.btnScanIfAutoReconFalse.setOnClickListener {
////            checkAndRequestPermissions()
////        }
////
////        binding.btnScanIfAutoReconTrue.setOnClickListener {
////            checkAndRequestPermissions()
////        }
////
////        binding.btnConnectIfAutoReconFalse.setOnClickListener {
////            lifecycleScope.launch {
////                settingViewModel.isInitialized
////                    .filter { it } // hanya lanjut kalau true
////                    .first()
////                val canConnect = settingViewModel.checkDataForConnecting()
////                val address = settingViewModel.bluetoothAddress.first()
//////                makeToast(this@MainActivity, "top : $topic, p: $port, ho: $host, adress : $address")
////                if (!bluetoothAdapter.isEnabled) {
////                    val enableBtnIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
////                    enableBluetoothLauncher.launch(enableBtnIntent)
////                } else {
////                    if (canConnect && address != null){
////                        connectToDevice(address)
////                    } else {
////                        makeToast(this@MainActivity, "topic atau port tidak boleh kosong")
////                    }
////                }
////            }
////        }
////
////        binding.btnDisconnectIfAutoReconFalse.setOnClickListener {
////            disconnectOrClose()
////        }
//
////        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
////            when(menuItem.itemId) {
////                R.id.nav_settings -> {
////                    val intent = Intent(this, SettingActivity::class.java)
////                    startActivity(intent)
////                    true
////                }
////                R.id.nav_log -> {
////                    val intent = Intent(this, LogViewActivity::class.java)
////                    startActivity(intent)
////                    true
////                }
////                R.id.nav_dtc -> {
////                    val intent = Intent(this, DTCActivity::class.java)
////                    startActivity(intent)
////                    true
////                }
////                R.id.nav_treshold -> {
////                    val intent = Intent(this, ThresholdActivity::class.java)
////                    startActivity(intent)
////                    true
////                }
////                R.id.nav_quit -> {
////                    disconnectOrClose()
////                    finish()
////                    true
////                }
////                else -> false
////            }
////        }
//
////        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
////            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
////            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
////            insets
////        }
//
//    }

//    private fun observerViewModel(){
//        lifecycleScope.launch {
//            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//
//                launch {
//                    settingViewModel.mqttAutoRecon.collect { isAuto ->
//                        if (isAuto){
//                            binding.clContainerButtonIfAutoReconTrue.visibility = View.VISIBLE
//                            binding.clContainerButtonIfAutoReconFalse.visibility = View.GONE
//                        } else {
//                            binding.clContainerButtonIfAutoReconTrue.visibility = View.GONE
//                            binding.clContainerButtonIfAutoReconFalse.visibility = View.VISIBLE
//                        }
//                    }
//                }
//
//                launch {
//                    bluetoothViewModel.bluetoothSocket.collect {
//                        if (it != null && it.isConnected == false){
//                            disconnectOrClose()
//                        }
//                    }
//                }
//
//                launch {
//                    repeatOnLifecycle(Lifecycle.State.STARTED) {
//                        obdViewModel.obdData.collect { data ->
//                            binding.tvDataRpm.text = data["Rpm"]?.toIntOrNull()?.toString() ?: "-"
//                            binding.tvDataSpeed.text = data["Speed"]?.toIntOrNull()?.toString() ?: "-"
//                            binding.tvDataThrottle.text = data["Throttle"]?.toIntOrNull()?.toString() ?: "-"
//                            binding.tvDataTemp.text = data["Temp"]?.toIntOrNull()?.toString() ?: "-"
//                            binding.tvDataMaf.text = data["Maf"]?.toDoubleOrNull()?.toString() ?: "-"
//                        }
//                    }
//                }
//
//                launch {
//                    combine(
//                        settingViewModel.bluetoothAddress,
//                        bluetoothViewModel.isConnected,
//                        settingViewModel.mqttAutoRecon,
//                        bluetoothViewModel.reconnectingJob
//                    ) { address, isConnected, isAuto, reconnectingJob ->
//                        // Combine jadi Quadruple
//                        ObserveConnectionBluetooth(address, isConnected, isAuto, reconnectingJob)
//                    }.collect { (address, isConnected, isAuto, reconnectingJob) ->
//                        if (address != null && !isConnected && isAuto) {
//                            if (reconnectingJob?.isActive != true && settingViewModel.checkDataForConnecting()) {
//                                makeToast(this@MainActivity, "recon")
//                                bluetoothViewModel.updateReconnectingJob(reconnectUntilSuccess(address))
//                            } else if (settingViewModel.checkDataForConnecting() == false){
//                                makeToast(this@MainActivity, "topic atau port tidak boleh kosong")
//                                saveLogToFile(
//                                    this@MainActivity,
//                                    "reconnect",
//                                    "ERROR",
//                                    "Topic atau port tidak boleh kosong"
//                                )
//                                delay(1000)
//                            } else {
//                                makeToast(this@MainActivity, "error saat akan reconnecting otomatis")
//                                saveLogToFile(
//                                    this@MainActivity,
//                                    "reconnect",
//                                    "ERROR",
//                                    "error saat akan reconnecting otomatis")
//                            }
//                        } else {
//                            bluetoothViewModel.updateReconnectingJob(null)
//                        }
//
//                        // Pengecekan untuk disconnect jika address == null atau isAuto berubah
//                        val addressChangedToNull = bluetoothViewModel.previousAddress.first() != address && address == null
//                        val isAutoChanged = bluetoothViewModel.previousIsAuto.first() != isAuto
//
//                        if ((addressChangedToNull || isAutoChanged) && (address == null || !isAuto)) {
//                            makeToast(this@MainActivity, "disconnect when address == null or isAuto == false")
//                            disconnectOrClose()
//                        }
//
//                        // Simpan nilai saat ini sebagai nilai sebelumnya
////                        previousAddress = address
////                        previousIsAuto = isAuto
//                        address?.let { bluetoothViewModel.updatePreviousAddress(it) }
//                        isAuto.let { bluetoothViewModel.updatePreviousIsAuto(it) }
//
//                        address?.let { binding.tvAddressBluetooth.text = it } ?: "-"
//                        // Update UI
//                        //jika address tidak ada dan terputus (kondisi awal)
//                        if (address == null && !isConnected){
//                            makeToast(this@MainActivity, "address tidak ada dan terputus")
//                            binding.rvListDevices.visibility = View.VISIBLE
//                            binding.llDashboard.visibility = View.GONE
//                            binding.btnScanIfAutoReconTrue.visibility = View.VISIBLE
//                            binding.btnScanIfAutoReconFalse.visibility = View.VISIBLE
//                            binding.rvListDevices.visibility = View.VISIBLE
//                            binding.btnConnectIfAutoReconFalse.visibility = View.GONE
//                        }
//                        //jika address ada dan terhubung
//                        else if (address != null && isConnected) {
//                            makeToast(this@MainActivity, "address ada dan terhubung")
//                            if (isAuto){
////                                binding.btnScanIfAutoReconFalse.visibility = View.GONE
////                                binding.btnDisconnectIfAutoReconFalse.visibility = View.VISIBLE
//                                binding.btnConnectIfAutoReconTrue.visibility = View.VISIBLE
//                                binding.btnConnectIfAutoReconTrue.text = "Connected"
//                            } else {
//                                binding.btnDisconnectIfAutoReconFalse.visibility = View.VISIBLE
//
//                            }
//                            binding.btnScanIfAutoReconFalse.visibility = View.GONE
//                            binding.btnScanIfAutoReconTrue.visibility = View.GONE
//                            binding.rvListDevices.visibility = View.GONE
//                            binding.btnConnectIfAutoReconFalse.visibility = View.GONE
//                            binding.rvListDevices.visibility = View.GONE
//                            binding.llDashboard.visibility = View.VISIBLE
//                        }
//                        //jika address ada namun terputus
//                        else if (address != null && !isConnected){
//                            makeToast(this@MainActivity, "address ada namun terputus")
//
//                            if (!isAuto){
//                                binding.btnConnectIfAutoReconFalse.visibility = View.VISIBLE
//                                binding.btnDisconnectIfAutoReconFalse.visibility = View.GONE
//                            } else {
//                                binding.btnScanIfAutoReconTrue.visibility = View.GONE
//                                binding.btnConnectIfAutoReconTrue.visibility = View.VISIBLE
//                                binding.btnConnectIfAutoReconTrue.isEnabled = false
//                                binding.btnConnectIfAutoReconTrue.text = "Reconnecting..."
//                            }
//                            binding.rvListDevices.visibility = View.GONE
//                            binding.llDashboard.visibility = View.GONE
////                            binding.rvListDevices.visibility = View.VISIBLE
////                            binding.llDashboard.visibility = View.GONE
//                        }
//                    }
//                }
//            }
//        }
//    }
}