package com.example.obd_iiservice.obd

//data class OBDItem(
//    val iconResId: Int,      // R.drawable.icon_throttle
//    val label: String,       // e.g., "Throttle"
//    var value: String,       // e.g., "75%"
//    var isNormal: Boolean    // menentukan warna lingkaran status
//)

data class OBDItem(
    val label: String,       // e.g., "Throttle"
    var value: String,       // e.g., "75%"
    var unit: String,    // menentukan warna lingkaran status
    var startValue: String,
    var endValue : String,
    var currValue: String,
)
//    val iconResId: Int,      // R.drawable.icon_throttle
