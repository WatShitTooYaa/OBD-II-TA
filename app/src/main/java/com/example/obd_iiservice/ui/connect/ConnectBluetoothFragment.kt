package com.example.obd_iiservice.ui.connect

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.MainViewModel
import com.example.obd_iiservice.R
import com.example.obd_iiservice.bluetooth.BluetoothDeviceAdapter
import com.example.obd_iiservice.bluetooth.BluetoothDeviceItem
import com.example.obd_iiservice.bluetooth.BluetoothViewModel
import com.example.obd_iiservice.databinding.FragmentConnectBluetoothBinding
import com.example.obd_iiservice.databinding.FragmentHomeBinding
import com.example.obd_iiservice.helper.saveLogToFile
import com.example.obd_iiservice.obd.OBDForegroundService
import com.example.obd_iiservice.obd.OBDItem
import com.example.obd_iiservice.obd.OBDViewModel
import com.example.obd_iiservice.setting.SettingViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ConnectBluetoothFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ConnectBluetoothFragment : Fragment() {
    private var _binding : FragmentConnectBluetoothBinding? = null
    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
    private lateinit var rvBluetooth: RecyclerView
    private val listBluetoothDevice = mutableListOf<BluetoothDeviceItem>()
//    private var listOBD = mutableListOf<OBDItem>()
    private val bluetoothViewModel : BluetoothViewModel by viewModels()
    private val obdViewModel : OBDViewModel by viewModels()
    private val settingViewModel: SettingViewModel by viewModels()
    private val mainViewModel : MainViewModel by viewModels()

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


    private val REQUEST_PERMISSION = 2
    private val PERMISSION_REQUEST_BLUETOOTH = 1
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_connect_bluetooth, container, false)
        _binding = FragmentConnectBluetoothBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            startDiscovery()
        } else {
//            Toast.makeText(this, "Bluetooth tidak diaktifkan", Toast.LENGTH_SHORT).show()
            Toast.makeText(activity, "Bluetooth tidak diaktifkan", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = mutableListOf<String>()
//            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
//            }
            activity?.let {
                if (checkSelfPermission(it, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                }
                if (checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
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

    @Deprecated("Deprecated in Java")
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
                Toast.makeText(activity, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
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

    @SuppressLint("NotifyDataSetChanged")
    private fun startDiscovery(){
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        lifecycleScope.launch {
            if (!bluetoothViewModel.isReceiverRegistered.first()) {
                activity?.let { registerReceiver(it, receiver, filter, RECEIVER_EXPORTED) }
                bluetoothViewModel.changeIsReceiverRegistered(true)
            }
        }

        activity?.let {
            if(ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION)
                return
            }
        }
//        deviceList.clear()
        listBluetoothDevice.clear()
//        deviceListAdapter.notifyDataSetChanged()
        bluetoothDeviceAdapter.notifyDataSetChanged()
        bluetoothAdapter.startDiscovery()
        Toast.makeText(activity, "Scanning bluetooth devices....", Toast.LENGTH_LONG).show()

    }

    private fun connectToDevice(address: String) {
        // UUID default untuk SPP (Serial Port Profile)
        activity?.let {
            if(ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
                if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
                }
                return
            }
        }
        bluetoothViewModel.connectToDevice(
            address = address,
            onSuccess = {
                val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
//                binding.tvStatusBluetooth.text = device?.name ?: "Unknown"
                lifecycleScope.launch {
                    settingViewModel.saveBluetoothAddress(address)
                    obdViewModel.updateBluetoothConnection(true)
                }
                Toast.makeText(activity, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
                saveLogToFile(requireContext(), "Connect Bluetooth", "OK", "Connected to ${device?.name}")
                activity?.let { saveLogToFile(it, "Connect Bluetooth", "OK", "Connected to ${device?.name}") }
//                testObdConnection()
                startAndBindOBDService()
            },
            onError = { error ->
                Toast.makeText(activity, "Connection failed: $error", Toast.LENGTH_LONG).show()
                activity?.let { saveLogToFile(it, "Connect Bluetooth", "ERROR", "Connection failed: $error") }
                Log.e("Bluetooth", "Connection failed: $error")
            }
        )
    }


    private fun reconnectUntilSuccess(address: String): Job? {
        activity?.let {
            if (ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
                }
                return null
            }
        }

        return lifecycleScope.launch {
            var attempt = 0
//            val maxAttempts = 5
            while (!bluetoothViewModel.isConnected.value) {
                Log.d("Bluetooth", "Attempt reconnect: $attempt")

                val success = bluetoothViewModel.connectToDeviceSuspend(address)

                if (success) {
                    val device = bluetoothViewModel.bluetoothSocket.value?.remoteDevice
//                    binding.tvStatusBluetooth.text = device?.name ?: "Unknown"
                    Toast.makeText(activity, "Connected to ${device?.name}", Toast.LENGTH_SHORT).show()
                    activity?.let {
                        saveLogToFile(
                            it,
                            "Connect Bluetooth",
                            "OK",
                            "Reconnected to ${device?.name}"
                        )
                    }
//                    testObdConnection()
                    startAndBindOBDService()
                    obdViewModel.updateBluetoothConnection(true)
                    break // berhenti mencoba jika sudah terkoneksi
                } else {
                    Log.e("Bluetooth", "Reconnect failed")
                    activity?.let {
                        saveLogToFile(
                            it,
                            "Reconnect Bluetooth",
                            "ERROR",
                            "Reconnect ke-${attempt} gagal"
                        )
                    }
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

                    activity?.let {
                        if (ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                it,
                                arrayOf(Manifest.permission.BLUETOOTH),
                                REQUEST_PERMISSION
                            )
                            return
                        }
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
        activity?.let {
            val serviceIntent = Intent(it, OBDForegroundService::class.java)
            // Start the service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.startForegroundService(serviceIntent)
            } else {
                it.startService(serviceIntent)
            }
        }
    }

    private fun stopAndUnbindOBDService() {
        activity?.let {
            val serviceIntent = Intent(it, OBDForegroundService::class.java)
            it.stopService(serviceIntent) // This will eventually call onDestroy in the service
            Log.d("MainActivity", "OBD Service stopped and unbound")
        }
    }

    private fun disconnectOrClose(){
        stopAndUnbindOBDService()
        obdViewModel.stopReading()
        if (obdViewModel.serviceIntent.value != null && activity != null) {
            activity?.stopService(obdViewModel.serviceIntent.value)
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

    suspend fun unRegReceiver(){
        if (bluetoothViewModel.isReceiverRegistered.first() == true){
            activity?.unregisterReceiver(receiver)
            bluetoothViewModel.changeIsReceiverRegistered(false)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ConnectBluetoothFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ConnectBluetoothFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}