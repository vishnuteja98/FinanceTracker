package com.financetracker.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.financetracker.R
import com.financetracker.databinding.FragmentBankAccountsBinding
import com.financetracker.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BankAccountsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentBankAccountsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BankAccountsViewModel by viewModels()
    private lateinit var bankAccountAdapter: BankAccountAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBankAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        setupFab()
        hideBottomNavigation()

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun hideBottomNavigation() {
        (requireActivity() as? MainActivity)?.let { mainActivity ->
            mainActivity.binding.navView.visibility = View.GONE
        }
    }

    private fun showBottomNavigation() {
        (requireActivity() as? MainActivity)?.let { mainActivity ->
            mainActivity.binding.navView.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        bankAccountAdapter = BankAccountAdapter(
            onAccountClick = { account ->
                showEditAccountDialog(account)
            },
            onToggleActiveClick = { account ->
                if (account.isActive) {
                    viewModel.deactivateAccount(account.id)
                } else {
                    viewModel.activateAccount(account.id)
                }
            }
        )

        binding.recyclerViewBankAccounts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bankAccountAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.bankAccounts.observe(viewLifecycleOwner) { accounts ->
            bankAccountAdapter.submitList(accounts)
            
            if (accounts.isEmpty()) {
                binding.recyclerViewBankAccounts.visibility = View.GONE
                binding.textViewEmptyState.visibility = View.VISIBLE
                binding.textViewEmptyState.text = getString(R.string.message_no_bank_accounts)
            } else {
                binding.recyclerViewBankAccounts.visibility = View.VISIBLE
                binding.textViewEmptyState.visibility = View.GONE
            }
        }
    }

    private fun setupFab() {
        binding.fabAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
    }

    private fun showAddAccountDialog() {
        val dialog = AddEditBankAccountDialog.newInstance()
        dialog.setOnSaveListener { accountName, bankName, accountNumber, accountType ->
            viewModel.addBankAccount(accountName, bankName, accountNumber, accountType)
        }
        dialog.show(childFragmentManager, "AddAccountDialog")
    }

    private fun showEditAccountDialog(account: com.financetracker.data.models.BankAccount) {
        val dialog = AddEditBankAccountDialog.newInstance(account)
        dialog.setOnSaveListener { accountName, bankName, accountNumber, accountType ->
            viewModel.updateBankAccount(account.id, accountName, bankName, accountNumber, accountType)
        }
        dialog.show(childFragmentManager, "EditAccountDialog")
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // No menu items needed for now
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }
}
