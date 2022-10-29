package fr.ectalhawk.rgbweatherkit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog.Builder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import java.util.*

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE = 3
private const val UUID_CHAR_PIXELX = "BADDCAFE-0000-0000-0000-000000000002"
private const val UUID_CHAR_PIXELY = "BADDCAFE-0000-0000-0000-000000000003"
private const val UUID_CHAR_COLOR = "BADDCAFE-0000-0000-0000-000000000004"
private const val UUID_CHAR_SEND = "BADDCAFE-0000-0000-0000-000000000005"

class BLEinterface(private val act: Activity, private val context: Context) {
    //Enum and variables of class
    enum class BLELifecycleState {
        Disconnected,
        Scanning,
        Connecting,
        ConnectedDiscovering,
        ConnectedSubscribing,
        Connected
    }
    enum class AskType {
        AskOnce,
        InsistUntilSuccess
    }
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val  bluetoothManager = getSystemService(context, BluetoothManager::class.java)
        bluetoothManager!!.adapter
    }
    private var isScanning = false
    private var connectedGatt: BluetoothGatt? = null
    private var pixelXCharacteristic: BluetoothGattCharacteristic? = null
    private var pixelYCharacteristic: BluetoothGattCharacteristic? = null
    private var colorCharacteristic: BluetoothGattCharacteristic? = null
    private var sendCharacteristic: BluetoothGattCharacteristic? = null
    /*private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
        .build()
    */
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    fun prepareAndStartBleScan(){
        ensureBluetoothCanBeUsed { isSuccess, message ->
            myLogger(message)
            if (isSuccess) {
                safeStartBleScan()
            }
        }
    }

    private fun safeStartBleScan() {
        if (isScanning) {
            myLogger("Already scanning")
            return
        }

        //val serviceFilter = scanFilter.serviceUuid?.uuid.toString()
        //myLogger("Starting BLE scan, filter: $serviceFilter")

        isScanning = true
        lifecycleState = BLELifecycleState.Scanning
        if (ActivityCompat.checkSelfPermission( context, Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            myLogger("safeStartBleScan n'a pas les permissions nécessaires pour scanner")
            val intent = Intent(act, NoBLEAuthorization::class.java)
            startActivity(context, intent, null)
            return
        }
        bleScanner.startScan(scanCallback)
    }

    fun safeStopBleScan() {
        if (!isScanning) {
            myLogger("Already stopped")
            return
        }

        myLogger("Stopping BLE scan")
        isScanning = false
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            myLogger("safeStopBleScan n'a pas les permissions nécessaires pour stopper le scan")
            val intent = Intent(act, NoBLEAuthorization::class.java)
            startActivity(context, intent, null)
            return
        }
        bleScanner.stopScan(scanCallback)
    }

    private var lifecycleState = BLELifecycleState.Disconnected
        @SuppressLint("SetTextI18n")
        set(value) {
            field = value
            myLogger("status = $value")

            val textViewLifecycleState : TextView = act.findViewById(R.id.textViewLifecycleState)
            textViewLifecycleState.text = "State: ${value.name}"
        }

    fun connectToDevice(device : BluetoothDevice)
    {
        lifecycleState = BLELifecycleState.Connecting
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            myLogger("connectToDevice n'a pas les permissions nécessaires pour se connecter")
            val intent = Intent(act, NoBLEAuthorization::class.java)
            startActivity(context, intent, null)
            return
        }
        device.connectGatt(context, false, gattCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                myLogger("scanCallback n'a pas les permissions nécessaires pour récupérer le nom du device")
                val intent = Intent(act, NoBLEAuthorization::class.java)
                startActivity(context, intent, null)
                return
            }
            val name: String? = result.scanRecord?.deviceName ?: result.device.name
            myLogger("onScanResult name=$name address= ${result.device?.address}")


        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            myLogger("onBatchScanResults, ignoring")
        }

        override fun onScanFailed(errorCode: Int) {
            myLogger("onScanFailed errorCode=$errorCode")
            safeStopBleScan()
            lifecycleState = BLELifecycleState.Disconnected
            bleRestartLifecycle()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    myLogger("Connected to $deviceAddress")

                    // recommended on UI thread https://punchthrough.com/android-ble-guide/
                    Handler(Looper.getMainLooper()).post {
                        lifecycleState = BLELifecycleState.ConnectedDiscovering
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            myLogger("onConnectionStateChange n'a pas les permissions nécessaires pour se deconnecter")
                            val intent = Intent(act, NoBLEAuthorization::class.java)
                            startActivity(context, intent, null)
                        }
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    myLogger("Disconnected from $deviceAddress")
                    setConnectedGattToNull()
                    gatt.close()
                    lifecycleState = BLELifecycleState.Disconnected
                    bleRestartLifecycle()
                }
            } else {

                myLogger("ERROR: onConnectionStateChange status=$status deviceAddress=$deviceAddress, disconnecting")

                setConnectedGattToNull()
                gatt.close()
                lifecycleState = BLELifecycleState.Disconnected
                bleRestartLifecycle()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            myLogger("onServicesDiscovered services.count=${gatt.services.size} status=$status")
            lifecycleState = BLELifecycleState.ConnectedSubscribing

            if (status == 129 /*GATT_INTERNAL_ERROR*/) {
                // it should be a rare case, this article recommends to disconnect:
                // https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
                myLogger("ERROR: status=129 (GATT_INTERNAL_ERROR), disconnecting")
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    myLogger("sonServicesDiscovered n'a pas les permissions nécessaires pour se deconnecter")
                    val intent = Intent(act, NoBLEAuthorization::class.java)
                    startActivity(context, intent, null)
                    return
                }
                gatt.disconnect()
                return
            }

            //On scan tous les services disponibles sur l'appareil pour trouver les 4 charactérstiques
            val services = gatt.services
            for (service in services){
                pixelXCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_PIXELX))
                pixelYCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_PIXELY))
                colorCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_COLOR))
                sendCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_SEND))
                if(pixelXCharacteristic != null && pixelYCharacteristic != null &&
                    colorCharacteristic != null && sendCharacteristic != null){
                    myLogger("Found all 4 characteristics, YEAH !")
                    lifecycleState = BLELifecycleState.Connected
                    break
                }
            }

            connectedGatt = gatt

            if(pixelXCharacteristic == null || pixelYCharacteristic == null ||
                colorCharacteristic == null || sendCharacteristic == null){
                myLogger("Could not find the right chracteristics on the device, disconnecting...")

            }
        }

        //TODO: Dans le futur: override ces fonction pour récupérer des logs ?
        /*
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_READ_UUID)) {
                val strValue = characteristic.value.toString(Charsets.UTF_8)
                val log = "onCharacteristicRead " + when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                    else -> "error $status"
                }
                appendLog(log)
                runOnUiThread {
                    textViewReadValue.text = strValue
                }
            } else {
                appendLog("onCharacteristicRead unknown uuid $characteristic.uuid")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_WRITE_UUID)) {
                val log: String = "onCharacteristicWrite " + when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "OK"
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "not allowed"
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "invalid length"
                    else -> "error $status"
                }
                appendLog(log)
            } else {
                appendLog("onCharacteristicWrite unknown uuid $characteristic.uuid")
            }
        }*/
    }

    private fun ensureBluetoothCanBeUsed(completion: (Boolean, String) -> Unit) {
        grantBluetoothCentralPermissions(AskType.AskOnce) { isGranted ->
            if (!isGranted) {
                completion(false, "Bluetooth permissions denied")
                return@grantBluetoothCentralPermissions
            }

            enableBluetooth(AskType.AskOnce) { isEnabled ->
                if (!isEnabled) {
                    completion(false, "Bluetooth OFF")
                    return@enableBluetooth
                }

                grantLocationPermissionIfRequired(AskType.AskOnce) { isGranted ->
                    if (!isGranted) {
                        completion(false, "Location permission denied")
                        return@grantLocationPermissionIfRequired
                    }

                    completion(true, "Bluetooth ON, permissions OK, ready")
                }
            }
        }
    }

    private fun enableBluetooth(askType: AskType, completion: (Boolean) -> Unit) {
        if (bluetoothAdapter.isEnabled) {
            completion(true)
        } else {
            //val intentString = BluetoothAdapter.ACTION_REQUEST_ENABLE
            val requestCode = ENABLE_BLUETOOTH_REQUEST_CODE

            // set activity result handler
            activityResultHandlers[requestCode] = { result ->
                val isSuccess = result == Activity.RESULT_OK
                if (isSuccess || askType != AskType.InsistUntilSuccess) {
                    activityResultHandlers.remove(requestCode)
                    completion(isSuccess)
                } else {
                    // start activity for the request again
                   // startActivityForResult(Intent(intentString), requestCode)
                }
            }

            // start activity for the request
            //startActivityForResult(Intent(intentString), requestCode)
        }
    }

    private fun grantBluetoothCentralPermissions(askType: AskType, completion: (Boolean) -> Unit) {
        val wantedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            emptyArray()
        }

        if (wantedPermissions.isEmpty() || hasPermissions(wantedPermissions)) {
            completion(true)
        } else {
            act.runOnUiThread {
                val requestCode = BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE

                // set permission result handler
                permissionResultHandlers[requestCode] = { _ /*permissions*/, grantResults ->
                    val isSuccess = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                    if (isSuccess || askType != AskType.InsistUntilSuccess) {
                        permissionResultHandlers.remove(requestCode)
                        completion(isSuccess)
                    } else {
                        // request again
                        requestPermissionArray(wantedPermissions, requestCode)
                    }
                }

                requestPermissionArray(wantedPermissions, requestCode)
            }
        }
    }

    private fun grantLocationPermissionIfRequired(askType: AskType, completion: (Boolean) -> Unit) {
        val wantedPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // BLUETOOTH_SCAN permission has flag "neverForLocation", so location not needed
            completion(true)
        } else if (hasPermissions(wantedPermissions)) {
            completion(true)
        } else {
            act.runOnUiThread {
                val requestCode = LOCATION_PERMISSION_REQUEST_CODE

                // prepare motivation message
                val builder = Builder(context)
                builder.setTitle("Location permission required")
                builder.setMessage("BLE advertising requires location access, starting from Android 6.0")
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissionArray(wantedPermissions, requestCode)
                }
                builder.setCancelable(false)

                // set permission result handler
                permissionResultHandlers[requestCode] = { _, grantResults ->
                    val isSuccess = grantResults.firstOrNull() != PackageManager.PERMISSION_DENIED
                    if (isSuccess || askType != AskType.InsistUntilSuccess) {
                        permissionResultHandlers.remove(requestCode)
                        completion(isSuccess)
                    } else {
                        // show motivation message again
                        builder.create().show()
                    }
                }

                // show motivation message
                builder.create().show()
            }
        }
    }

    private var activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()
    private var permissionResultHandlers = mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()

    private fun setConnectedGattToNull() {
        connectedGatt = null
        pixelXCharacteristic = null
        pixelYCharacteristic = null
        colorCharacteristic = null
        sendCharacteristic = null
    }

    private fun bleRestartLifecycle() {
        act.runOnUiThread {
            prepareAndStartBleScan()
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionArray(permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(act, permissions, requestCode)
    }

    private fun myLogger(message: String) {
        Log.d("BLEinterface", message)
    }
}