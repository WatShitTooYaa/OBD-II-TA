package com.example.obd_iiservice.dtc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DTCViewModel @Inject constructor() : ViewModel() {
    private var _listDTC = MutableStateFlow<List<DTCItem>>(emptyList())
    val listDTC : StateFlow<List<DTCItem>> = _listDTC.asStateFlow()

    fun parseAndSetDTC(rawResponse: String) {
        val dtcCodes = parseDTCResponse(rawResponse)
        _listDTC.value = dtcCodes
    }

    private fun parseDTCResponse(response: String): List<DTCItem> {
        val clean = response.replace("\\s".toRegex(), "")
        Log.d("OBD", "Raw DTC response: $response")

        if (!clean.startsWith("43")) return emptyList()

        val hexData = clean.removePrefix("43")
        val dtcList = mutableListOf<DTCItem>()

        for (i in hexData.indices step 4) {
            if (i + 4 > hexData.length) break
            val code = hexData.substring(i, i + 4)
            if (code == "0000") continue
            val parsedCode = convertHexToDTC(code)
            dtcList.add(DTCItem(parsedCode, "Deskripsi tidak tersedia"))
        }

        return dtcList
    }

    private fun convertHexToDTC(hex: String): String {
        val firstChar = hex[0]
        val dtcType = when (firstChar) {
            '0', '1', '2', '3' -> "P0"
            '4', '5', '6', '7' -> "C0"
            '8', '9', 'A', 'B' -> "B0"
            'C', 'D', 'E', 'F' -> "U0"
            else -> "P0"
        }
        return dtcType + hex.substring(1)
    }

}