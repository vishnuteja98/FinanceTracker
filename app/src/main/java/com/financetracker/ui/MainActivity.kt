package com.financetracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment

import androidx.navigation.ui.setupWithNavController
import com.financetracker.R
import com.financetracker.databinding.ActivityMainBinding
import com.financetracker.ui.viewmodels.MainViewModel
import com.financetracker.utils.TestDataHelper

import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val SHOW_DEBUG_TOASTS = false // Set to true for debugging
    }

    lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var testDataHelper: TestDataHelper
    


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsPermissionGranted = permissions[Manifest.permission.READ_SMS] == true
        val receiveSmsPermissionGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        
        if (smsPermissionGranted && receiveSmsPermissionGranted) {
            // Only show success message when debugging
            if (SHOW_DEBUG_TOASTS) {
                Toast.makeText(this, "SMS permissions granted. Ready for transaction detection!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Always show permission required message as it's important for functionality
            Toast.makeText(this, "SMS permissions are required for transaction detection", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Wait for the NavHostFragment to be ready
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Hide the action bar since we have a custom header
        supportActionBar?.hide()

        val navView: BottomNavigationView = binding.navView
        navView.setupWithNavController(navController)

        // Check and request SMS permissions for transaction detection
        checkAndRequestPermissions()





        // Initialize bank accounts only (no test data)
        lifecycleScope.launch {
            viewModel.initializeDefaultBankAccounts()
        }

        // Observe pending transaction count for badge
        viewModel.pendingTransactionCount.observe(this) { count ->
            val badge = navView.getOrCreateBadge(R.id.navigation_pending)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.isVisible = false
            }
        }
    }


    

    


    private fun checkAndRequestPermissions() {
        val smsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        val receiveSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)

        if (smsPermission != PackageManager.PERMISSION_GRANTED || 
            receiveSmsPermission != PackageManager.PERMISSION_GRANTED) {
            
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            ))
        } else {
            // Permissions already granted
            Toast.makeText(this, "Ready for manual testing!", Toast.LENGTH_SHORT).show()
        }
    }



}
