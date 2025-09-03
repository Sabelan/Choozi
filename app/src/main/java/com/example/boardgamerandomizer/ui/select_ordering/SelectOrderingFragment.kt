package com.example.boardgamerandomizer.ui.select_ordering

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.boardgamerandomizer.R
import com.example.boardgamerandomizer.databinding.FragmentSelectOrderingBinding
import com.example.boardgamerandomizer.ui.select_ordering.FingerOrderingView

class SelectOrderingFragment : Fragment() {

    private var _binding: FragmentSelectOrderingBinding? = null // For ViewBinding
    private val binding get() = _binding!!

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

        // You might want to show the reset button after selection is complete.
        // This would require a callback from the FingerOrderingView to the Fragment.
        // For simplicity, this example keeps it manual or could be triggered
        // after a certain delay within the FingerOrderingView itself.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear ViewBinding reference
    }

    // Companion object from your code
    companion object {
        fun newInstance() = SelectOrderingFragment()
    }
}
