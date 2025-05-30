package com.example.obd_iiservice.log

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.ActivityLogViewBinding
import com.example.obd_iiservice.helper.clearLogFile
import com.example.obd_iiservice.helper.readLogFromFile

class LogViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogViewBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLogViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLog(context = this)

        binding.btnRefreshLog.setOnClickListener {
            showLog(this)
        }

        binding.btnClearLog.setOnClickListener {
            showLog(this)
            clearLogFile(context = this)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_log)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showLog(context: Context) {
        val logText = readLogFromFile(context = context)
        binding.tvLogContent.text = logText

    }
}

