package com.example.obd_iiservice.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.obd_iiservice.main.MainRepository
import com.example.obd_iiservice.obd.OBDItem
import com.example.obd_iiservice.obd.OBDRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    obdRepository: OBDRepository
) : ViewModel() {

    val obdList = listOf(
        OBDItem(
            "RPM",
            "0",
            "rpm",
            startValue = "0",
            endValue = "10000",
            currValue = "0"
        ),
        OBDItem(
            "Speed",
            "0",
            "km/h",
            startValue = "0",
            endValue = "250",
            currValue = "0"
        ),
        OBDItem(
            "Throttle",
            "0",
            "%",
            startValue = "0",
            endValue = "100",
            currValue = "0"
        ),
        OBDItem(
            "Temperature",
            "0",
            "°C",
            startValue = "0",
            endValue = "140",
            currValue = "0"
        ),
        OBDItem(
            "MAF",
            "0",
            "g/s",
            startValue = "0",
            endValue = "120",
            currValue = "0"
        ),
        OBDItem(
            "Fuel Consumption",
            "0",
            "Km/L",
            startValue = "0",
            endValue = "30",
            currValue = "0"
        ),
    )

    // StateFlow internal yang menyimpan List<OBDItem> lengkap
//    private val _obdItemsState = MutableStateFlow<List<OBDItem>>(obdList)

    // StateFlow publik yang akan diobservasi oleh Fragment
    val obdItemsState: StateFlow<List<OBDItem>> = obdRepository.obdItemsState

//    fun onNewObdDataReceived(newDataMap: Map<String, String>) {
//        _obdItemsState.update { currentList ->
//            // Buat list baru dengan nilai yang diperbarui
//            currentList.map { obdItem ->
//                // Cek apakah item ini ada di data map yang baru.
//                // Jika ada, gunakan nilai baru. Jika tidak, pertahankan nilai lama.
//                val newValue = newDataMap[obdItem.label]
//                if (newValue != null) {
//                    obdItem.copy(currValue = newValue) // .copy() adalah cara aman untuk membuat instance baru
//                } else {
//                    obdItem // Tidak ada perubahan, kembalikan item yang sama
//                }
//            }
//        }
//    }
}