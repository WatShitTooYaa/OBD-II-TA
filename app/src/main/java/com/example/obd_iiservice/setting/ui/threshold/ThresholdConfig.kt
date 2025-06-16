package com.example.obd_iiservice.setting.ui.threshold

data class ThresholdConfig(
    val rpm : Int,
    val speed: Int,
    val throttle: Int,
    val temp : Int,
    val maf : Double
)
