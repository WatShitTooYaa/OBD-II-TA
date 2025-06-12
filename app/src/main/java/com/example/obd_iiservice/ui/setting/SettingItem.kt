package com.example.obd_iiservice.ui.setting

import android.app.Activity
import android.content.Intent

data class SettingItem(
    val icon: Int,
    val label: String,
    val description: String,
    val targetActivity : Class<out Activity>
)
