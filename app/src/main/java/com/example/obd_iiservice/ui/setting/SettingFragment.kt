package com.example.obd_iiservice.ui.setting

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.obd_iiservice.R
import com.example.obd_iiservice.setting.ui.bluetooth.BluetoothActivity
import com.example.obd_iiservice.databinding.FragmentSettingBinding
import com.example.obd_iiservice.setting.ui.mqtt.MqttActivity
import com.example.obd_iiservice.setting.ui.threshold.ThresholdActivity
import com.example.obd_iiservice.ui.decoration.RecyclerViewItemDecoration

class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private lateinit var rvSetting : RecyclerView
    private lateinit var settingAdapter: SettingAdapter
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = SettingFragment()
    }

    private val viewModel: SettingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //viewmodel using
//        viewModel.listSetting
//        settingAdapter

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root

    }

    private fun setupRecyclerView(){
        rvSetting = binding.rvSetting
        rvSetting.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        rvSetting.layoutManager = layoutManager
        val listSetting = listOf(
            SettingItem(
                R.drawable.bluetooth_circle,
                "Bluetooth",
                "Connection",
                BluetoothActivity::class.java
            ),
            SettingItem(
                R.drawable.threshold_icon,
                "Threshold",
                "Limit data",
                ThresholdActivity::class.java
            ),
            SettingItem(
                R.drawable.port,
                "MQTT",
                "Setting connection MQTT",
                MqttActivity::class.java
            ),
        )
        settingAdapter = SettingAdapter(listSetting) { clickedSetting ->
            clickedSetting.targetActivity.let { activityClass ->
                val intent = Intent(activity, activityClass)
                startActivity(intent)
            }
        }

        rvSetting.adapter = settingAdapter
        rvSetting.addItemDecoration(
//            DividerItemDecoration(
//                activity,
//                layoutManager.orientation
////            )
//            RecyclerViewItemDecoration(
//                requireActivity(),
//                R.drawable.rv_divider
//            )
            activity?.let {
                RecyclerViewItemDecoration(
                    context = it,
                    R.drawable.rv_divider
                )
            } ?: DividerItemDecoration(
                activity,
                layoutManager.orientation
            )
        )
    }
}