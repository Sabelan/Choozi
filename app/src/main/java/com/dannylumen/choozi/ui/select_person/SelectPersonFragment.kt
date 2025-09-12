package com.dannylumen.choozi.ui.select_person

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dannylumen.choozi.databinding.FragmentSelectPersonBinding
import com.dannylumen.choozi.ui.shared.AudioPlayer

class SelectPersonFragment : Fragment() {

    private var _binding: FragmentSelectPersonBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    var chargeAudioPlayer: AudioPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectPersonBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFingerSelectorView()
    }

    private fun setupFingerSelectorView() {
        Log.d("HomeFragment", "settingUpFingerSelectorView")
        // Access views using binding
        val fingerSelectorViewInstance = binding.fingerSelectorView
        val resetButtonInstance = binding.selectPersonResetButton

        // Set the listener for when selection is complete
        fingerSelectorViewInstance.onSelectionCompleteListener = {
            // This block is executed when a finger is selected after the timer
            Log.d("HomeFragment", "onSelectionComplete triggered! Setting resetButton VISIBLE.")
            resetButtonInstance.visibility = View.VISIBLE
        }

        // Optional: Listen to timer start to hide the reset button if it was visible
        fingerSelectorViewInstance.onTimerStartListener = {
            resetButtonInstance.visibility = View.GONE
        }

        resetButtonInstance.setOnClickListener {
            fingerSelectorViewInstance.resetSelectionProcess()
            resetButtonInstance.visibility = View.GONE // Hide reset button again
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Important to prevent memory leaks with listeners, especially if FingerSelectorView could outlive the fragment's view
        binding.fingerSelectorView.onSelectionCompleteListener = null
        binding.fingerSelectorView.onTimerStartListener = null
        _binding = null
        // Release charge audio player
        chargeAudioPlayer?.release()
    }
}
