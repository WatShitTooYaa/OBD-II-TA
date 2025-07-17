package com.example.obd_iiservice.setting.ui.threshold

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        val listData = listOf<String>("RPM", "Speed", "Throttle", "Temp", "Maf", "Fuel")

        lifecycleScope.launch {
            thresholdViewModel.thresholdData.collect { data ->
                val keyMap = mapOf(
                    "RPM" to data.rpm,
                    "Speed" to data.speed,
                    "Throttle" to data.throttle,
                    "Temp" to data.temp,
                    "Maf" to data.maf.toInt(),
                    "Fuel" to data.fuel,
                )

                thresholdViewModel.thresholdKey.putAll(keyMap)

                binding.apply {
                    setupSeekBar(seekBarRpm, textRpm, "RPM", keyMap["RPM"] ?: 0)
                    setupSeekBar(seekBarSpeed, textSpeed, "Speed", keyMap["Speed"] ?: 0)
                    setupSeekBar(seekBarThrottle, textThrottle, "Throttle", keyMap["Throttle"] ?: 0)
                    setupSeekBar(seekBarTemp, textTemp, "Temp", keyMap["Temp"] ?: 0)
                    setupSeekBar(seekBarMaf, textMaf, "Maf", keyMap["Maf"] ?: 0)
                    setupSeekBar(seekBarFuel, textFuel, "Fuel", keyMap["Fuel"] ?: 0)
                }
            }
        }

        binding.apply {
            this.btnSaveThresholds.setOnClickListener {
                Log.d("Thresholds", thresholdViewModel.thresholdKey.toString())
                makeToast(this@ThresholdActivity, "Threshold saved")
                lifecycleScope.launch {
                    thresholdViewModel.apply {
                        for (list in listData){
                            this.thresholdKey[list]?.let { data ->
                                when(list){
                                    "RPM" -> saveRpmThreshold(data)
                                    "Speed" -> saveSpeedThreshold(data)
                                    "Throttle" -> saveThrottleThreshold(data)
                                    "Temp" -> saveTempThreshold(data)
                                    "Maf" -> saveMafThreshold(data.toDouble())
                                    "Fuel" -> saveFuelThreshold(data)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupSeekBar(seekBar: SeekBar, textView: TextView, key: String, initialValue: Int) {
        seekBar.thumb = ContextCompat.getDrawable(this, R.drawable.scrubber_control)
        seekBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.scrubber_progress)
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