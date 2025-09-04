package com.example.boardgamerandomizer.ui.select_ordering

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.boardgamerandomizer.R
import com.example.boardgamerandomizer.databinding.FragmentSelectOrderingBinding
import com.example.boardgamerandomizer.ui.shared.AudioPlayer

class SelectOrderingFragment : Fragment() {

    private var _binding: FragmentSelectOrderingBinding? = null // For ViewBinding
    private val binding get() = _binding!!

    var chargeAudioPlayer: AudioPlayer? = null

    // viewModel is already there from your provided code
    private val viewModel: SelectOrderingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectOrderingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chargeAudioPlayer = AudioPlayer(requireContext())
        chargeAudioPlayer?.loadSound(R.raw.charge_sound)
        val fingerOrderingView = binding.fingerOrderingView
        fingerOrderingView.chargeAudioPlayer = chargeAudioPlayer
        val resetButtonInstance = binding.selectOrderingResetButton
        // If you added a reset button:
        resetButtonInstance.setOnClickListener {
            fingerOrderingView.publicResetView()
            resetButtonInstance.visibility = View.GONE
        }

        fingerOrderingView.onAllAnimationsCompleteListener = {
            resetButtonInstance.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear ViewBinding reference
        // Release AudioPlayer resources when the view is destroyed
        chargeAudioPlayer?.release()
    }

    // Companion object from your code
    companion object {
        fun newInstance() = SelectOrderingFragment()
    }
}
