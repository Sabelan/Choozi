package com.dannylumen.choozi.ui.select_ordering

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dannylumen.choozi.databinding.FragmentSelectOrderingBinding

class SelectOrderingFragment : Fragment() {

    private var _binding: FragmentSelectOrderingBinding? = null // For ViewBinding
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectOrderingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fingerOrderingView = binding.fingerOrderingView
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
    }
}
