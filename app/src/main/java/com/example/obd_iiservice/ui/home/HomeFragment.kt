package com.example.obd_iiservice.ui.home

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
//    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
//    private lateinit var rvBluetooth: RecyclerView
    private lateinit var rvOBD: RecyclerView
    private val listBluetoothDevice = mutableListOf<BluetoothDeviceItem>()
    private var listOBD = mutableListOf<OBDItem>()
    private val homeViewModel : HomeViewModel by viewModels()

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var obdAdapter: ListAdapter<OBDItem, OBDAdapter.ListViewHolder>


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
//                    loadDashboardData()
                    observeDataOBD()
                }
            }
        })
    }

    //inisiasi recycler view
    private fun setupRecyclerView(itemHeight: Int) {
        val spanCount = 2 // Jumlah kolom grid Anda
        val spacingInDp = 2 // Jarak yang Anda inginkan dalam dp

        val spacingInPixels = spacingInDp.dpToPx(requireContext())

        rvOBD = binding.rvObd
        rvOBD.setHasFixedSize(true)
        rvOBD.layoutManager = GridLayoutManager(activity, 2)
        obdAdapter = OBDAdapter(
            itemHeight
        )
        rvOBD.adapter = obdAdapter
        if (binding.rvObd.itemDecorationCount > 0) {
            binding.rvObd.removeItemDecorationAt(0)
        }

        binding.rvObd.addItemDecoration(GridSpacingItemDecoration(spanCount, spacingInPixels, false))
    }

    //mengamati perubahan data lalu memasukkannya kedalam adapter recycle view
    private fun observeDataOBD(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.obdItemsState.collect { updatedList ->
                    obdAdapter.submitList(updatedList)
                }
            }
        }
    }
}