package com.dannylumen.choozi.ui.select_ordering

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dannylumen.choozi.databinding.FragmentSelectOrderingBinding
import com.dannylumen.choozi.ui.shared.AutoResetController

class SelectOrderingFragment : Fragment() {

    private var _binding: FragmentSelectOrderingBinding? = null // For ViewBinding
    private val binding get() = _binding!!
    private var autoResetController: AutoResetController? = null

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

        val controller = AutoResetController(
            button = resetButtonInstance,
            onReset = {
                fingerOrderingView.publicResetView()
                resetButtonInstance.visibility = View.GONE
            }
        )
        autoResetController = controller

        resetButtonInstance.setOnClickListener {
            controller.performReset()
        }

        fingerOrderingView.onAllAnimationsCompleteListener = {
            controller.startAutoReset(requireContext())
        }

        fingerOrderingView.onActiveFingerCountChangedListener = { fingerCount, isEndState ->
            (activity as? com.dannylumen.choozi.MainActivity)?.setThemeFabVisible(fingerCount == 0 && !isEndState)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoResetController?.cancel()
        autoResetController = null
        binding.fingerOrderingView.onInteractionStateChangeListener = null
        binding.fingerOrderingView.onActiveFingerCountChangedListener = null
        _binding = null // Clear ViewBinding reference
    }
}
