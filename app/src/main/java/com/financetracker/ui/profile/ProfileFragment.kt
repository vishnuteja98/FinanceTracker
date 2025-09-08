package com.financetracker.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.financetracker.R
import com.financetracker.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuItems()
    }

    private fun setupMenuItems() {
        binding.menuBankAccounts.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_bank_accounts)
        }

        binding.menuAnalytics.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_analytics)
        }

        // Settings menu commented out until needed
        // binding.menuSettings.setOnClickListener {
        //     // TODO: Navigate to settings when implemented
        // }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
