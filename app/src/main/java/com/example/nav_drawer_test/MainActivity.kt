package com.example.nav_drawer_test

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.View
import android.widget.Button
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.nav_drawer_test.databinding.ActivityMainBinding
import com.example.nav_drawer_test.ui.SharedViewModel
import com.example.nav_drawer_test.ui.home.HomeFragment
import com.punchthrough.blestarterappandroid.ScanResultAdapter
import com.punchthrough.blestarterappandroid.CharacteristicAdapter
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import kotlinx.android.synthetic.main.app_bar_main.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import org.jetbrains.anko.alert
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

class MainActivity : AppCompatActivity() {

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var isScanning = false
        set(value) {
            field = value
//            runOnUiThread { scan_button.text = if (value) "Stop Scan" else "Start Scan" }
            runOnUiThread { homeScanButton.text = if (value) "Stop Scan" else "Start Scan" }
        }

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Timber.w("Connecting to $address")
                ConnectionManager.connect(this, this@MainActivity)
            }
        }
    }

    private val dateFormatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(gattDevice)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics) { characteristic ->
//            showCharacteristicOptions(characteristic) //TODO
        }
    }

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private val navController get() = findNavController(R.id.nav_host_fragment_content_main)

    private var mainViewCreated = false
    private var deviceConnected = false

    private lateinit var homeScanButton: Button
    private lateinit var homeScanResultRecyclerView: RecyclerView
    private lateinit var gattDevice: BluetoothDevice
    private lateinit var devMtuButton: Button
    private lateinit var devCharacteristicsRecyclerView: RecyclerView

    /*******************************************
     * Activity function overrides
     *******************************************/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

//        binding.appBarMain.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_home, R.id.nav_data, R.id.nav_dev), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

//        supportFragmentManager.executePendingTransactions()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        Timber.i("$navHostFragment")

//        navHostFragment!!.childFragmentManager.getFragments()
//                                              .getPrimaryNavigationFragment()

//        navHostFragment!!.childFragmentManager.executePendingTransactions()
//        val homeFragment: HomeFragment?
//            = navHostFragment!!.childFragmentManager.findFragmentById(R.id.nav_home) as HomeFragment?

        val homeFragment: HomeFragment? =
            navHostFragment!!.childFragmentManager.fragments[0] as HomeFragment?
        Timber.i("$homeFragment")

        homeScanButton = scan_button
        homeScanResultRecyclerView = scan_results_recycler_view
        scan_button.setOnClickListener { onClickScanButton() }
        setupRecyclerViewScanResult()
        mainViewCreated = true
    }

    override fun onResume() {
        super.onResume()
        ConnectionManager.registerListener(connectionEventListener)
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    /*******************************************
     * Private functions
     *******************************************/

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        } else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            alert {
                title = "Location permission required"
                message = "Starting from Android M (6.0), the system requires apps to be granted " +
                        "location access in order to scan for BLE devices."
                isCancelable = false
                positiveButton(android.R.string.ok) {
                    requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }.show()
        }
    }

    private fun setupRecyclerViewScanResult() {
//        scan_results_recycler_view.apply {
        homeScanResultRecyclerView.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

//        val animator = scan_results_recycler_view.itemAnimator
        val animator = homeScanResultRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun setupRecyclerViewCharacteristics() {
//        characteristics_recycler_view.apply {
        devCharacteristicsRecyclerView.apply {
            adapter = characteristicAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = devCharacteristicsRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    /*******************************************
     * Callback bodies
     *******************************************/

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
//                Intent(this@MainActivity, BleOperationsActivity::class.java).also {
//                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
//                    startActivity(it)
//                }

                gattDevice = gatt.device
//                R.id.action_nav_home_to_nav_data: Clears backstack
//                runOnUiThread {
                Handler(Looper.getMainLooper()).post {
                    navController.navigate(R.id.nav_dev)
                }

                deviceConnected = true

                Timber.i("Connecting to $gatt.device")
                ConnectionManager.unregisterListener(this)
            }
            onDisconnect = {
                runOnUiThread {
                    deviceConnected = false
                    alert {
                        title = "Disconnected"
                        message = "Disconnected or unable to connect to device."
                        positiveButton("OK") {}
                    }.show()
                }
            }
        }
    }

    /*******************************************
     * Extension functions
     *******************************************/

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    fun getNavBackStackEntryCount() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val backStackEntryCount = navHostFragment?.childFragmentManager?.backStackEntryCount
        Timber.i("NavBackStackCount = $backStackEntryCount")
    }

    private fun onClickScanButton() {
        if (isScanning) {
            stopBleScan()
        } else {
            startBleScan()
        }
    }

    fun onHomeFragmentViewCreated(recyclerView: RecyclerView, scanButton: Button) {
        if (mainViewCreated) {
            homeScanResultRecyclerView = recyclerView
            homeScanButton = scanButton
            setupRecyclerViewScanResult()
            scanButton.setOnClickListener { onClickScanButton() }

            ConnectionManager.registerListener(connectionEventListener)
            if (!bluetoothAdapter.isEnabled) {
                promptEnableBluetooth()
            }
        }

        if (deviceConnected) {
            deviceConnected = false
            ConnectionManager.teardownConnection(gattDevice)
        }
    }

    fun onHomeFragmentDestroyView() {
        if (isScanning) {
            stopBleScan()
        }
    }

    fun onDeveloperFragmentViewCreated(recyclerView: RecyclerView, requestMtuButton: Button) {
        if (mainViewCreated) {
            devCharacteristicsRecyclerView = recyclerView
            devMtuButton = requestMtuButton
            setupRecyclerViewCharacteristics()
            devMtuButton.setOnClickListener { } //TODO

//            ConnectionManager.registerListener(connectionEventListener)
            if (!bluetoothAdapter.isEnabled) {
                promptEnableBluetooth()
            }
        }
    }

    fun onDeveloperFragmentDestroyView() {
//        if (deviceConnected) {
//            ConnectionManager.unregisterListener(connectionEventListenerDev)
//            characteristicsResults.clear()
//            characteristicAdapter.notifyDataSetChanged()
//        }
    }

}
