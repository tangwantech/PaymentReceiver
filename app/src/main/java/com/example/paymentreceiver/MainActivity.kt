package com.example.paymentreceiver

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.paymentreceiver.ui.MainViewModel
import com.example.paymentreceiver.ui.PaymentMessageAdapter
import com.example.paymentreceiver.ui.ProviderFilter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val adapter = PaymentMessageAdapter()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var btnPermission: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private var smsObserver: ContentObserver? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] ?: false

        if (readGranted || checkSmsPermission()) {
            btnPermission.visibility = View.GONE
            registerSmsObserver()
            viewModel.syncInbox()
        } else {
            showPermissionNeededUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupBottomNavigation()
        setupBackNavigation()
        observeViewModel()
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (checkSmsPermission()) {
            registerSmsObserver()
            viewModel.syncInbox()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterSmsObserver()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnPermission = findViewById(R.id.btnPermission)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        btnPermission.setOnClickListener {
            requestSmsPermissions()
        }
    }

    private fun setupToolbar() {
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    confirmAndDeleteSelected()
                    true
                }
                else -> false
            }
        }

        toolbar.setNavigationOnClickListener {
            if (adapter.isSelectionMode) {
                adapter.clearSelection()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.adapter = adapter

        adapter.onSelectionChangedListener = { selectedCount ->
            val deleteMenuItem = toolbar.menu.findItem(R.id.action_delete)
            if (selectedCount > 0) {
                toolbar.title = "$selectedCount selected"
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
                deleteMenuItem?.isVisible = true
            } else {
                toolbar.title = "Payment Receiver"
                toolbar.navigationIcon = null
                deleteMenuItem?.isVisible = false
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            if (adapter.isSelectionMode) {
                adapter.clearSelection()
            }
            when (item.itemId) {
                R.id.nav_mobile_money -> {
                    viewModel.selectProvider(ProviderFilter.MOBILE_MONEY)
                    true
                }
                R.id.nav_orange_money -> {
                    viewModel.selectProvider(ProviderFilter.ORANGE_MONEY)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.isSelectionMode) {
                    adapter.clearSelection()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun confirmAndDeleteSelected() {
        val selectedMessages = adapter.getSelectedMessages()
        if (selectedMessages.isEmpty()) return

        val count = selectedMessages.size
        val messageText = if (count == 1) {
            "Are you sure you want to delete this message permanently from Room database?"
        } else {
            "Are you sure you want to delete these $count messages permanently from Room database?"
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Messages")
            .setMessage(messageText)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMessages(selectedMessages)
                adapter.clearSelection()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredMessages.collect { messages ->
                        viewModel.updateMessages(messages)
                        adapter.submitList(messages)
                        updateUiState(messages.isEmpty())
                    }
                }

                launch {
                    viewModel.isSyncing.collect { isSyncing ->
                        progressBar.visibility = if (isSyncing) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.selectedProvider.collect {
                        updateUiState(viewModel.filteredMessages.value.isEmpty())
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (checkSmsPermission()) {
            btnPermission.visibility = View.GONE
            registerSmsObserver()
            viewModel.syncInbox()
        } else {
            showPermissionNeededUi()
            requestSmsPermissions()
        }
    }

    private fun checkSmsPermission(): Boolean {
        val readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        return readSms == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            )
        )
    }

    private fun registerSmsObserver() {
        if (smsObserver == null && checkSmsPermission()) {
            smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    viewModel.syncInbox()
                }
            }
            try {
                contentResolver.registerContentObserver(
                    Telephony.Sms.CONTENT_URI,
                    true,
                    smsObserver!!
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun unregisterSmsObserver() {
        smsObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            smsObserver = null
        }
    }

    private fun showPermissionNeededUi() {
        tvEmptyState.text = "SMS permission is required to read payment messages from MobileMoney and OrangeMoney."
        btnPermission.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun updateUiState(isEmpty: Boolean) {
        if (!checkSmsPermission()) {
            showPermissionNeededUi()
            return
        }

        if (isEmpty) {
            val providerName = viewModel.selectedProvider.value.displayName
            tvEmptyState.text = "No payment messages found for $providerName."
            btnPermission.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
