package com.example.obd_iiservice.setting.ui.threshold

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.ActivityThresholdBinding
import com.example.obd_iiservice.helper.makeToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ThresholdActivity : AppCompatActivity() {

    private val thresholdViewModel : ThresholdViewModel by viewModels()

    private lateinit var binding: ActivityThresholdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityThresholdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.threshold_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initUI(){
        val listData = listOf<String>("RPM", "Speed", "Throttle", "Temp", "Maf")

        lifecycleScope.launch {
            thresholdViewModel.thresholdData.collect { data ->
                val keyMap = mapOf(
                    "RPM" to data.rpm,
                    "Speed" to data.speed,
                    "Throttle" to data.throttle,
                    "Temp" to data.temp,
                    "Maf" to data.maf.toInt()
                )

                thresholdViewModel.thresholdKey.putAll(keyMap)

                binding.apply {
                    setupSeekBar(seekBarRpm, textRpm, "RPM", keyMap["RPM"] ?: 0)
                    setupSeekBar(seekBarSpeed, textSpeed, "Speed", keyMap["Speed"] ?: 0)
                    setupSeekBar(seekBarThrottle, textThrottle, "Throttle", keyMap["Throttle"] ?: 0)
                    setupSeekBar(seekBarTemp, textTemp, "Temp", keyMap["Temp"] ?: 0)
                    setupSeekBar(seekBarMaf, textMaf, "Maf", keyMap["Maf"] ?: 0)
                }
            }
//            thresholdViewModel.apply {
//                this.thresholdData.collect { data ->
//                    for (list in listData){
//                        if (list == "RPM"){
//                            this.thresholdKey[list] = data.rpm
//                        } else if (list == "Speed") {
//                            this.thresholdKey[list] = data.speed
//                        } else if (list == "Throttle") {
//                            this.thresholdKey[list] = data.throttle
//                        } else if (list == "Temp"){
//                            this.thresholdKey[list] = data.temp
//                        } else if (list == "Maf"){
//                            this.thresholdKey[list] = data.maf.toInt()
//                        }
//                    }
//                }
//            }
        }

        binding.apply {
//            for (list in listData){
//                if (list == "RPM"){
//                    setupSeekBar(this.seekBarRpm, this.textRpm, list)
//                } else if (list == "Speed") {
//                    setupSeekBar(this.seekBarSpeed, this.textSpeed, list)
//                } else if (list == "Throttle") {
//                    setupSeekBar(this.seekBarThrottle, this.textThrottle, list)
//                } else if (list == "Temp"){
//                    setupSeekBar(this.seekBarTemp, this.textTemp, list)
//                } else if (list == "Maf"){
//                    setupSeekBar(this.seekBarMaf, this.textMaf, list)
//                } else {
//                    return
//                }
//            }

            this.btnSaveThresholds.setOnClickListener {
                // Lakukan sesuatu dengan threshold
                Log.d("Thresholds", thresholdViewModel.thresholdKey.toString())
                makeToast(this@ThresholdActivity, "Threshold saved")
//                val listData = listOf<String>("RPM", "Speed", "Throttle", "Temp", "Maf")
                lifecycleScope.launch {
                    thresholdViewModel.apply {
//                        this.thresholdData.collect { data ->
                        for (list in listData){
                            this.thresholdKey[list]?.let { data ->
                                if (list == "RPM"){
                                    saveRpmThreshold(data)
                                } else if (list == "Speed") {
                                    saveSpeedThreshold(data)
                                } else if (list == "Throttle") {
                                    saveThrottleThreshold(data)
                                } else if (list == "Temp"){
                                    saveTempThreshold(data)
                                } else if (list == "Maf"){
                                    saveMafThreshold(data.toDouble())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupSeekBar(seekBar: SeekBar, textView: TextView, key: String, initialValue: Int) {
        seekBar.progress = initialValue
        textView.text = initialValue.toString()
        thresholdViewModel.thresholdKey[key] = initialValue

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = progress.toString()
                thresholdViewModel.thresholdKey[key] = progress
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

}