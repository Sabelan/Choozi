package com.dannylumen.choozi.ui.select_teams

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.dannylumen.choozi.R
import com.dannylumen.choozi.databinding.FragmentTeamSelectBinding
import com.dannylumen.choozi.ui.shared.AutoResetController

class SelectTeamsFragment : Fragment() {

    private var _binding: FragmentTeamSelectBinding? = null
    private val binding get() = _binding!!
    private var autoResetController: AutoResetController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTeamSelectorView()
    }

    private fun setupTeamSelectorView() {
        Log.d("SelectTeamsFragment", "Setting up TeamSelectorView")
        val resetButton = binding.selectTeamsResetButton
        val radioButtonGroup = binding.teamCountRadioGroup

        val controller = AutoResetController(
            button = resetButton,
            onReset = {
                binding.teamSelectorView.resetSelectionProcess()
                resetButton.visibility = View.GONE
                radioButtonGroup.visibility = View.VISIBLE
            }
        )
        autoResetController = controller

        radioButtonGroup.setOnCheckedChangeListener { group, checkedId ->
            controller.cancel()
            val selectedRadioButton = view?.findViewById<RadioButton>(checkedId)
            when (selectedRadioButton?.id) {
                R.id.radioTwoTeams -> binding.teamSelectorView.numberOfTeams = 2
                R.id.radioThreeTeams -> binding.teamSelectorView.numberOfTeams = 3
                R.id.radioFourTeams -> binding.teamSelectorView.numberOfTeams = 4
            }
            binding.teamSelectorView.resetSelectionProcess(clearFingers = false)
            binding.teamSelectorView.possiblyStartSelectionProcess()
        }

        binding.teamSelectorView.onTimerStartListener = {
            Log.d("TeamSelectorFragment", "Timer Started")
            controller.cancel()
            resetButton.visibility = View.GONE
        }

        binding.teamSelectorView.onTeamAssignmentCompleteListener = {
            Log.d("TeamSelectorFragment", "Team assignment complete, animations starting.")
            radioButtonGroup.visibility = View.GONE
        }

        binding.teamSelectorView.onAllTeamAnimationsCompleteListener = {
            Log.d("TeamSelectorFragment", "All team animations complete.")
            controller.startAutoReset(requireContext())
        }

        binding.teamSelectorView.onActiveFingerCountChangedListener = { fingerCount, isEndState ->
            (activity as? com.dannylumen.choozi.MainActivity)?.setThemeFabVisible(fingerCount == 0 && !isEndState)
        }

        resetButton.setOnClickListener {
            controller.performReset()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoResetController?.cancel()
        autoResetController = null
        binding.teamSelectorView.onTimerStartListener = null
        binding.teamSelectorView.onTeamAssignmentCompleteListener = null
        binding.teamSelectorView.onAllTeamAnimationsCompleteListener = null
        binding.teamSelectorView.onInteractionStateChangeListener = null
        binding.teamSelectorView.onActiveFingerCountChangedListener = null
        _binding = null
    }
}
