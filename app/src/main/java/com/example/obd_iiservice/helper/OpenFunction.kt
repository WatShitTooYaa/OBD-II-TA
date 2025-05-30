package com.example.obd_iiservice.helper

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.IOException

fun saveLogToFile(context: Context, tag: String, status: String, message: String) {
    try {
        val logFile = File(context.filesDir, "app_log.txt")
        val logEntry = "${System.currentTimeMillis()} | $status | $tag: $message\n"
        logFile.appendText(logEntry)
    } catch (e: IOException) {
        Log.e("FileLogger", "Error writing log: ${e.message}")
    }
}


fun readLogFromFile(context: Context): String {
    return try {
        val logFile = File(context.filesDir, "app_log.txt")
        if (logFile.exists()) {
            logFile.readText()
        } else {
            "Log file not found."
        }
    } catch (e: IOException) {
        "Error reading log: ${e.message}"
    }
}

fun clearLogFile(context: Context) {
    val logFile = File(context.filesDir, "app_log.txt")
    if (logFile.exists()) {
        logFile.writeText("") // Kosongkan isinya
    }
}

fun makeToast(context: Context, message: String){
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}