package com.example.nav_drawer_test

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
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
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
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
import com.punchthrough.blestarterappandroid.ble.*
import kotlinx.android.synthetic.main.app_bar_main.view.*
import kotlinx.android.synthetic.main.fragment_developer.*
import kotlinx.android.synthetic.main.fragment_home.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.selector
import org.jetbrains.anko.yesButton
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

    /**************** BLE Device Scan ****************/
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

    /**************** BLE Device (Connected) Operations ****************/
    private val dateFormatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val adcVal1ServiceUuid = UUID.fromString("15005991-b131-3396-014c-664c9867b917")

    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(gattDevice)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    private val characteristicProperties by lazy {
        characteristics.map { characteristic ->
            characteristic to mutableListOf<CharacteristicProperty>().apply {
                if (characteristic.isNotifiable()) add(CharacteristicProperty.Notifiable)
                if (characteristic.isIndicatable()) add(CharacteristicProperty.Indicatable)
                if (characteristic.isReadable()) add(CharacteristicProperty.Readable)
                if (characteristic.isWritable()) add(CharacteristicProperty.Writable)
                if (characteristic.isWritableWithoutResponse()) {
                    add(CharacteristicProperty.WritableWithoutResponse)
                }
            }.toList()
        }.toMap()
    }
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics) { characteristic ->
            showCharacteristicOptions(characteristic)
        }
    }
    private var notifyingCharacteristics = mutableListOf<UUID>()
    private enum class CharacteristicProperty {
        Readable,
        Writable,
        WritableWithoutResponse,
        Notifiable,
        Indicatable;

        val action
            get() = when (this) {
                Readable -> "Read"
                Writable -> "Write"
                WritableWithoutResponse -> "Write Without Response"
                Notifiable -> "Toggle Notifications"
                Indicatable -> "Toggle Indications"
            }
    }

    /**************** Misc ****************/
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
    private lateinit var devLogTextView: TextView
    private lateinit var devLogScrollView: ScrollView
    private lateinit var datalistDataTextView: TextView
    private lateinit var datalistDataScrollView: ScrollView

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
        // TODO: separate listener for Scan and Operation
        if (!deviceConnected) {
            ConnectionManager.registerListener(connectionEventListenerScan)
        }
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

    private fun logOperation(message: String) {
        val formattedMessage = String.format("%s: %s", dateFormatter.format(Date()), message)
        runOnUiThread {
            val currentLogText = if (devLogTextView.text.isEmpty()) {
                "Beginning of log."
            } else {
                devLogTextView.text
            }
            devLogTextView.text = "$currentLogText\n$formattedMessage"
            devLogScrollView.post { devLogScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun appendDatalist(message: String) {
        val formattedMessage = String.format("%s: %s", timeFormatter.format(Date()), message)
        runOnUiThread {
            val currentLogText = if (datalistDataTextView.text.isEmpty()) {
                "    Time       Data"
            } else {
                datalistDataTextView.text
            }
            datalistDataTextView.text = "$currentLogText\n$formattedMessage"
            datalistDataScrollView.post { datalistDataScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun showCharacteristicOptions(characteristic: BluetoothGattCharacteristic) {
        characteristicProperties[characteristic]?.let { properties ->
            selector("Select an action to perform", properties.map { it.action }) { _, i ->
                when (properties[i]) {
                    CharacteristicProperty.Readable -> {
                        logOperation("Reading from ${characteristic.uuid}")
                        ConnectionManager.readCharacteristic(gattDevice, characteristic)
                    }
                    CharacteristicProperty.Writable, CharacteristicProperty.WritableWithoutResponse -> {
                        showWritePayloadDialog(characteristic)
                    }
                    CharacteristicProperty.Notifiable, CharacteristicProperty.Indicatable -> {
                        if (notifyingCharacteristics.contains(characteristic.uuid)) {
                            logOperation("Disabling notifications on ${characteristic.uuid}")
                            ConnectionManager.disableNotifications(gattDevice, characteristic)
                        } else {
                            logOperation("Enabling notifications on ${characteristic.uuid}")
                            ConnectionManager.enableNotifications(gattDevice, characteristic)
                        }
                    }
                }
            }
        }
    }

    private fun showWritePayloadDialog(characteristic: BluetoothGattCharacteristic) {
        val hexField = layoutInflater.inflate(R.layout.edittext_hex_payload, null) as EditText
        alert {
            customView = hexField
            isCancelable = false
            yesButton {
                with(hexField.text.toString()) {
                    if (isNotBlank() && isNotEmpty()) {
                        val bytes = hexToBytes()
                        logOperation("Writing to ${characteristic.uuid}: ${bytes.toHexString()}")
                        ConnectionManager.writeCharacteristic(gattDevice, characteristic, bytes)
                    } else {
                        logOperation("Please enter a hex payload to write to ${characteristic.uuid}")
                    }
                }
            }
            noButton {}
        }.show()
        hexField.showKeyboard()
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

    private val connectionEventListenerScan by lazy {
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
//                ConnectionManager.enableNotifications(gattDevice, gatt.findCharacteristic(adcVal1ServiceUuid)!!)
                ConnectionManager.enableNotifications(gattDevice, characteristics.first{it.uuid == adcVal1ServiceUuid})
//                ConnectionManager.servicesOnDevice(gattDevice)?.forEach { service ->
//                    service.characteristics?.find { characteristic ->
//                        characteristic.uuid == adcVal1ServiceUuid
//                    }.let { matchingCharacteristic ->
//                        ConnectionManager.enableNotifications(gattDevice, matchingCharacteristic!!)
//                    }
//                }

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

    private val connectionEventListenerDev by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected from device."
                        positiveButton("OK") { } // TODO: onBackPressed()
                    }.show()
                }
            }

            onCharacteristicRead = { _, characteristic ->
                logOperation("Read from ${characteristic.uuid}: ${characteristic.value.toHexString()}")
            }

            onCharacteristicWrite = { _, characteristic ->
                logOperation("Wrote to ${characteristic.uuid}")
            }

            onMtuChanged = { _, mtu ->
                logOperation("MTU updated to $mtu")
            }

            onCharacteristicChanged = { _, characteristic ->
                logOperation("Value changed on ${characteristic.uuid}: ${characteristic.value.toHexString()}")
            }

            onNotificationsEnabled = { _, characteristic ->
                logOperation("Enabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                logOperation("Disabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    private val connectionEventListenerDatalist by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {}

            onCharacteristicRead = { _, _ -> }

            onCharacteristicWrite = { _, _ -> }

            onMtuChanged = { _, _ -> }

            onCharacteristicChanged = { _, characteristic ->
                appendDatalist(characteristic.value.toHexString())
            }

            onNotificationsEnabled = { _, _ -> }

            onNotificationsDisabled = { _, _ -> }
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

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun EditText.showKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        requestFocus()
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()

    private fun onClickScanButton() {
        if (isScanning) {
            stopBleScan()
        } else {
            startBleScan()
        }
    }

    fun onHomeFragmentViewCreated(recyclerView: RecyclerView, scanButton: Button) {
//        Timber.i("onHomeFragmentViewCreated")
        if (mainViewCreated) {
            homeScanResultRecyclerView = recyclerView
            homeScanButton = scanButton

            setupRecyclerViewScanResult()
            scanButton.setOnClickListener { onClickScanButton() }

            ConnectionManager.registerListener(connectionEventListenerScan)
        }

        if (deviceConnected) {
            deviceConnected = false
            ConnectionManager.unregisterListener(connectionEventListenerDev)
            ConnectionManager.unregisterListener(connectionEventListenerDatalist)
            ConnectionManager.teardownConnection(gattDevice)
        }

        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    fun onHomeFragmentDestroyView() {
        if (isScanning) {
            stopBleScan()
        }
    }

    fun onDeveloperFragmentViewCreated(
        recyclerView: RecyclerView,
        requestMtuButton: Button,
        logTextView: TextView,
        logScrollView: ScrollView
    ) {
        devCharacteristicsRecyclerView = recyclerView
        devMtuButton = requestMtuButton
        devLogTextView = logTextView
        devLogScrollView = logScrollView

        if (deviceConnected) {
            setupRecyclerViewCharacteristics()

            devMtuButton.setOnClickListener {
                if (mtu_field.text.isNotEmpty() && mtu_field.text.isNotBlank()) {
                    mtu_field.text.toString().toIntOrNull()?.let { mtu ->
                        logOperation("Requesting for MTU value of $mtu")
                        ConnectionManager.requestMtu(gattDevice, mtu)
                    } ?: logOperation("Invalid MTU value: ${mtu_field.text}")
                } else {
                    logOperation("Please specify a numeric value for desired ATT MTU (23-517)")
                }
                // hideKeyboard() // TODO
            }

            ConnectionManager.registerListener(connectionEventListenerDev)
        }

        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    fun onDeveloperFragmentDestroyView() {
//        Timber.i("onDeveloperFragmentDestroyView")
//        if (deviceConnected) {
//            ConnectionManager.unregisterListener(connectionEventListenerDev)
//            characteristicsResults.clear()
//            characteristicAdapter.notifyDataSetChanged()
//        }
    }

    fun onDatalistFragmentViewCreated(
        dataTextView: TextView,
        dataScrollView: ScrollView
    ) {
        datalistDataTextView = dataTextView
        datalistDataScrollView = dataScrollView

        if (deviceConnected) {
//            setupRecyclerViewData()

            ConnectionManager.registerListener(connectionEventListenerDatalist)
        }

        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    fun onDatalistFragmentDestroyView() {
//        Timber.i("onDatalistFragmentDestroyView")
//        if (deviceConnected) {
//            ConnectionManager.unregisterListener(connectionEventListenerDatalist)
//        }
    }

}
