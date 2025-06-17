package com.example.obd_iiservice.ui.log

import android.content.Context
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.obd_iiservice.R
import com.example.obd_iiservice.databinding.FragmentHomeBinding
import com.example.obd_iiservice.databinding.FragmentLogBinding
import com.example.obd_iiservice.helper.clearLogFile
import com.example.obd_iiservice.helper.readLogFromFile

class LogFragment : Fragment() {

    private var _binding : FragmentLogBinding? = null

    private val binding get() = _binding!!
    companion object {
        fun newInstance() = LogFragment()
    }

    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLog(context = requireContext())

        binding.btnRefreshLog.setOnClickListener {
            showLog(requireContext())
        }

        binding.btnClearLog.setOnClickListener {
            showLog(requireContext())
            clearLogFile(context = requireContext())
        }
    }

    private fun showLog(context: Context) {
        val logText = readLogFromFile(context = context)
        binding.tvLogContent.text = logText
        binding.tvLogContent.setTextColor(resources.getColor(R.color.hitam_pekat, requireContext().theme))
    }
}