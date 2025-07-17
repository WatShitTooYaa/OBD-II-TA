package com.example.obd_iiservice.home

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.obd_iiservice.R
import com.example.obd_iiservice.bluetooth.BluetoothRepository
import com.example.obd_iiservice.databinding.ActivityHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    @Inject lateinit var bluetoothRepository: BluetoothRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_home)
        navView.setupWithNavController(navController)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.bot_nav_home, R.id.bot_nav_dtc, R.id.bot_nav_log, R.id.bot_nav_setting
            )
        )

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
//                Toast.makeText(this@MainActivity, "menenak tombol kembali", Toast.LENGTH_LONG).show()
                AlertDialog.Builder(this@HomeActivity)
                    .setTitle("Keluar")
                    .setMessage("Apakah anda ingin keluar dari aplikasi?")
                    .setPositiveButton("Ya") { _,_ ->
//                        obdViewModel.stopReading()
//                        lifecycleScope.launch {
////                            if (bluetoothViewModel.isReceiverRegistered.first() == true){
////                                unregisterReceiver(receiver)
////                            }
//                            unRegReceiver()
//                        }
                        disconnect()
                        finish()
                    }
                    .setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
        })
//        setupActionBarWithNavController(navController, appBarConfiguration)

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_home)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
    }

    fun disconnect() {
        try {
//            bluetoothSocket?.close()
            bluetoothRepository.bluetoothSocket.value.takeIf { it?.isConnected == true }?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Disconnect failed", e)
        } finally {
//            bluetoothSocket = null
            lifecycleScope.launch {
                bluetoothRepository.updateBluetoothSocket(null)
            }
        }
    }

}